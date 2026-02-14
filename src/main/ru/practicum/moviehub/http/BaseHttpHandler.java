package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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

    protected Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }
        try {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);

                if (keyValue.length == 2) {
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                    params.put(key, value);
                } else if (keyValue.length == 1) {
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                    params.put(key, "");
                }
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Ошибка декодирования query-параметров: " + e.getMessage());
        }
        return params;
    }

    protected String getQueryParam(String query, String paramName) {
        Map<String, String> params = parseQueryParams(query);
        return params.get(paramName);
    }
}

