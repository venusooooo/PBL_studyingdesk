package studyDesk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * AI 接口配置，存放在数据目录下的 ai.properties。
 * Key 只留在本机，不写进代码、不进仓库；兼容 OpenAI 格式的接口都能用。
 */
final class AiConfig {

    String baseUrl = "https://api.openai.com/v1";
    String apiKey = "";
    String model = "gpt-4o-mini";

    static Path file() {
        return Store.get().dataDir().resolve("ai.properties");
    }

    static AiConfig load() {
        AiConfig config = new AiConfig();
        Path path = file();
        if (!Files.exists(path)) {
            return config;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            System.err.println("读取 AI 配置失败：" + e.getMessage());
            return config;
        }
        config.baseUrl = props.getProperty("baseUrl", config.baseUrl).trim();
        config.apiKey = props.getProperty("apiKey", "").trim();
        config.model = props.getProperty("model", config.model).trim();
        return config;
    }

    void save() throws IOException {
        Path path = file();
        Files.createDirectories(path.getParent());
        Properties props = new Properties();
        props.setProperty("baseUrl", baseUrl);
        props.setProperty("apiKey", apiKey);
        props.setProperty("model", model);
        try (OutputStream out = Files.newOutputStream(path)) {
            props.store(out, "StudyDesk AI config (local only)");
        }
    }

    boolean configured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** 允许用户填 https://xxx/v1 或完整地址，这里统一补成 /chat/completions。 */
    String endpoint() {
        String base = baseUrl == null ? "" : baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/chat/completions")) {
            return base;
        }
        return base + "/chat/completions";
    }
}
