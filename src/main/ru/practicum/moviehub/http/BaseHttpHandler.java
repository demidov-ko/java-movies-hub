package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {


    protected static final String CT_JSON = "application/json; charset=UTF-8";
    protected static final Gson GSON = new Gson();

    //метод для отправки ответа с телом в формате JSON
    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);

        if (json == null) {
            ex.sendResponseHeaders(status, -1);
            ex.close();
            return;
        }

        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
        long responseLength = responseBytes.length;

        ex.sendResponseHeaders(status, responseLength);

        try (var outputStream = ex.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }

    //метод для отправки ответа с пустым телом
    protected void sendNoContent(HttpExchange ex) throws java.io.IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(204, -1); // -1 означает отсутствие тела
        ex.close();
    }

    // Вспомогательный метод для отправки ошибок
    protected void sendError(HttpExchange httpExchange, ErrorResponse errorResponse) throws IOException {
        String jsonResponse = GSON.toJson(errorResponse);
        sendJson(httpExchange, errorResponse.getStatus(), jsonResponse);
    }
}

