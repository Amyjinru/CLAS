package com.clas.controller;

import com.clas.common.Result;
import com.clas.config.RequireRole;
import com.clas.entity.Favorite;
import com.clas.entity.Merchant;
import com.clas.service.FavoriteService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favorites")
@RequireRole("USER")
public class FavoriteController {
    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping("/mine")
    public Result<List<Merchant>> mine() {
        return Result.ok(favoriteService.mine());
    }

    @PostMapping("/{merchantId}")
    public Result<Favorite> add(@PathVariable Long merchantId) {
        return Result.ok(favoriteService.add(merchantId));
    }

    @DeleteMapping("/{merchantId}")
    public Result<Void> remove(@PathVariable Long merchantId) {
        favoriteService.remove(merchantId);
        return Result.ok();
    }
}
