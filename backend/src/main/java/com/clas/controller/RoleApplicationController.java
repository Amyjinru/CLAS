package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.config.UserContext;
import com.clas.dto.RoleApplicationAuditRequest;
import com.clas.dto.RoleApplicationCreateRequest;
import com.clas.entity.RoleApplication;
import com.clas.service.RoleApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/role-applications")
public class RoleApplicationController {
    private final RoleApplicationService roleApplicationService;

    public RoleApplicationController(RoleApplicationService roleApplicationService) {
        this.roleApplicationService = roleApplicationService;
    }

    @PostMapping("/rider")
    @RequireRole("USER")
    public Result<RoleApplication> applyForRider(@Valid @RequestBody RoleApplicationCreateRequest request) {
        return Result.ok(roleApplicationService.applyForRider(UserContext.getUserId(), request));
    }

    @GetMapping("/mine")
    @RequireRole({"USER", "RIDER"})
    public Result<List<RoleApplication>> listMine() {
        return Result.ok(roleApplicationService.listMine(UserContext.getUserId()));
    }

    @GetMapping("/admin")
    @RequireRole("ADMIN")
    public Result<List<RoleApplication>> listForAdmin(@RequestParam(required = false) String status) {
        return Result.ok(roleApplicationService.listForAdmin(status));
    }

    @PostMapping("/admin/{id}/audit")
    @RequireRole("ADMIN")
    public Result<RoleApplication> audit(
        @PathVariable Long id,
        @Valid @RequestBody RoleApplicationAuditRequest request
    ) {
        return Result.ok(roleApplicationService.audit(id, request, UserContext.getUserId()));
    }
}
