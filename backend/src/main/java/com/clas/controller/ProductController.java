package com.clas.controller;

import com.clas.common.Result;
import com.clas.entity.Product;
import com.clas.service.ProductService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/list/{merchantId}")
    public Result<List<Product>> list(@PathVariable Long merchantId) {
        return Result.ok(productService.listByMerchant(merchantId));
    }
}

