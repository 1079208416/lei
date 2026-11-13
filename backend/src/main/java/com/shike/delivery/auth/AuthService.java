package com.shike.delivery.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shike.delivery.common.ApiException;
import com.shike.delivery.domain.Account;
import com.shike.delivery.domain.DomainEnums.AccountRole;
import com.shike.delivery.mapper.AccountMapper;
import com.shike.delivery.security.AuthPrincipal;
import com.shike.delivery.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {

    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final boolean demoMode;

    public AuthService(
        AccountMapper accountMapper,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        @Value("${app.demo-mode:true}") boolean demoMode
    ) {
        this.accountMapper = accountMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.demoMode = demoMode;
    }

    public Map<String, Object> login(String phone, String password) {
        Account account = accountMapper.selectOne(
            Wrappers.<Account>lambdaQuery().eq(Account::getPhone, phone)
        );
        if (account == null || !passwordEncoder.matches(password, account.getPasswordHash())) {
            throw ApiException.badRequest("手机号或密码不正确");
        }
        if (!Boolean.TRUE.equals(account.getActive())) {
            throw ApiException.forbidden("账号已被停用");
        }
        return buildLoginResult(account);
    }

    public Map<String, Object> demoLogin(String roleValue) {
        if (!demoMode) {
            throw ApiException.forbidden("演示登录未开启");
        }
        AccountRole role;
        try {
            role = AccountRole.valueOf(roleValue.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest("不支持的体验角色");
        }
        Account account = accountMapper.selectOne(
            Wrappers.<Account>lambdaQuery()
                .eq(Account::getRole, role)
                .eq(Account::getActive, true)
                .orderByAsc(Account::getId)
                .last("limit 1")
        );
        if (account == null) {
            throw ApiException.notFound("体验账号尚未初始化");
        }
        return buildLoginResult(account);
    }

    public Map<String, Object> me(Account account) {
        return accountView(account);
    }

    public Account requireAccount(Long id) {
        Account account = accountMapper.selectById(id);
        if (account == null) {
            throw ApiException.notFound("账号不存在");
        }
        return account;
    }

    private Map<String, Object> buildLoginResult(Account account) {
        AuthPrincipal principal = new AuthPrincipal(
            account.getId(),
            account.getDisplayName(),
            account.getRole().name(),
            account.getMerchantId()
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", jwtService.createToken(principal));
        result.put("account", accountView(account));
        return result;
    }

    public Map<String, Object> accountView(Account account) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", account.getId());
        view.put("phone", account.getPhone());
        view.put("name", account.getDisplayName());
        view.put("role", account.getRole().name());
        view.put("merchantId", account.getMerchantId());
        return view;
    }
}
