package com.clas.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clas.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ContentModerationServiceTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void chatLocalForbiddenWordRejectsWithoutCallingAi() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        startServer(exchange -> {
            requests.incrementAndGet();
            respond(exchange, 200, moderationResponse(true, ""));
        });

        ContentModerationService service = newService(500);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> service.assertChatTextAllowed("这里包含赌博信息"));

        assertTrue(exception.getMessage().contains("违禁词"));
        assertTrue(requests.get() == 0);
    }

    @Test
    void chatAiUnsafeResultRejectsMessage() throws Exception {
        startServer(exchange -> respond(exchange, 200, moderationResponse(false, "AI 判定违规")));

        ContentModerationService service = newService(500);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> service.assertChatTextAllowed("普通文本"));

        assertTrue(exception.getMessage().contains("AI 判定违规"));
    }

    @Test
    void chatAiFailureFallsBackToLocalAllowedResult() throws Exception {
        startServer(exchange -> respond(exchange, 503, "service unavailable"));

        ContentModerationService service = newService(500);

        assertDoesNotThrow(() -> service.assertChatTextAllowed("正常配送沟通"));
    }

    @Test
    void chatAiTimeoutFallsBackWithoutLongWait() throws Exception {
        startServer(exchange -> {
            try {
                Thread.sleep(500);
                respond(exchange, 200, moderationResponse(true, ""));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
        ContentModerationService service = newService(100);

        long startedAt = System.nanoTime();
        assertDoesNotThrow(() -> service.assertChatTextAllowed("正常配送沟通"));
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

        assertTrue(elapsedMillis < 450, "AI 超时后应快速采用本地结果");
    }

    private ContentModerationService newService(long timeoutMillis) {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        return new ContentModerationService(
            new ObjectMapper(), "test-api-key", baseUrl, "test-model", "test-image-model", "", timeoutMillis
        );
    }

    private void startServer(ExchangeHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", handler::handle);
        server.start();
    }

    private String moderationResponse(boolean safe, String reason) {
        return "{\"choices\":[{\"message\":{\"content\":\"{\\\"safe\\\":" + safe
            + ",\\\"reason\\\":\\\"" + reason + "\\\"}\"}}]}";
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
