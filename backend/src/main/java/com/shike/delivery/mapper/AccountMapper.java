package com.shike.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shike.delivery.domain.Account;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountMapper extends BaseMapper<Account> {
}
