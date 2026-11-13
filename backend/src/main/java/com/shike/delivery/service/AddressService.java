package com.shike.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shike.delivery.common.ApiException;
import com.shike.delivery.domain.UserAddress;
import com.shike.delivery.mapper.UserAddressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AddressService {

    private final UserAddressMapper addressMapper;

    public AddressService(UserAddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    public List<UserAddress> list(Long accountId) {
        return addressMapper.selectList(
            Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getAccountId, accountId)
                .orderByDesc(UserAddress::getDefaultAddress)
                .orderByDesc(UserAddress::getId)
        );
    }

    @Transactional
    public UserAddress create(Long accountId, AddressInput input) {
        UserAddress address = new UserAddress();
        address.setAccountId(accountId);
        apply(address, input);
        address.setCreatedAt(LocalDateTime.now());
        if (Boolean.TRUE.equals(input.defaultAddress())) {
            clearDefault(accountId);
        }
        addressMapper.insert(address);
        return address;
    }

    @Transactional
    public UserAddress update(Long accountId, Long id, AddressInput input) {
        UserAddress address = requireOwned(accountId, id);
        apply(address, input);
        if (Boolean.TRUE.equals(input.defaultAddress())) {
            clearDefault(accountId);
        }
        addressMapper.updateById(address);
        return address;
    }

    public void delete(Long accountId, Long id) {
        UserAddress address = requireOwned(accountId, id);
        addressMapper.deleteById(address.getId());
    }

    public UserAddress requireOwned(Long accountId, Long id) {
        UserAddress address = addressMapper.selectById(id);
        if (address == null || !accountId.equals(address.getAccountId())) {
            throw ApiException.notFound("收货地址不存在");
        }
        return address;
    }

    private void clearDefault(Long accountId) {
        List<UserAddress> addresses = list(accountId);
        addresses.forEach(address -> {
            if (Boolean.TRUE.equals(address.getDefaultAddress())) {
                address.setDefaultAddress(false);
                addressMapper.updateById(address);
            }
        });
    }

    private void apply(UserAddress address, AddressInput input) {
        address.setLabel(input.label());
        address.setContactName(input.contactName());
        address.setPhone(input.phone());
        address.setAddress(input.address());
        address.setDetail(input.detail());
        address.setLongitude(input.longitude());
        address.setLatitude(input.latitude());
        address.setDefaultAddress(Boolean.TRUE.equals(input.defaultAddress()));
    }

    public record AddressInput(
        String label,
        String contactName,
        String phone,
        String address,
        String detail,
        Double longitude,
        Double latitude,
        Boolean defaultAddress
    ) {
    }
}
