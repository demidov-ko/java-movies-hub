package ru.practicum.moviehub.api;

import java.util.List;

public class ErrorResponse {
    private int status;
    private String error;
    private List<String> details;

    public ErrorResponse(int status, String error, List<String> details) {
        this.status = status;
        this.error = error;
        this.details = details;
    }

    public int getStatus() {
        return status;
    }

    //метод для некорректного запроса
    public static ErrorResponse badRequest(String detail) {
        return new ErrorResponse(400, "Bad Request", List.of(detail));
    }

    //метод для отсутствующего ресурса
    public static ErrorResponse notFound(String detail) {
        return new ErrorResponse(404, "Not Found", List.of(detail));
    }

    //метод для неподдерживаемого метода
    public static ErrorResponse methodNotAllowed(String detail) {
        return new ErrorResponse(405, "Method Not Allowed", List.of(detail));
    }

    //метод для неподходящего заголовка
    public static ErrorResponse unsupportedMediaType(String detail) {
        return new ErrorResponse(415, "Unsupported Media Type", List.of(detail));
    }

    //метод для ошибок валидации
    public static ErrorResponse unprocessableEntity(List<String> validationErrors) {
        return new ErrorResponse(422, "Unprocessable Entity", validationErrors);
    }

    //метод для внутренней ошибки сервера
    public static ErrorResponse internalError(String detail) {
        return new ErrorResponse(500, "Internal Server Error", List.of(detail));
    }
}

