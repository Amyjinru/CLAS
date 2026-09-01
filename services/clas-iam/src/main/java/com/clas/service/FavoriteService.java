package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.client.MerchantClient;
import com.clas.config.UserContext;
import com.clas.entity.Favorite;
import com.clas.entity.Merchant;
import com.clas.mapper.FavoriteMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FavoriteService {
    private final FavoriteMapper favoriteMapper;
    private final MerchantClient merchantClient;

    public FavoriteService(FavoriteMapper favoriteMapper, MerchantClient merchantClient) {
        this.favoriteMapper = favoriteMapper;
        this.merchantClient = merchantClient;
    }

    public Favorite add(Long merchantId) {
        merchantClient.requireMerchant(merchantId);
        Favorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
            .eq(Favorite::getUserId, UserContext.getUserId())
            .eq(Favorite::getMerchantId, merchantId));
        if (existing != null) {
            return existing;
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(UserContext.getUserId());
        favorite.setMerchantId(merchantId);
        favoriteMapper.insert(favorite);
        return favorite;
    }

    public void remove(Long merchantId) {
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
            .eq(Favorite::getUserId, UserContext.getUserId())
            .eq(Favorite::getMerchantId, merchantId));
    }

    public List<Merchant> mine() {
        List<Long> merchantIds = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, UserContext.getUserId())
                .orderByDesc(Favorite::getId))
            .stream()
            .map(Favorite::getMerchantId)
            .toList();
        return merchantClient.getMerchantsByIds(merchantIds);
    }
}
