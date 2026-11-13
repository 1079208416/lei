package com.shike.delivery.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shike.delivery.domain.DomainEnums.AccountRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("app_account")
public class Account {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String passwordHash;
    private String displayName;
    private AccountRole role;
    private Long merchantId;
    private Boolean active;
    private LocalDateTime createdAt;
}
