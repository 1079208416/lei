package com.shike.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shike.delivery.common.ApiException;
import com.shike.delivery.domain.Account;
import com.shike.delivery.domain.DeliveryTask;
import com.shike.delivery.domain.DomainEnums.AccountRole;
import com.shike.delivery.domain.DomainEnums.DeliveryStatus;
import com.shike.delivery.domain.DomainEnums.OrderStatus;
import com.shike.delivery.domain.DomainEnums.PaymentStatus;
import com.shike.delivery.domain.DomainEnums.RefundStatus;
import com.shike.delivery.domain.FoodOrder;
import com.shike.delivery.domain.Merchant;
import com.shike.delivery.domain.RefundRequest;
import com.shike.delivery.mapper.AccountMapper;
import com.shike.delivery.mapper.DeliveryTaskMapper;
import com.shike.delivery.mapper.FoodOrderMapper;
import com.shike.delivery.mapper.MerchantMapper;
import com.shike.delivery.mapper.RefundRequestMapper;
import com.shike.delivery.notification.NotificationWebSocketHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final FoodOrderMapper orderMapper;
    private final AccountMapper accountMapper;
    private final MerchantMapper merchantMapper;
    private final DeliveryTaskMapper deliveryTaskMapper;
    private final RefundRequestMapper refundMapper;
    private final OrderService orderService;
    private final CatalogService catalogService;
    private final NotificationWebSocketHandler notificationHandler;

    public AdminService(
        FoodOrderMapper orderMapper,
        AccountMapper accountMapper,
        MerchantMapper merchantMapper,
        DeliveryTaskMapper deliveryTaskMapper,
        RefundRequestMapper refundMapper,
        OrderService orderService,
        CatalogService catalogService,
        NotificationWebSocketHandler notificationHandler
    ) {
        this.orderMapper = orderMapper;
        this.accountMapper = accountMapper;
        this.merchantMapper = merchantMapper;
        this.deliveryTaskMapper = deliveryTaskMapper;
        this.refundMapper = refundMapper;
        this.orderService = orderService;
        this.catalogService = catalogService;
        this.notificationHandler = notificationHandler;
    }

    public Map<String, Object> dashboard() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        List<FoodOrder> today = orderMapper.selectList(
            Wrappers.<FoodOrder>lambdaQuery().ge(FoodOrder::getCreatedAt, start)
        );
        long pendingDispatch = orderMapper.selectCount(
            Wrappers.<FoodOrder>lambdaQuery().eq(FoodOrder::getStatus, OrderStatus.WAITING_PICKUP)
        );
        long activeDeliveries = orderMapper.selectCount(
            Wrappers.<FoodOrder>lambdaQuery().eq(FoodOrder::getStatus, OrderStatus.DELIVERING)
        );
        long refundRequests = refundMapper.selectCount(
            Wrappers.<RefundRequest>lambdaQuery().eq(RefundRequest::getStatus, RefundStatus.REQUESTED)
        );
        int gmv = today.stream()
            .filter(order -> order.getPaymentStatus() == PaymentStatus.PAID
                || order.getPaymentStatus() == PaymentStatus.REFUNDING)
            .mapToInt(FoodOrder::getTotalAmount)
            .sum();
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("orderCount", today.size());
        view.put("gmv", gmv);
        view.put("pendingDispatch", pendingDispatch);
        view.put("activeDeliveries", activeDeliveries);
        view.put("refundRequests", refundRequests);
        view.put("merchantCount", merchantMapper.selectCount(null));
        return view;
    }

    public List<Map<String, Object>> orders(OrderStatus status, String keyword) {
        var query = Wrappers.<FoodOrder>lambdaQuery().orderByDesc(FoodOrder::getCreatedAt);
        if (status != null) {
            query.eq(FoodOrder::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            String value = keyword.trim();
            query.and(wrapper -> wrapper
                .like(FoodOrder::getOrderNo, value)
                .or()
                .like(FoodOrder::getMerchantName, value)
            );
        }
        query.last("limit 100");
        return orderMapper.selectList(query).stream().map(orderService::orderView).toList();
    }

    public List<Map<String, Object>> riders() {
        return accountMapper.selectList(
            Wrappers.<Account>lambdaQuery()
                .eq(Account::getRole, AccountRole.RIDER)
                .eq(Account::getActive, true)
                .orderByAsc(Account::getId)
        ).stream().map(this::riderView).toList();
    }

    @Transactional
    public Map<String, Object> assign(Long orderId, Long riderId) {
        FoodOrder order = orderService.requireOrder(orderId);
        if (order.getStatus() != OrderStatus.WAITING_PICKUP) {
            throw ApiException.conflict("只有待取餐订单可以派单");
        }
        Account rider = accountMapper.selectById(riderId);
        if (rider == null || rider.getRole() != AccountRole.RIDER || !Boolean.TRUE.equals(rider.getActive())) {
            throw ApiException.badRequest("骑手不存在或已停用");
        }
        DeliveryTask task = deliveryTaskMapper.selectOne(
            Wrappers.<DeliveryTask>lambdaQuery().eq(DeliveryTask::getOrderId, orderId)
        );
        if (task == null) {
            task = new DeliveryTask();
            task.setOrderId(orderId);
            task.setRiderId(riderId);
            task.setStatus(DeliveryStatus.ASSIGNED);
            task.setAssignedAt(LocalDateTime.now());
            deliveryTaskMapper.insert(task);
        } else {
            if (task.getStatus() == DeliveryStatus.PICKED_UP || task.getStatus() == DeliveryStatus.DELIVERED) {
                throw ApiException.conflict("骑手已取餐，不能改派");
            }
            task.setRiderId(riderId);
            task.setStatus(DeliveryStatus.ASSIGNED);
            task.setAssignedAt(LocalDateTime.now());
            deliveryTaskMapper.updateById(task);
        }
        order.setRiderId(riderId);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        orderService.appendLog(order, "平台已分配骑手：" + rider.getDisplayName(), "ADMIN", null);
        notificationHandler.notifyOrderUpdate(order, "平台已分配骑手");
        return orderService.orderView(order);
    }

    public List<Map<String, Object>> refunds(RefundStatus status) {
        var query = Wrappers.<RefundRequest>lambdaQuery().orderByDesc(RefundRequest::getCreatedAt);
        if (status != null) {
            query.eq(RefundRequest::getStatus, status);
        }
        return refundMapper.selectList(query).stream().map(refund -> {
            FoodOrder order = orderService.requireOrder(refund.getOrderId());
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", refund.getId());
            view.put("orderId", refund.getOrderId());
            view.put("orderNo", order.getOrderNo());
            view.put("merchantName", order.getMerchantName());
            view.put("amount", refund.getAmount());
            view.put("reason", refund.getReason());
            view.put("status", refund.getStatus().name());
            view.put("createdAt", refund.getCreatedAt());
            return view;
        }).toList();
    }

    @Transactional
    public void decideRefund(Long refundId, boolean approve) {
        RefundRequest refund = refundMapper.selectById(refundId);
        if (refund == null) {
            throw ApiException.notFound("退款申请不存在");
        }
        if (refund.getStatus() != RefundStatus.REQUESTED) {
            throw ApiException.conflict("退款申请已处理");
        }
        if (approve) {
            orderService.completeRefund(refund.getOrderId(), "ADMIN", null);
        } else {
            refund.setStatus(RefundStatus.REJECTED);
            refund.setReviewedAt(LocalDateTime.now());
            refundMapper.updateById(refund);
            FoodOrder order = orderService.requireOrder(refund.getOrderId());
            order.setRefundStatus(RefundStatus.REJECTED);
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            orderService.appendLog(order, "平台驳回退款申请", "ADMIN", null);
            notificationHandler.notifyOrderUpdate(order, "退款申请未通过");
        }
    }

    public List<Map<String, Object>> merchants() {
        return merchantMapper.selectList(
            Wrappers.<Merchant>lambdaQuery().orderByDesc(Merchant::getMonthlySales)
        ).stream().map(merchant -> {
            Map<String, Object> view = catalogService.merchantSummary(merchant);
            view.put("productCount", 0);
            return view;
        }).toList();
    }

    public Map<String, Object> toggleMerchant(Long merchantId, boolean open) {
        Merchant merchant = catalogService.requireMerchant(merchantId);
        merchant.setOpen(open);
        merchantMapper.updateById(merchant);
        return catalogService.merchantSummary(merchant);
    }

    private Map<String, Object> riderView(Account rider) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        List<DeliveryTask> tasks = deliveryTaskMapper.selectList(
            Wrappers.<DeliveryTask>lambdaQuery()
                .eq(DeliveryTask::getRiderId, rider.getId())
                .ge(DeliveryTask::getAssignedAt, start)
        );
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", rider.getId());
        view.put("name", rider.getDisplayName());
        view.put("phone", rider.getPhone());
        view.put("todayTasks", tasks.size());
        view.put("activeTasks", tasks.stream()
            .filter(task -> task.getStatus() == DeliveryStatus.ASSIGNED
                || task.getStatus() == DeliveryStatus.PICKED_UP)
            .count());
        return view;
    }
}
