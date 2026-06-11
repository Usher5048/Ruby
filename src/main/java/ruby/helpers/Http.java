package ruby.helpers;

import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;

public final class Http {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .build();
    private static final Gson GSON = new Gson();

    private Http() {}

    public static Request get(String url) {
        return new Request("GET", url);
    }

    public static Request post(String url) {
        return new Request("POST", url);
    }

    public static final class Request {
        private final HttpRequest.Builder builder;
        private String method;

        private Request(String method, String url) {
            this.method = method;
            this.builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "RubyClient/1.0");
        }

        public Request bearer(String token) {
            this.builder.header("Authorization", "Bearer " + token);
            return this;
        }

        public Request bodyForm(String body) {
            this.builder.header("Content-Type", "application/x-www-form-urlencoded");
            this.builder.method(this.method, HttpRequest.BodyPublishers.ofString(body));
            this.method = null;
            return this;
        }

        public Request bodyJson(String body) {
            this.builder.header("Content-Type", "application/json");
            this.builder.method(this.method, HttpRequest.BodyPublishers.ofString(body));
            this.method = null;
            return this;
        }

        public <T> T sendJson(Class<T> type) {
            try {
                if (this.method != null) {
                    this.builder.method(this.method, HttpRequest.BodyPublishers.noBody());
                }

                HttpResponse<String> response = Http.CLIENT.send(
                        this.builder.build(),
                        HttpResponse.BodyHandlers.ofString()
                );

                if (response.statusCode() < 200 || response.statusCode() >= 300) return null;
                return Http.GSON.fromJson(response.body(), type);
            } catch (Exception e) {
                ruby.RubyClient.LOGGER.error("HTTP request failed", e);
                return null;
            }
        }
    }
}
