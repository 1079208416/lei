package com.shike.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shike.delivery.domain.CartItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}
