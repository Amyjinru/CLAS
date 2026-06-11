package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.dto.BankCardRequest;
import com.clas.dto.BankCardResponse;
import com.clas.service.UserBankCardService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/bank-cards")
@RequireRole("USER")
public class UserBankCardController {
    private final UserBankCardService userBankCardService;

    public UserBankCardController(UserBankCardService userBankCardService) {
        this.userBankCardService = userBankCardService;
    }

    @GetMapping
    public Result<List<BankCardResponse>> mine() {
        return Result.ok(userBankCardService.listMine());
    }

    @PostMapping
    public Result<BankCardResponse> create(@Valid @RequestBody BankCardRequest request) {
        return Result.ok(userBankCardService.create(request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userBankCardService.delete(id);
        return Result.ok();
    }
}
