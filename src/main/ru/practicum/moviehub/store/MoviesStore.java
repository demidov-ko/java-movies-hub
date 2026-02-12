package ru.practicum.moviehub.store;


import ru.practicum.moviehub.model.Movie;

import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

//отвечает за хранение, добавление, поиск и удаление фильмов;
public class MoviesStore {
    private Map<Integer, Movie> movies = new HashMap<>();
    private int nextId = 0;
    private static final int MIN_YEAR = 1888;
    private static final int MAX_YEAR = Year.now().getValue() + 1;

    public int getNextId() {
        return ++nextId;
    }

    public Map<Integer, Movie> getMovies() {
        return movies;
    }

    //вернуть все фильмы
    public List<Movie> getAllMovies() {
        return new ArrayList<>(movies.values()); // Возвращаем копию значений HashMap
    }

    //добавить фильм
    public Optional<Movie> addMovie(Movie movie) {
        List<String> validationError = validateMovie(movie);
        if (!validationError.isEmpty()) {
            return Optional.empty();
        }

        int newId = getNextId();
        movie.setId(newId);

        movies.put(newId, movie);
        return Optional.of(movie);
    }

    public Optional<Movie> getMovieById(int id) {
        return Optional.ofNullable(movies.get(id));
    }

    public Optional<Movie> deleteMovieById(int id) {
        return Optional.ofNullable(movies.remove(id));
    }


    public Optional<List<String>> getTitlesByYear(int year) {
        return Optional.of(
                movies.values().stream()
                        .filter(movie -> movie.getYear() == year)
                        .map(Movie::getTitle)
                        //Убираем дубликаты
                        .distinct()
                        .collect(Collectors.toList())
        ).filter(list -> !list.isEmpty());
    }

    public void clear() {
        this.movies.clear();
    }

    public List<String> validateMovie(Movie movie) {
        List<String> errors = new ArrayList<>();

        if (movie == null) {
            errors.add("Фильм не может быть null");
            return errors;
        }
        if (movie.getTitle() == null || movie.getTitle().trim().isEmpty()) {
            errors.add("Название не должно быть пустым");
        } else if (movie.getTitle().length() > 100) {
            errors.add("Название не может превышать 100 символов");
        }
        if (movie.getYear() < MIN_YEAR || movie.getYear() > MAX_YEAR) {
            errors.add("Год должен быть между " + MIN_YEAR + " и " + MAX_YEAR);
        }
        return errors;
    }
}
