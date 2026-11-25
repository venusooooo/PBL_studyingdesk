package studyDesk;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 极简大模型客户端：POST /chat/completions，只依赖 JDK 11 自带的 java.net.http。
 * 同步阻塞调用，必须由 SwingWorker 在后台线程执行，否则会卡住界面。
 */
final class AiClient {

    private final AiConfig config;

    AiClient(AiConfig config) {
        this.config = config;
    }

    String chat(String system, String user) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.model);
        body.put("temperature", 0.3);
        List<Object> messages = new ArrayList<>();
        messages.add(message("system", system));
        messages.add(message("user", user));
        body.put("messages", messages);

        HttpRequest request = HttpRequest.newBuilder(URI.create(config.endpoint()))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + config.apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(Json.write(body), StandardCharsets.UTF_8))
                .build();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("接口返回 " + response.statusCode() + "："
                    + shorten(response.body()));
        }
        Map<String, Object> root = Json.map(Json.parse(response.body()));
        List<Object> choices = Json.list(root.get("choices"));
        if (choices.isEmpty()) {
            throw new IllegalStateException("接口没有返回内容");
        }
        Map<String, Object> message = Json.map(Json.map(choices.get(0)).get("message"));
        String content = Json.str(message, "content", "").trim();
        if (content.isEmpty()) {
            throw new IllegalStateException("模型返回了空内容");
        }
        return content;
    }

    private static Map<String, Object> message(String role, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private static String shorten(String text) {
        if (text == null) {
            return "";
        }
        String clean = text.replaceAll("\\s+", " ").trim();
        return clean.length() > 300 ? clean.substring(0, 300) + "…" : clean;
    }
}
