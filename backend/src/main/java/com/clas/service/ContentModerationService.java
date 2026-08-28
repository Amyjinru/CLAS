package com.clas.service;

import com.clas.common.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ContentModerationService {
    private static final Logger log = LoggerFactory.getLogger(ContentModerationService.class);
    private static final List<String> DEFAULT_FORBIDDEN_WORDS = List.of(
        "色情", "涉黄", "赌博", "毒品", "违禁", "诈骗"
    );

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String baseUrl;
    private final String textModel;
    private final String imageModel;
    private final List<String> forbiddenWords;
    private final Duration chatApiTimeout;

    public ContentModerationService(
        ObjectMapper objectMapper,
        @Value("${dashscope.api-key:}") String apiKey,
        @Value("${dashscope.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl,
        @Value("${dashscope.text-model:qwen3.6-flash}") String textModel,
        @Value("${dashscope.image-model:qwen3.5-flash}") String imageModel,
        @Value("${content-moderation.forbidden-words:}") String configuredForbiddenWords,
        @Value("${content-moderation.chat-api-timeout-ms:3000}") long chatApiTimeoutMillis
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.textModel = textModel;
        this.imageModel = imageModel;
        this.forbiddenWords = buildForbiddenWords(configuredForbiddenWords);
        this.chatApiTimeout = Duration.ofMillis(Math.max(100, chatApiTimeoutMillis));
    }

    public void assertTextAllowed(String text, String scene) {
        if (text == null || text.isBlank()) {
            return;
        }
        String matched = findForbiddenWord(text);
        if (matched != null) {
            throw new BusinessException(scene + "包含违禁词，提交失败");
        }
        if (apiKey.isBlank()) {
            return;
        }
        ModerationResult result = moderateText(text, scene);
        if (!result.safe()) {
            throw new BusinessException(result.reason().isBlank()
                ? scene + "未通过内容安全审核"
                : scene + "未通过内容安全审核：" + result.reason());
        }
    }

    public void assertChatTextAllowed(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        String matched = findForbiddenWord(text);
        if (matched != null) {
            throw new BusinessException("聊天消息包含违禁词，发送失败");
        }
        if (apiKey.isBlank()) {
            return;
        }
        try {
            ModerationResult result = moderateText(text, "聊天消息", chatApiTimeout);
            if (!result.safe()) {
                throw new BusinessException(result.reason().isBlank()
                    ? "聊天消息未通过内容安全审核"
                    : "聊天消息未通过内容安全审核：" + result.reason());
            }
        } catch (BusinessException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("未通过内容安全审核")) {
                throw exception;
            }
            log.warn("聊天消息 AI 审核不可用，已按本地词库结果放行：{}", exception.getMessage());
        } catch (RuntimeException exception) {
            log.warn("聊天消息 AI 审核异常，已按本地词库结果放行：{}", exception.getMessage());
        }
    }

    public void assertAvatarAllowed(MultipartFile file) {
        if (file == null || file.isEmpty() || apiKey.isBlank()) {
            return;
        }
        try {
            String contentType = file.getContentType();
            String mediaType = contentType == null || contentType.isBlank() ? "image/jpeg" : contentType;
            String dataUrl = "data:" + mediaType + ";base64," + java.util.Base64.getEncoder().encodeToString(file.getBytes());
            assertImageAllowed(dataUrl, "头像图片");
        } catch (IOException exception) {
            throw new BusinessException("头像图片读取失败");
        }
    }

    public void assertAvatarUrlAllowed(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank() || apiKey.isBlank()) {
            return;
        }
        assertImageAllowed(imageUrl.trim(), "头像图片");
    }

    private void assertImageAllowed(String imageUrl, String scene) {
        ModerationResult result = moderateImage(imageUrl, scene);
        if (!result.safe()) {
            throw new BusinessException(result.reason().isBlank()
                ? scene + "未通过内容安全审核"
                : scene + "未通过内容安全审核：" + result.reason());
        }
    }

    private ModerationResult moderateText(String text, String scene) {
        return moderateText(text, scene, Duration.ofSeconds(15));
    }

    private ModerationResult moderateText(String text, String scene, Duration timeout) {
        List<Map<String, Object>> messages = List.of(
            Map.of("role", "system", "content", textSystemPrompt()),
            Map.of("role", "user", "content", scene + "：\n" + text)
        );
        return callDashScope(textModel, messages, timeout);
    }

    private ModerationResult moderateImage(String imageUrl, String scene) {
        List<Map<String, Object>> userContent = List.of(
            Map.of("type", "image_url", "image_url", Map.of("url", imageUrl)),
            Map.of("type", "text", "text", "请审核这张" + scene + "，同时检查图片中的可见文字。")
        );
        List<Map<String, Object>> messages = List.of(
            Map.of("role", "system", "content", imageSystemPrompt()),
            Map.of("role", "user", "content", userContent)
        );
        return callDashScope(imageModel, messages, Duration.ofSeconds(15));
    }

    private ModerationResult callDashScope(String model, List<Map<String, Object>> messages, Duration timeout) {
        Map<String, Object> payload = Map.of(
            "model", model,
            "messages", messages,
            "temperature", 0,
            "response_format", Map.of("type", "json_object")
        );
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/completions"))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("内容审核服务暂时不可用");
            }
            String content = objectMapper.readTree(response.body())
                .path("choices").path(0).path("message").path("content").asText();
            return parseModerationJson(content);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("审核服务返回格式异常");
        } catch (IOException exception) {
            throw new BusinessException("内容审核服务调用失败");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("内容审核服务调用失败");
        }
    }

    private ModerationResult parseModerationJson(String content) throws JsonProcessingException {
        String normalized = content == null ? "" : content.trim()
            .replaceFirst("(?i)^```json\\s*", "")
            .replaceFirst("(?i)^```\\s*", "")
            .replaceFirst("(?i)```$", "")
            .trim();
        JsonNode node = objectMapper.readTree(normalized);
        boolean safe = node.path("safe").asBoolean(false);
        String reason = node.path("reason").asText("");
        return new ModerationResult(safe, reason);
    }

    private String findForbiddenWord(String text) {
        String normalized = text.toLowerCase();
        return forbiddenWords.stream()
            .filter(word -> normalized.contains(word.toLowerCase()))
            .findFirst()
            .orElse(null);
    }

    private List<String> buildForbiddenWords(String configuredForbiddenWords) {
        List<String> words = new ArrayList<>(DEFAULT_FORBIDDEN_WORDS);
        if (configuredForbiddenWords != null && !configuredForbiddenWords.isBlank()) {
            for (String word : configuredForbiddenWords.split(",")) {
                String trimmed = word.trim();
                if (!trimmed.isBlank()) {
                    words.add(trimmed);
                }
            }
        }
        return words;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://dashscope.aliyuncs.com/compatible-mode/v1";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String textSystemPrompt() {
        return """
            你是一个中文社区内容安全审核器，只输出 JSON，不要输出任何额外文字。
            审核标准：不得包含色情低俗、暴力血腥、违法犯罪、毒品赌博诈骗、仇恨辱骂、未成年人不当内容、政治敏感动员、隐私泄露、违禁词或不适合作为公开社区内容的信息。
            当前任务是审核用户提交文本。
            返回格式：{"safe":true,"reason":""} 或 {"safe":false,"reason":"违规原因"}
            """;
    }

    private String imageSystemPrompt() {
        return """
            你是一个中文社区内容安全审核器，只输出 JSON，不要输出任何额外文字。
            审核标准：不得包含色情低俗、暴力血腥、违法犯罪、毒品赌博诈骗、仇恨辱骂、未成年人不当内容、政治敏感动员、隐私泄露、违禁词或不适合作为公开社区内容的信息。
            当前任务是审核用户头像图片，同时检查图片中可见文字是否违规。
            返回格式：{"safe":true,"reason":""} 或 {"safe":false,"reason":"违规原因"}
            """;
    }

    private record ModerationResult(boolean safe, String reason) {
    }
}
