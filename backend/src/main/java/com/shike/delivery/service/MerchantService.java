package com.shike.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shike.delivery.common.ApiException;
import com.shike.delivery.domain.DomainEnums.OrderStatus;
import com.shike.delivery.domain.DomainEnums.PaymentStatus;
import com.shike.delivery.domain.DomainEnums.RefundStatus;
import com.shike.delivery.domain.FoodOrder;
import com.shike.delivery.domain.Merchant;
import com.shike.delivery.domain.Product;
import com.shike.delivery.domain.ProductCategory;
import com.shike.delivery.domain.RefundRequest;
import com.shike.delivery.mapper.FoodOrderMapper;
import com.shike.delivery.mapper.MerchantMapper;
import com.shike.delivery.mapper.ProductCategoryMapper;
import com.shike.delivery.mapper.ProductMapper;
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
public class MerchantService {

    private final FoodOrderMapper orderMapper;
    private final MerchantMapper merchantMapper;
    private final ProductCategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final RefundRequestMapper refundMapper;
    private final OrderService orderService;
    private final CatalogService catalogService;
    private final NotificationWebSocketHandler notificationHandler;

    public MerchantService(
        FoodOrderMapper orderMapper,
        MerchantMapper merchantMapper,
        ProductCategoryMapper categoryMapper,
        ProductMapper productMapper,
        RefundRequestMapper refundMapper,
        OrderService orderService,
        CatalogService catalogService,
        NotificationWebSocketHandler notificationHandler
    ) {
        this.orderMapper = orderMapper;
        this.merchantMapper = merchantMapper;
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
        this.refundMapper = refundMapper;
        this.orderService = orderService;
        this.catalogService = catalogService;
        this.notificationHandler = notificationHandler;
    }

    public Map<String, Object> dashboard(Long merchantId) {
        Merchant merchant = catalogService.requireMerchant(merchantId);
        LocalDateTime start = LocalDate.now().atStartOfDay();
        List<FoodOrder> todayOrders = orderMapper.selectList(
            Wrappers.<FoodOrder>lambdaQuery()
                .eq(FoodOrder::getMerchantId, merchantId)
                .ge(FoodOrder::getCreatedAt, start)
        );
        long pendingAccept = todayOrders.stream()
            .filter(order -> order.getStatus() == OrderStatus.WAITING_ACCEPT)
            .count();
        long activeOrders = todayOrders.stream()
            .filter(order -> order.getStatus() == OrderStatus.PREPARING
                || order.getStatus() == OrderStatus.WAITING_PICKUP
                || order.getStatus() == OrderStatus.DELIVERING)
            .count();
        long completed = todayOrders.stream()
            .filter(order -> order.getStatus() == OrderStatus.COMPLETED)
            .count();
        int revenue = todayOrders.stream()
            .filter(order -> order.getPaymentStatus() == PaymentStatus.PAID
                || order.getPaymentStatus() == PaymentStatus.REFUNDING)
            .mapToInt(FoodOrder::getTotalAmount)
            .sum();

        Map<String, Object> view = new LinkedHashMap<>();
        view.put("merchant", catalogService.merchantSummary(merchant));
        view.put("orderCount", todayOrders.size());
        view.put("pendingAccept", pendingAccept);
        view.put("activeOrders", activeOrders);
        view.put("completedOrders", completed);
        view.put("revenue", revenue);
        return view;
    }

    public List<Map<String, Object>> orders(Long merchantId, OrderStatus status) {
        var query = Wrappers.<FoodOrder>lambdaQuery()
            .eq(FoodOrder::getMerchantId, merchantId)
            .orderByDesc(FoodOrder::getCreatedAt);
        if (status != null) {
            query.eq(FoodOrder::getStatus, status);
        }
        return orderMapper.selectList(query).stream().map(orderService::orderView).toList();
    }

    @Transactional
    public Map<String, Object> accept(Long merchantId, Long orderId) {
        FoodOrder order = requireMerchantOrder(merchantId, orderId);
        if (order.getStatus() != OrderStatus.WAITING_ACCEPT) {
            throw ApiException.conflict("订单当前不能接单");
        }
        orderService.transition(
            order,
            OrderStatus.PREPARING,
            PaymentStatus.PAID,
            "商家已接单，开始制作",
            "MERCHANT",
            merchantId
        );
        order.setAcceptedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        notificationHandler.notifyOrderUpdate(order, "商家已接单，正在制作");
        return orderService.orderView(order);
    }

    @Transactional
    public Map<String, Object> reject(Long merchantId, Long orderId, String reason) {
        FoodOrder order = requireMerchantOrder(merchantId, orderId);
        if (order.getStatus() != OrderStatus.WAITING_ACCEPT) {
            throw ApiException.conflict("订单当前不能拒单");
        }
        orderService.transition(
            order,
            OrderStatus.REJECTED,
            PaymentStatus.REFUNDED,
            "商家拒单：" + (reason == null ? "无法接单" : reason),
            "MERCHANT",
            merchantId
        );
        order.setRefundStatus(RefundStatus.COMPLETED);
        order.setCancelReason(reason == null ? "商家无法接单" : reason);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        RefundRequest refund = new RefundRequest();
        refund.setOrderId(order.getId());
        refund.setAccountId(order.getAccountId());
        refund.setReason(order.getCancelReason());
        refund.setAmount(order.getTotalAmount());
        refund.setStatus(RefundStatus.COMPLETED);
        refund.setCreatedAt(LocalDateTime.now());
        refund.setReviewedAt(LocalDateTime.now());
        refundMapper.insert(refund);
        notificationHandler.notifyOrderUpdate(order, "商家未能接单，订单已退款");
        return orderService.orderView(order);
    }

