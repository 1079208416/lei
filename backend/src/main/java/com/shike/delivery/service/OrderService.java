package com.shike.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shike.delivery.common.ApiException;
import com.shike.delivery.domain.Account;
import com.shike.delivery.domain.CartItem;
import com.shike.delivery.domain.DeliveryTask;
import com.shike.delivery.domain.DomainEnums.DeliveryStatus;
import com.shike.delivery.domain.DomainEnums.OrderStatus;
import com.shike.delivery.domain.DomainEnums.PaymentStatus;
import com.shike.delivery.domain.DomainEnums.RefundStatus;
import com.shike.delivery.domain.FoodOrder;
import com.shike.delivery.domain.Merchant;
import com.shike.delivery.domain.OrderItem;
import com.shike.delivery.domain.OrderStatusLog;
import com.shike.delivery.domain.Product;
import com.shike.delivery.domain.RefundRequest;
import com.shike.delivery.domain.UserAddress;
import com.shike.delivery.mapper.AccountMapper;
import com.shike.delivery.mapper.DeliveryTaskMapper;
import com.shike.delivery.mapper.FoodOrderMapper;
import com.shike.delivery.mapper.MerchantMapper;
import com.shike.delivery.mapper.OrderItemMapper;
import com.shike.delivery.mapper.OrderStatusLogMapper;
import com.shike.delivery.mapper.ProductMapper;
import com.shike.delivery.mapper.RefundRequestMapper;
import com.shike.delivery.notification.NotificationWebSocketHandler;
import com.shike.delivery.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private final FoodOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final RefundRequestMapper refundMapper;
    private final DeliveryTaskMapper deliveryTaskMapper;
    private final MerchantMapper merchantMapper;
    private final ProductMapper productMapper;
    private final AccountMapper accountMapper;
    private final CartService cartService;
    private final AddressService addressService;
    private final NotificationWebSocketHandler notificationHandler;

    public OrderService(
        FoodOrderMapper orderMapper,
        OrderItemMapper orderItemMapper,
        OrderStatusLogMapper statusLogMapper,
        RefundRequestMapper refundMapper,
        DeliveryTaskMapper deliveryTaskMapper,
        MerchantMapper merchantMapper,
        ProductMapper productMapper,
        AccountMapper accountMapper,
        CartService cartService,
        AddressService addressService,
        NotificationWebSocketHandler notificationHandler
    ) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.statusLogMapper = statusLogMapper;
        this.refundMapper = refundMapper;
        this.deliveryTaskMapper = deliveryTaskMapper;
        this.merchantMapper = merchantMapper;
        this.productMapper = productMapper;
        this.accountMapper = accountMapper;
        this.cartService = cartService;
        this.addressService = addressService;
        this.notificationHandler = notificationHandler;
    }

    @Transactional
    public Map<String, Object> create(Long accountId, CreateOrderRequest request) {
        Merchant merchant = merchantMapper.selectById(request.merchantId());
        if (merchant == null) {
            throw ApiException.notFound("商户不存在");
        }
        if (!Boolean.TRUE.equals(merchant.getOpen())) {
            throw ApiException.badRequest("商户当前休息中");
        }
        UserAddress address = addressService.requireOwned(accountId, request.addressId());
        List<CartItem> cartItems = cartService.rawItems(accountId, merchant.getId());
        if (cartItems.isEmpty()) {
            throw ApiException.badRequest("购物车为空");
        }

        int itemAmount = 0;
        for (CartItem cartItem : cartItems) {
            Product product = productMapper.selectById(cartItem.getProductId());
            if (product == null || Boolean.TRUE.equals(product.getSoldOut())) {
                throw ApiException.badRequest("部分商品已售罄，请刷新购物车");
            }
            itemAmount += cartItem.getUnitPrice() * cartItem.getQuantity();
        }
        if (itemAmount < merchant.getMinOrderAmount()) {
            throw ApiException.badRequest("订单未达到起送价");
        }

        LocalDateTime now = LocalDateTime.now();
        FoodOrder order = new FoodOrder();
        order.setOrderNo(nextOrderNo(now));
        order.setAccountId(accountId);
        order.setMerchantId(merchant.getId());
        order.setMerchantName(merchant.getName());
        order.setStatus(OrderStatus.WAITING_PAYMENT);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setRefundStatus(RefundStatus.NONE);
        order.setAddressSnapshot(address.getAddress() + " " + nullToEmpty(address.getDetail()));
        order.setContactName(address.getContactName());
        order.setContactPhone(address.getPhone());
        order.setItemAmount(itemAmount);
        order.setPackingFee(merchant.getPackingFee());
        order.setDeliveryFee(merchant.getDeliveryFee());
        order.setTotalAmount(itemAmount + merchant.getPackingFee() + merchant.getDeliveryFee());
        order.setRemark(request.remark());
        order.setEstimatedDeliveryAt(now.plusMinutes(merchant.getEtaMinutes()));
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        orderMapper.insert(order);

        for (CartItem cartItem : cartItems) {
            Product product = productMapper.selectById(cartItem.getProductId());
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductImage(product.getImageUrl());
            item.setOptionsSnapshot(cartItem.getOptionsSnapshot());
            item.setUnitPrice(cartItem.getUnitPrice());
            item.setQuantity(cartItem.getQuantity());
            item.setTotalPrice(cartItem.getUnitPrice() * cartItem.getQuantity());
            orderItemMapper.insert(item);
        }
        appendLog(order, "订单已创建，等待支付", "USER", accountId);
        cartService.clear(accountId, merchant.getId());
        return detail(order.getId(), accountId, "USER");
    }

    @Transactional
    public Map<String, Object> pay(Long accountId, Long orderId) {
        FoodOrder order = requireOwned(accountId, orderId);
        if (order.getStatus() != OrderStatus.WAITING_PAYMENT) {
            throw ApiException.conflict("当前订单状态不能支付");
        }
        LocalDateTime now = LocalDateTime.now();
        int changed = orderMapper.update(
            null,
            Wrappers.<FoodOrder>lambdaUpdate()
                .eq(FoodOrder::getId, orderId)
                .eq(FoodOrder::getStatus, OrderStatus.WAITING_PAYMENT)
                .set(FoodOrder::getStatus, OrderStatus.WAITING_ACCEPT)
                .set(FoodOrder::getPaymentStatus, PaymentStatus.PAID)
                .set(FoodOrder::getPaidAt, now)
                .set(FoodOrder::getUpdatedAt, now)
        );
        if (changed == 0) {
            throw ApiException.conflict("订单状态已变化，请刷新");
        }
        reload(order);
        appendLog(order, "支付成功，等待商家接单", "USER", accountId);
        notificationHandler.notifyOrderUpdate(order, "有新的已支付订单");
        return orderView(order);
    }

    public List<Map<String, Object>> listForUser(Long accountId, OrderStatus status) {
        var query = Wrappers.<FoodOrder>lambdaQuery()
            .eq(FoodOrder::getAccountId, accountId)
            .orderByDesc(FoodOrder::getCreatedAt);
        if (status != null) {
            query.eq(FoodOrder::getStatus, status);
        }
        return orderMapper.selectList(query).stream().map(this::orderView).toList();
    }

    @Transactional
    public Map<String, Object> cancel(Long accountId, Long orderId, String reason) {
        FoodOrder order = requireOwned(accountId, orderId);
        if (order.getStatus() == OrderStatus.WAITING_PAYMENT) {
            transition(order, OrderStatus.CANCELLED, order.getPaymentStatus(),
                reason == null || reason.isBlank() ? "用户取消订单" : reason,
                "USER", accountId);
        } else if (order.getStatus() == OrderStatus.WAITING_ACCEPT
            && order.getPaymentStatus() == PaymentStatus.PAID) {
            transition(order, OrderStatus.CANCELLED, PaymentStatus.REFUNDED,
                "商家接单前用户取消，已原路退款", "USER", accountId);
            order.setCancelReason(reason);
            order.setRefundStatus(RefundStatus.COMPLETED);
            orderMapper.updateById(order);
            createCompletedRefund(order, "用户取消");
        } else {
            throw ApiException.conflict("当前订单状态不能直接取消，请申请售后");
        }
        notificationHandler.notifyOrderUpdate(order, "订单已取消");
        return orderView(order);
    }

    @Transactional
    public Map<String, Object> requestRefund(Long accountId, Long orderId, String reason) {
        FoodOrder order = requireOwned(accountId, orderId);
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw ApiException.badRequest("订单尚未支付");
        }
        if (order.getRefundStatus() == RefundStatus.REQUESTED) {
            throw ApiException.conflict("退款申请正在处理中");
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REJECTED) {
            throw ApiException.conflict("订单已结束");
        }
        RefundRequest refund = new RefundRequest();
        refund.setOrderId(order.getId());
        refund.setAccountId(accountId);
        refund.setReason(reason == null || reason.isBlank() ? "用户申请退款" : reason);
        refund.setAmount(order.getTotalAmount());
        refund.setStatus(RefundStatus.REQUESTED);
        refund.setCreatedAt(LocalDateTime.now());
        refundMapper.insert(refund);

        order.setRefundStatus(RefundStatus.REQUESTED);
        order.setPaymentStatus(PaymentStatus.REFUNDING);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        appendLog(order, "用户提交退款申请：" + refund.getReason(), "USER", accountId);
        notificationHandler.notifyOrderUpdate(order, "收到新的退款申请");
        return orderView(order);
    }

    @Transactional
    public void completeRefund(Long orderId, String operatorRole, Long operatorId) {
        FoodOrder order = requireOrder(orderId);
        RefundRequest refund = refundMapper.selectOne(
            Wrappers.<RefundRequest>lambdaQuery()
                .eq(RefundRequest::getOrderId, orderId)
                .orderByDesc(RefundRequest::getId)
                .last("limit 1")
        );
        if (refund == null) {
            throw ApiException.notFound("退款申请不存在");
        }
        refund.setStatus(RefundStatus.COMPLETED);
        refund.setReviewedAt(LocalDateTime.now());
        refundMapper.updateById(refund);

        if (order.getStatus() != OrderStatus.COMPLETED) {
            order.setStatus(OrderStatus.CANCELLED);
        }
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setRefundStatus(RefundStatus.COMPLETED);
        order.setCancelReason(refund.getReason());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        appendLog(order, "退款已完成", operatorRole, operatorId);
        deliveryTaskMapper.update(
            null,
            Wrappers.<DeliveryTask>lambdaUpdate()
                .eq(DeliveryTask::getOrderId, orderId)
                .in(DeliveryTask::getStatus, DeliveryStatus.ASSIGNED, DeliveryStatus.PICKED_UP)
                .set(DeliveryTask::getStatus, DeliveryStatus.CANCELLED)
        );
        notificationHandler.notifyOrderUpdate(order, "退款已完成");
    }

    public Map<String, Object> detail(Long orderId, Long actorId, String actorRole) {
        FoodOrder order = requireOrder(orderId);
        if ("USER".equals(actorRole) && !order.getAccountId().equals(actorId)) {
            throw ApiException.forbidden("不能查看他人订单");
        }
        if ("MERCHANT".equals(actorRole)) {
            Long merchantId = SecurityUtils.merchantId();
            if (!order.getMerchantId().equals(merchantId)) {
                throw ApiException.forbidden("不能查看其他商户订单");
            }
        }
        if ("RIDER".equals(actorRole) && !actorId.equals(order.getRiderId())) {
            throw ApiException.forbidden("不能查看未分配订单");
        }
        return orderView(order);
    }

    public Map<String, Object> orderView(FoodOrder order) {
        List<OrderItem> items = orderItemMapper.selectList(
            Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, order.getId())
        );
        List<OrderStatusLog> logs = statusLogMapper.selectList(
            Wrappers.<OrderStatusLog>lambdaQuery()
                .eq(OrderStatusLog::getOrderId, order.getId())
                .orderByAsc(OrderStatusLog::getCreatedAt)
        );
        DeliveryTask task = deliveryTaskMapper.selectOne(
            Wrappers.<DeliveryTask>lambdaQuery().eq(DeliveryTask::getOrderId, order.getId())
        );
        Account rider = order.getRiderId() == null ? null : accountMapper.selectById(order.getRiderId());

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", order.getId());
        view.put("orderNo", order.getOrderNo());
        view.put("merchantId", order.getMerchantId());
        view.put("merchantName", order.getMerchantName());
        view.put("status", order.getStatus().name());
        view.put("paymentStatus", order.getPaymentStatus().name());
        view.put("refundStatus", order.getRefundStatus().name());
        view.put("addressSnapshot", order.getAddressSnapshot());
        view.put("contactName", order.getContactName());
        view.put("contactPhone", order.getContactPhone());
        view.put("itemAmount", order.getItemAmount());
        view.put("packingFee", order.getPackingFee());
        view.put("deliveryFee", order.getDeliveryFee());
        view.put("totalAmount", order.getTotalAmount());
        view.put("remark", order.getRemark());
        view.put("riderId", order.getRiderId());
        view.put("riderName", rider == null ? null : rider.getDisplayName());
        view.put("riderPhone", rider == null ? null : rider.getPhone());
        view.put("deliveryStatus", task == null ? null : task.getStatus().name());
        view.put("cancelReason", order.getCancelReason());
        view.put("estimatedDeliveryAt", order.getEstimatedDeliveryAt());
        view.put("createdAt", order.getCreatedAt());
        view.put("paidAt", order.getPaidAt());
        view.put("completedAt", order.getCompletedAt());
        view.put("items", items.stream().map(item -> {
            Map<String, Object> itemView = new LinkedHashMap<>();
            itemView.put("id", item.getId());
            itemView.put("productId", item.getProductId());
            itemView.put("productName", item.getProductName());
            itemView.put("productImage", item.getProductImage());
            itemView.put("optionsSnapshot", item.getOptionsSnapshot());
            itemView.put("unitPrice", item.getUnitPrice());
            itemView.put("quantity", item.getQuantity());
            itemView.put("totalPrice", item.getTotalPrice());
            return itemView;
        }).toList());
        view.put("timeline", logs.stream().map(log -> {
            Map<String, Object> logView = new LinkedHashMap<>();
            logView.put("status", log.getStatus().name());
            logView.put("paymentStatus", log.getPaymentStatus().name());
            logView.put("note", log.getNote());
            logView.put("createdAt", log.getCreatedAt());
            return logView;
        }).toList());
        return view;
    }

    public FoodOrder requireOrder(Long orderId) {
        FoodOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw ApiException.notFound("订单不存在");
        }
        return order;
    }

    public FoodOrder requireOwned(Long accountId, Long orderId) {
        FoodOrder order = requireOrder(orderId);
        if (!accountId.equals(order.getAccountId())) {
            throw ApiException.forbidden("不能操作他人订单");
        }
        return order;
    }

    public void appendLog(FoodOrder order, String note, String role, Long operatorId) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(order.getId());
        log.setStatus(order.getStatus());
        log.setPaymentStatus(order.getPaymentStatus());
        log.setNote(note);
        log.setOperatorRole(role);
        log.setOperatorId(operatorId);
        log.setCreatedAt(LocalDateTime.now());
        statusLogMapper.insert(log);
    }

    public void transition(
        FoodOrder order,
        OrderStatus status,
        PaymentStatus paymentStatus,
        String note,
        String role,
        Long operatorId
    ) {
        OrderStatus previous = order.getStatus();
        int changed = orderMapper.update(
            null,
            Wrappers.<FoodOrder>lambdaUpdate()
                .eq(FoodOrder::getId, order.getId())
                .eq(FoodOrder::getStatus, previous)
                .set(FoodOrder::getStatus, status)
                .set(FoodOrder::getPaymentStatus, paymentStatus)
                .set(FoodOrder::getUpdatedAt, LocalDateTime.now())
        );
        if (changed == 0) {
            throw ApiException.conflict("订单状态已变化，请刷新后重试");
        }
        order.setStatus(status);
        order.setPaymentStatus(paymentStatus);
        order.setUpdatedAt(LocalDateTime.now());
        appendLog(order, note, role, operatorId);
    }

    private void createCompletedRefund(FoodOrder order, String reason) {
        RefundRequest refund = new RefundRequest();
        refund.setOrderId(order.getId());
        refund.setAccountId(order.getAccountId());
        refund.setReason(reason);
        refund.setAmount(order.getTotalAmount());
        refund.setStatus(RefundStatus.COMPLETED);
        refund.setCreatedAt(LocalDateTime.now());
        refund.setReviewedAt(LocalDateTime.now());
        refundMapper.insert(refund);
    }

    private void reload(FoodOrder order) {
        FoodOrder fresh = orderMapper.selectById(order.getId());
        order.setStatus(fresh.getStatus());
        order.setPaymentStatus(fresh.getPaymentStatus());
        order.setPaidAt(fresh.getPaidAt());
        order.setUpdatedAt(fresh.getUpdatedAt());
    }

    private String nextOrderNo(LocalDateTime now) {
        String time = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "SK" + time + suffix;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record CreateOrderRequest(Long merchantId, Long addressId, String remark) {
    }
}
