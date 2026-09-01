package com.clas.common.web;

import com.clas.common.Result;
import com.clas.common.service.ServiceIdentity;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@ConditionalOnBean(ServiceIdentity.class)
public class ServiceHealthController {
    private final ServiceIdentity identity;

    public ServiceHealthController(ServiceIdentity identity) {
        this.identity = identity;
    }

    @GetMapping
    public Result<Map<String, String>> health() {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("status", "ok");
        payload.put("service", identity.id());
        payload.put("name", identity.displayName());
        return Result.ok(payload);
    }
}
