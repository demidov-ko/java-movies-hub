package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.Endpoint;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }


    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String method = httpExchange.getRequestMethod();
        URI uri = httpExchange.getRequestURI();
        String path = uri.getPath();
        //Извлекает всю строку параметров запроса из URI — то, что идёт после символа ?
        String query = uri.getQuery();

        Endpoint endpoint = getEndpoint(path, method, query);

        switch (endpoint) {
            case GET_MOVIES:
                handleGetMovies(httpExchange);
                break;
            case POST_MOVIE:
                handlePostMovies(httpExchange);
                break;
            case GET_MOVIE_ID:
                handleGetMoviesById(httpExchange);
                break;
            case DELETE_MOVIE_ID:
                handleDeleteMoviesById(httpExchange);
                break;
            case GET_MOVIES_YEAR:
                handleGetMoviesByYear(httpExchange, query);
                break;
            case UNKNOWN:
                if (isPotentialMovieIdPath(path)) {
                    // Путь похож на /movies/{id}, но ID не число
                    sendError(httpExchange, ErrorResponse.badRequest(
                            "Параметр 'id' должен быть положительным целым числом"));
                } else {
                    sendError(httpExchange, ErrorResponse.notFound("Эндпоинт не найден"));
                }
                break;
        }
    }

    //возвращает фильмы
    private void handleGetMovies(HttpExchange httpExchange) throws IOException {
        List<Movie> movies = moviesStore.getAllMovies();
        String jsonResponse = GSON.toJson(movies);

        sendJson(httpExchange, 200, jsonResponse);
    }

    //возвращает фильмы по году
    private void handleGetMoviesByYear(HttpExchange httpExchange, String query) throws IOException {
        try {
            String yearParam = getQueryParam(query, "year");

            if (yearParam == null) {
                sendError(httpExchange, ErrorResponse.badRequest("Отсутствует параметр запроса year"));
                return;
            }
            if (yearParam.isEmpty()) {
                sendError(httpExchange, ErrorResponse.badRequest("Параметр year не может быть пустым"));
                return;
            }
            int year = Integer.parseInt(yearParam);
            Optional<List<Movie>> moviesOpt = moviesStore.getMoviesByYear(year);

            if (moviesOpt.isPresent()) {
                String jsonResponse = GSON.toJson(moviesOpt.get());
                sendJson(httpExchange, 200, jsonResponse);
            } else {
                sendJson(httpExchange, 200, "[]");
            }
        } catch (NumberFormatException e) {
            sendError(httpExchange, ErrorResponse.badRequest("Параметр year должен быть числом"));
        }
    }

    //возвращает фильмы по id
    private void handleGetMoviesById(HttpExchange httpExchange) throws IOException {
        try {
            String path = httpExchange.getRequestURI().getPath();
            String idString = path.substring(path.lastIndexOf('/') + 1);
            int id = Integer.parseInt(idString);

            Optional<Movie> movie = moviesStore.getMovieById(id);

            if (movie.isPresent()) {
                String jsonResponse = GSON.toJson(movie.get());
                sendJson(httpExchange, 200, jsonResponse);
            } else {
                sendError(httpExchange, ErrorResponse.notFound("Фильм не найден"));
            }
        } catch (NumberFormatException e) {
            sendError(httpExchange, ErrorResponse.badRequest("Некорректный ID"));
        }
    }

    //удаляет фильм по ID
    private void handleDeleteMoviesById(HttpExchange httpExchange) throws IOException {
        try {
            String path = httpExchange.getRequestURI().getPath();
            String idString = path.substring(path.lastIndexOf('/') + 1);
            int id = Integer.parseInt(idString);

            Optional<Movie> movie = moviesStore.getMovieById(id);

            if (movie.isPresent()) {
                moviesStore.deleteMovieById(id);
                sendNoContent(httpExchange);
            } else {
                sendError(httpExchange, ErrorResponse.notFound("Фильм с ID " + id + " не найден"));
            }
        } catch (NumberFormatException e) {
            sendError(httpExchange,
                    ErrorResponse.badRequest("Некорректный параметр запроса — 'id' (должен быть числом)"));
        } catch (Exception e) {
            sendError(httpExchange,
                    ErrorResponse.internalError("Произошла внутренняя ошибка сервера"));
        }
    }

    //добавляет фильм
    private void handlePostMovies(HttpExchange httpExchange) throws IOException {
        try {
            String contentType = httpExchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.startsWith("application/json")) {
                sendError(httpExchange,
                        ErrorResponse.unsupportedMediaType("Требуется Content-Type: application/json"));
                return;
            }
            String requestBody = new String(httpExchange.getRequestBody().readAllBytes());

            Movie movie = GSON.fromJson(requestBody, Movie.class);

            List<String> validationErrors = moviesStore.validateMovie(movie);
            if (!validationErrors.isEmpty()) {
                sendError(httpExchange, ErrorResponse.unprocessableEntity(validationErrors));
                return;
            }

            Optional<Movie> savedMovieOpt = moviesStore.addMovie(movie);

            if (savedMovieOpt.isPresent()) {
                String jsonResponse = GSON.toJson(savedMovieOpt.get());
                sendJson(httpExchange, 201, jsonResponse);
            } else {
                sendError(httpExchange, ErrorResponse.internalError("Не удалось добавить фильм"));
            }
        } catch (com.google.gson.JsonSyntaxException e) {
            sendError(httpExchange, ErrorResponse.badRequest("Некорректный JSON в теле запроса"));
        } catch (Exception e) {
            sendError(httpExchange, ErrorResponse.internalError("Произошла внутренняя ошибка сервера"));
        }
    }


    private boolean isPotentialMovieIdPath(String path) {
        return path.startsWith("/movies/") && path.split("/").length == 3;
    }


    private Endpoint getEndpoint(String path, String method, String query) {
        if (method.equalsIgnoreCase("GET")) {
            if (path.equals("/movies")) {
                Map<String, String> params = parseQueryParams(query);
                if (params.containsKey("year")) {
                    return Endpoint.GET_MOVIES_YEAR;
                } else {
                    return Endpoint.GET_MOVIES;
                }
            } else if (path.matches("/movies/\\d+")) {
                return Endpoint.GET_MOVIE_ID;
            }
        } else if (method.equalsIgnoreCase("POST") && path.equals("/movies")) {
            return Endpoint.POST_MOVIE;
        } else if (method.equalsIgnoreCase("DELETE") && path.matches("/movies/\\d+")) {
            return Endpoint.DELETE_MOVIE_ID;
        }
        return Endpoint.UNKNOWN;
    }
}
