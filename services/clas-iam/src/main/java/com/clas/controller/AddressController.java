package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.dto.AddressRequest;
import com.clas.entity.UserAddress;
import com.clas.service.AddressService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/address")
@RequireRole("USER")
public class AddressController {
    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("/mine")
    public Result<List<UserAddress>> mine() {
        return Result.ok(addressService.listMine());
    }

    @PostMapping
    public Result<UserAddress> create(@Valid @RequestBody AddressRequest request) {
        return Result.ok(addressService.create(request));
    }

    @PutMapping("/{id}")
    public Result<UserAddress> update(@PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        return Result.ok(addressService.update(id, request));
    }

    @PostMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return Result.ok();
    }
}
