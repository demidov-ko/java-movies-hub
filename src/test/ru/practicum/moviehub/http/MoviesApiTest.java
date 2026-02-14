package ru.practicum.moviehub.http;

import org.junit.jupiter.api.*;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        // Останавливаем сервер, если он был создан
        if (server != null) {
            server.stop();
        }
    }

    // GET /movies
    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        //отправка запроса
        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        //проверка кода ответа
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        //проверка заголовка
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        //проверка, что был возвращен массив
        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenNotEmpty_returnsArrayWithMovies() throws Exception {
        store.addMovie(new Movie("Брат", 1997));
        store.addMovie(new Movie("Брат-2", 2000));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        String body = resp.body();
        assertTrue(body.contains("\"title\":\"Брат\"") && body.contains("\"year\":1997"),
                "Ответ должен содержать фильм 'Брат' с годом 1997");
        assertTrue(body.contains("\"title\":\"Брат-2\"") && body.contains("\"year\":2000"),
                "Ответ должен содержать фильм 'Брат-2' с годом 2000");
    }

    // POST /movies
    @Test
    void postMovies_addsMovieWithValidData() throws Exception {
        String jsonBody = "{\"title\": \"Новый фильм\", \"year\": 2026}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode(), "POST /movies с корректными данными должен вернуть 201");

        String body = resp.body();
        assertTrue(body.contains("\"title\":\"Новый фильм\"") && body.contains("\"year\":2026"),
                "Ответ должен содержать добавленный фильм");
        assertEquals(1, store.getAllMovies().size(), "В хранилище должен быть 1 фильм");
    }

    @Test
    void postMovies_returnsErrorWhenTitleIsEmpty() throws Exception {
        String jsonBody = "{\"title\": \"\", \"year\": 2026}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "Пустой title должен возвращать 422 Unprocessable Entity");

        String body = resp.body();
        assertTrue(body.contains("\"error\":\"Unprocessable Entity\""),
                "Ошибка должна иметь статус 422");
        assertTrue(body.contains("Название не должно быть пустым"),
                "Сообщение об ошибке должно указывать на пустой title");
    }

    @Test
    void postMovies_returnsErrorWhenTitleTooLong() throws Exception {
        String longTitle = "X".repeat(101);
        String jsonBody = String.format("{\"title\": \"%s\", \"year\": 2026}", longTitle);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());

        String body = resp.body();
        assertTrue(body.contains("Название не может превышать 100 символов"),
                "Сообщение должно указывать на слишком длинный title");
    }

    @Test
    void postMovies_returnsErrorWhenYearInvalid() throws Exception {
        String jsonBody = "{\"title\": \"Фильм\", \"year\": 1800}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());

        String body = resp.body();
        assertTrue(body.contains("Год должен быть между 1888 и"),
                "Сообщение должно указывать на некорректный год");
    }

    @Test
    void postMovies_returnsErrorWhenContentTypeInvalid() throws Exception {
        String jsonBody = "{\"title\": \"Фильм\", \"year\": 2026}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode(), "Неверный Content-Type должен возвращать 415");

        String body = resp.body();
        assertTrue(body.contains("Unsupported Media Type"),
                "Сообщение должно содержать 'Unsupported Media Type'");
    }

    @Test
    void postMovies_returnsErrorWhenJsonInvalid() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{invalid json}"))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "Некорректный JSON должен возвращать 400");
    }

    // GET /movies/{id}
    @Test
    void getMovieById_returnsMovieWhenExists() throws Exception {
        Movie movie = new Movie("Существующий фильм", 2026);
        Optional<Movie> addedMovie = store.addMovie(movie);
        int movieId = addedMovie.get().getId();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + movieId))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(),
                "GET /movies/{id} для существующего фильма должен вернуть 200");

        String body = resp.body();
        assertTrue(body.contains("\"title\":\"Существующий фильм\"") && body.contains("\"year\":2026"),
                "Ответ должен содержать данные фильма");
    }

    @Test
    void getMovieById_returnsErrorWhenNotFound() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/777"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(),
                "GET /movies/{id} для несуществующего фильма должен вернуть 404");

        String body = resp.body();
        assertTrue(body.contains("Not Found"), "Сообщение должно содержать 'Not Found'");
    }

    @Test
    void getMovieById_returnsErrorWhenIdNotNumber() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "GET /movies/{id} с нечисловым ID должен вернуть 400");

        String body = resp.body();
        assertTrue(body.contains("Bad Request"), "Сообщение должно содержать 'Bad Request'");
    }

    // DELETE /movies/{id}
    @Test
    void deleteMovieById_deletesMovieWhenExists() throws Exception {
        Movie movie = new Movie("Фильм для удаления", 2026);
        Optional<Movie> addedMovie = store.addMovie(movie);
        int movieId = addedMovie.get().getId();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + movieId))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode(),
                "DELETE /movies/{id} для существующего фильма должен вернуть 204");
        assertEquals("", resp.body().trim(), "При 204 No Content тело ответа должно быть пустым");
        assertFalse(store.getMovieById(movieId).isPresent(), "Фильм должен быть удалён из хранилища");
    }

    @Test
    void deleteMovieById_returnsErrorWhenNotFound() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/777"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(),
                "DELETE /movies/{id} для несуществующего фильма должен вернуть 404");

        String body = resp.body();
        assertTrue(body.contains("Not Found"), "Сообщение должно содержать 'Not Found'");
    }

    @Test
    void deleteMovieById_returnsErrorWhenIdNotNumber() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/abc"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "DELETE /movies/{id} с нечисловым ID должен вернуть 400");

        String body = resp.body();
        assertTrue(body.contains("Bad Request"), "Сообщение должно содержать 'Bad Request'");
    }

    // GET /movies?year=YYYY
    @Test
    void getMoviesByYear_returnsMoviesForSpecifiedYear() throws Exception {
        store.addMovie(new Movie("Фильм 2020", 2020));
        store.addMovie(new Movie("Ещё один 2020", 2020));
        store.addMovie(new Movie("Фильм 2021", 2021));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2020"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(),
                "GET /movies?year=YYYY для существующего года должен вернуть 200");

        String body = resp.body();
        assertTrue(body.contains("Фильм 2020") && body.contains("Ещё один 2020"),
                "Ответ должен содержать фильмы указанного года");
        assertFalse(body.contains("Фильм 2021"), "Ответ не должен содержать фильмы других лет");
    }

    @Test
    void getMoviesByYear_returnsEmptyArrayWhenNoMoviesForYear() throws Exception {
        store.addMovie(new Movie("Фильм 2020", 2020));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2025"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        String body = resp.body().trim();
        assertEquals("[]", body,
                "При отсутствии фильмов указанного года должен возвращаться пустой массив");
    }

    @Test
    void getMoviesByYear_returnsErrorWhenYearNotNumber() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=xxx"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(),
                "GET /movies?year= с нечисловым годом должен вернуть 400");


        String body = resp.body();
        assertTrue(body.contains("Bad Request"), "Сообщение должно содержать 'Bad Request'");
        assertTrue(body.contains("Параметр year должен быть числом"),
                "Сообщение должно указывать на ошибку в параметре year");
    }
}