    @Transactional
    public Map<String, Object> ready(Long merchantId, Long orderId) {
        FoodOrder order = requireMerchantOrder(merchantId, orderId);
        if (order.getStatus() != OrderStatus.PREPARING) {
            throw ApiException.conflict("订单当前不能标记出餐");
        }
        orderService.transition(
            order,
            OrderStatus.WAITING_PICKUP,
            PaymentStatus.PAID,
            "商家已出餐，等待骑手取餐",
            "MERCHANT",
            merchantId
        );
        order.setReadyAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        notificationHandler.notifyOrderUpdate(order, "餐品已出餐，等待骑手取餐");
        return orderService.orderView(order);
    }

    public Map<String, Object> toggleStore(Long merchantId, boolean open) {
        Merchant merchant = catalogService.requireMerchant(merchantId);
        merchant.setOpen(open);
        merchantMapper.updateById(merchant);
        return catalogService.merchantSummary(merchant);
    }

    public Map<String, Object> menu(Long merchantId) {
        return catalogService.merchantMenu(merchantId);
    }

    public ProductCategory createCategory(Long merchantId, CategoryInput input) {
        ProductCategory category = new ProductCategory();
        category.setMerchantId(merchantId);
        category.setName(input.name());
        category.setSortOrder(input.sortOrder() == null ? 99 : input.sortOrder());
        categoryMapper.insert(category);
        return category;
    }

    public ProductCategory updateCategory(Long merchantId, Long categoryId, CategoryInput input) {
        ProductCategory category = requireCategory(merchantId, categoryId);
        category.setName(input.name());
        category.setSortOrder(input.sortOrder() == null ? category.getSortOrder() : input.sortOrder());
        categoryMapper.updateById(category);
        return category;
    }

    public void deleteCategory(Long merchantId, Long categoryId) {
        requireCategory(merchantId, categoryId);
        long productCount = productMapper.selectCount(
            Wrappers.<Product>lambdaQuery().eq(Product::getCategoryId, categoryId)
        );
        if (productCount > 0) {
            throw ApiException.badRequest("请先移除该分类下的商品");
        }
        categoryMapper.deleteById(categoryId);
    }

    public Product createProduct(Long merchantId, ProductInput input) {
        requireCategory(merchantId, input.categoryId());
        Product product = new Product();
        product.setMerchantId(merchantId);
        applyProduct(product, input);
        product.setMonthlySales(0);
        product.setCreatedAt(LocalDateTime.now());
        productMapper.insert(product);
        return product;
    }

    public Product updateProduct(Long merchantId, Long productId, ProductInput input) {
        Product product = requireProduct(merchantId, productId);
        requireCategory(merchantId, input.categoryId());
        applyProduct(product, input);
        productMapper.updateById(product);
        return product;
    }

    public Product toggleSoldOut(Long merchantId, Long productId, boolean soldOut) {
        Product product = requireProduct(merchantId, productId);
        product.setSoldOut(soldOut);
        productMapper.updateById(product);
        return product;
    }

    public void deleteProduct(Long merchantId, Long productId) {
        requireProduct(merchantId, productId);
        productMapper.deleteById(productId);
    }

    private void applyProduct(Product product, ProductInput input) {
        product.setCategoryId(input.categoryId());
        product.setName(input.name());
        product.setDescription(input.description());
        product.setImageUrl(input.imageUrl());
        product.setPrice(input.price());
        product.setOriginalPrice(input.originalPrice());
        product.setSoldOut(Boolean.TRUE.equals(input.soldOut()));
        product.setSortOrder(input.sortOrder() == null ? 99 : input.sortOrder());
    }

    private FoodOrder requireMerchantOrder(Long merchantId, Long orderId) {
        FoodOrder order = orderService.requireOrder(orderId);
        if (!merchantId.equals(order.getMerchantId())) {
            throw ApiException.forbidden("不能操作其他商户订单");
        }
        return order;
    }

    private ProductCategory requireCategory(Long merchantId, Long categoryId) {
        ProductCategory category = categoryMapper.selectById(categoryId);
        if (category == null || !merchantId.equals(category.getMerchantId())) {
            throw ApiException.notFound("商品分类不存在");
        }
        return category;
    }

    private Product requireProduct(Long merchantId, Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || !merchantId.equals(product.getMerchantId())) {
            throw ApiException.notFound("商品不存在");
        }
        return product;
    }

    public record CategoryInput(String name, Integer sortOrder) {
    }

    public record ProductInput(
        Long categoryId,
        String name,
        String description,
        String imageUrl,
        Integer price,
        Integer originalPrice,
        Boolean soldOut,
        Integer sortOrder
    ) {
    }
}
