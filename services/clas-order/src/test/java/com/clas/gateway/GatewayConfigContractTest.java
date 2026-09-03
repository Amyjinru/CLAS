package com.clas.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GatewayConfigContractTest {
    @Test
    void 订单相关路径应转发到订单服务并保留请求标识() throws IOException {
        String config = Files.readString(gatewayConfig());

        assertThat(config).contains("location ^~ /api/cart { proxy_pass http://clas_order; }");
        assertThat(config).contains("location ^~ /api/order { proxy_pass http://clas_order; }");
        assertThat(config).contains("location ^~ /api/payment { proxy_pass http://clas_order; }");
        assertThat(config).contains("location ^~ /api/coupon { proxy_pass http://clas_order; }");
        assertThat(config).contains("location ^~ /api/review { proxy_pass http://clas_order; }");
        assertThat(config).contains("location ^~ /uploads/reviews/ { proxy_pass http://clas_order; }");
        assertThat(config).contains("proxy_set_header Authorization $http_authorization;");
        assertThat(config).contains("proxy_set_header X-Request-Id $clas_request_id;");
        assertThat(config).contains("add_header X-Request-Id $clas_request_id always;");
    }

    @Test
    void 上游故障应转换为包含请求标识的统一响应() throws IOException {
        String config = Files.readString(gatewayConfig());

        assertThat(config).contains("error_page 502 503 504 = @service_unavailable;");
        assertThat(config).contains("location @service_unavailable {");
        assertThat(config).contains("return 503 '{\"code\":503");
        assertThat(config).contains("\"requestId\":\"$clas_request_id\"");
    }

    private Path gatewayConfig() {
        Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (directory != null) {
            Path fromRepositoryRoot = directory.resolve("services/nginx/clas-gateway.conf");
            if (Files.isRegularFile(fromRepositoryRoot)) {
                return fromRepositoryRoot;
            }
            Path fromServicesDirectory = directory.resolve("nginx/clas-gateway.conf");
            if (Files.isRegularFile(fromServicesDirectory)) {
                return fromServicesDirectory;
            }
            directory = directory.getParent();
        }
        throw new IllegalStateException("未找到 services/nginx/clas-gateway.conf");
    }
}
