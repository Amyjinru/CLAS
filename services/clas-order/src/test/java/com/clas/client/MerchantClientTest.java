package com.clas.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.clas.common.BusinessException;
import com.clas.common.DomainErrorCode;
import com.clas.common.client.ServiceEndpoints;
import com.clas.entity.Merchant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class MerchantClientTest {
    @Test
    void 批量查询应访问商家内部接口并按商家标识返回结果() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        MerchantClient client = new MerchantClient(
            restTemplate,
            new ServiceEndpoints("http://iam.test", "http://merchant.test", "http://catalog.test", "http://order.test", "http://compat.test")
        );
        server.expect(once(), requestTo("http://merchant.test/internal/merchant/v1/merchants/batch?ids=1,2"))
            .andRespond(withSuccess("""
                {"code":200,"message":"success","data":[
                  {"id":1,"merchantName":"商家一"},
                  {"id":2,"merchantName":"商家二"}
                ]}
                """, MediaType.APPLICATION_JSON));

        Map<Long, Merchant> merchants = client.getMerchants(List.of(1L, 2L));

        assertThat(merchants).containsOnlyKeys(1L, 2L);
        assertThat(merchants.get(1L).getMerchantName()).isEqualTo("商家一");
        server.verify();
    }

    @Test
    void 商家服务异常应转换为统一的上游不可用错误() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        MerchantClient client = new MerchantClient(
            restTemplate,
            new ServiceEndpoints("http://iam.test", "http://merchant.test", "http://catalog.test", "http://order.test", "http://compat.test")
        );
        server.expect(once(), requestTo("http://merchant.test/internal/merchant/v1/merchants/1"))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client.getMerchant(1L))
            .isInstanceOfSatisfying(BusinessException.class, exception -> {
                assertThat(exception.getHttpStatus()).isEqualTo(503);
                assertThat(exception.getErrorCode()).isEqualTo(DomainErrorCode.UPSTREAM_UNAVAILABLE);
            });
        server.verify();
    }
}
