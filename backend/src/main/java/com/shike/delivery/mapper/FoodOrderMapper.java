package com.shike.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shike.delivery.domain.FoodOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FoodOrderMapper extends BaseMapper<FoodOrder> {
}
