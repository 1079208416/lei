package com.shike.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shike.delivery.domain.DomainEnums.OrderStatus;
import com.shike.delivery.domain.FoodOrder;
import com.shike.delivery.mapper.FoodOrderMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderTimeoutJob {

    private final FoodOrderMapper orderMapper;
    private final OrderService orderService;

    public OrderTimeoutJob(FoodOrderMapper orderMapper, OrderService orderService) {
        this.orderMapper = orderMapper;
        this.orderService = orderService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void closeExpiredUnpaidOrders() {
        List<FoodOrder> expired = orderMapper.selectList(
            Wrappers.<FoodOrder>lambdaQuery()
                .eq(FoodOrder::getStatus, OrderStatus.WAITING_PAYMENT)
                .lt(FoodOrder::getCreatedAt, LocalDateTime.now().minusMinutes(15))
                .last("limit 50")
        );
        for (FoodOrder order : expired) {
            try {
                orderService.transition(
                    order,
                    OrderStatus.CANCELLED,
                    order.getPaymentStatus(),
                    "支付超时，系统自动取消",
                    "SYSTEM",
                    null
                );
            } catch (RuntimeException ignored) {
                // Another request may have changed the order concurrently.
            }
        }
    }
}
