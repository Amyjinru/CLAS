package com.clas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.common.BusinessException;
import com.clas.common.GeoUtils;
import com.clas.config.UserContext;
import com.clas.dto.AddressRequest;
import com.clas.entity.UserAddress;
import com.clas.mapper.UserAddressMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressService {
    private final UserAddressMapper userAddressMapper;

    public AddressService(UserAddressMapper userAddressMapper) {
        this.userAddressMapper = userAddressMapper;
    }

    public List<UserAddress> listMine() {
        return userAddressMapper.selectList(new LambdaQueryWrapper<UserAddress>()
            .eq(UserAddress::getUserId, UserContext.getUserId())
            .orderByDesc(UserAddress::getIsDefault)
            .orderByDesc(UserAddress::getId));
    }

    @Transactional
    public UserAddress create(AddressRequest request) {
        if (!GeoUtils.hasCoordinate(request.longitude(), request.latitude())) {
            throw new BusinessException("请选择收货地址地图位置");
        }
        if (Boolean.TRUE.equals(request.isDefault())) {
            clearDefaults();
        }
        UserAddress address = new UserAddress();
        address.setUserId(UserContext.getUserId());
        address.setContactName(request.contactName());
        address.setPhone(request.phone());
        address.setAddress(request.address());
        address.setLongitude(request.longitude());
        address.setLatitude(request.latitude());
        address.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
        userAddressMapper.insert(address);
        return address;
    }

    @Transactional
    public void setDefault(Long id) {
        UserAddress address = userAddressMapper.selectById(id);
        if (address == null || !UserContext.getUserId().equals(address.getUserId())) {
            throw new BusinessException("地址不存在或无权操作");
        }
        clearDefaults();
        address.setIsDefault(true);
        userAddressMapper.updateById(address);
    }

    public void delete(Long id) {
        UserAddress address = userAddressMapper.selectById(id);
        if (address == null || !UserContext.getUserId().equals(address.getUserId())) {
            throw new BusinessException("地址不存在或无权操作");
        }
        userAddressMapper.deleteById(id);
    }

    private void clearDefaults() {
        for (UserAddress item : listMine()) {
            if (Boolean.TRUE.equals(item.getIsDefault())) {
                item.setIsDefault(false);
                userAddressMapper.updateById(item);
            }
        }
    }
}
