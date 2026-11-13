package com.shike.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shike.delivery.common.ApiException;
import com.shike.delivery.domain.CartItem;
import com.shike.delivery.domain.Merchant;
import com.shike.delivery.domain.Product;
import com.shike.delivery.domain.ProductOption;
import com.shike.delivery.domain.ProductOptionGroup;
import com.shike.delivery.mapper.CartItemMapper;
import com.shike.delivery.mapper.ProductMapper;
import com.shike.delivery.mapper.ProductOptionGroupMapper;
import com.shike.delivery.mapper.ProductOptionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final ProductOptionGroupMapper optionGroupMapper;
    private final ProductOptionMapper optionMapper;
    private final CatalogService catalogService;

    public CartService(
        CartItemMapper cartItemMapper,
        ProductMapper productMapper,
        ProductOptionGroupMapper optionGroupMapper,
        ProductOptionMapper optionMapper,
        CatalogService catalogService
    ) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.optionGroupMapper = optionGroupMapper;
        this.optionMapper = optionMapper;
        this.catalogService = catalogService;
    }

    @Transactional
    public Map<String, Object> add(Long accountId, AddCartItemRequest request) {
        Merchant merchant = catalogService.requireMerchant(request.merchantId());
        if (!Boolean.TRUE.equals(merchant.getOpen())) {
            throw ApiException.badRequest("商户当前休息中");
        }
        Product product = productMapper.selectById(request.productId());
        if (product == null || !request.merchantId().equals(product.getMerchantId())) {
            throw ApiException.notFound("商品不存在");
        }
        if (Boolean.TRUE.equals(product.getSoldOut())) {
            throw ApiException.badRequest("商品已售罄");
        }

        List<ProductOptionGroup> groups = optionGroupMapper.selectList(
            Wrappers.<ProductOptionGroup>lambdaQuery()
                .eq(ProductOptionGroup::getProductId, product.getId())
                .orderByAsc(ProductOptionGroup::getSortOrder)
        );
        List<Long> groupIds = groups.stream().map(ProductOptionGroup::getId).toList();
        List<ProductOption> allOptions = groupIds.isEmpty()
            ? List.of()
            : optionMapper.selectList(
                Wrappers.<ProductOption>lambdaQuery()
                    .in(ProductOption::getGroupId, groupIds)
                    .orderByAsc(ProductOption::getSortOrder)
            );

        List<Long> selectedIds = request.optionIds() == null ? List.of() : request.optionIds();
        List<ProductOption> selectedOptions = allOptions.stream()
            .filter(option -> selectedIds.contains(option.getId()))
            .toList();
        if (selectedIds.size() != selectedOptions.size()) {
            throw ApiException.badRequest("商品规格选择无效");
        }
        Map<Long, List<ProductOption>> selectedByGroup = selectedOptions.stream()
            .collect(Collectors.groupingBy(ProductOption::getGroupId));
        for (ProductOptionGroup group : groups) {
            List<ProductOption> selected = selectedByGroup.getOrDefault(group.getId(), List.of());
            if (Boolean.TRUE.equals(group.getRequiredFlag()) && selected.size() != 1) {
                throw ApiException.badRequest("请选择" + group.getName());
            }
            if (selected.size() > 1) {
                throw ApiException.badRequest(group.getName() + "只能选择一项");
            }
        }

        List<Long> sortedIds = selectedOptions.stream()
            .map(ProductOption::getId)
            .sorted()
            .toList();
        String optionIds = sortedIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String optionsSnapshot = selectedOptions.stream()
            .map(ProductOption::getName)
            .collect(Collectors.joining(" / "));
        int optionPrice = selectedOptions.stream()
            .mapToInt(ProductOption::getPriceDelta)
            .sum();
        int unitPrice = product.getPrice() + optionPrice;
        String cartKey = accountId + ":" + product.getId() + ":" + optionIds;

        CartItem existing = cartItemMapper.selectOne(
            Wrappers.<CartItem>lambdaQuery().eq(CartItem::getCartKey, cartKey)
        );
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.quantity());
            existing.setUpdatedAt(LocalDateTime.now());
            cartItemMapper.updateById(existing);
        } else {
            CartItem item = new CartItem();
            item.setAccountId(accountId);
            item.setMerchantId(request.merchantId());
            item.setProductId(product.getId());
            item.setQuantity(request.quantity());
            item.setOptionIds(optionIds);
            item.setOptionsSnapshot(optionsSnapshot);
            item.setUnitPrice(unitPrice);
            item.setCartKey(cartKey);
            item.setCreatedAt(LocalDateTime.now());
            item.setUpdatedAt(LocalDateTime.now());
            cartItemMapper.insert(item);
        }
        return view(accountId, request.merchantId());
    }

    public Map<String, Object> view(Long accountId, Long merchantId) {
        Merchant merchant = catalogService.requireMerchant(merchantId);
        List<CartItem> items = cartItemMapper.selectList(
            Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getAccountId, accountId)
                .eq(CartItem::getMerchantId, merchantId)
                .orderByAsc(CartItem::getCreatedAt)
        );
        List<Map<String, Object>> itemViews = new ArrayList<>();
        int itemAmount = 0;
        for (CartItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product == null) {
                continue;
            }
            int subtotal = item.getUnitPrice() * item.getQuantity();
            itemAmount += subtotal;
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", item.getId());
            view.put("productId", product.getId());
            view.put("name", product.getName());
            view.put("imageUrl", product.getImageUrl());
            view.put("optionsSnapshot", item.getOptionsSnapshot());
            view.put("unitPrice", item.getUnitPrice());
            view.put("quantity", item.getQuantity());
            view.put("subtotal", subtotal);
            itemViews.add(view);
        }
        int packingFee = itemViews.isEmpty() ? 0 : merchant.getPackingFee();
        int deliveryFee = itemViews.isEmpty() ? 0 : merchant.getDeliveryFee();
        int totalAmount = itemAmount + packingFee + deliveryFee;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("merchant", catalogService.merchantSummary(merchant));
        result.put("items", itemViews);
        result.put("itemAmount", itemAmount);
        result.put("packingFee", packingFee);
        result.put("deliveryFee", deliveryFee);
        result.put("totalAmount", totalAmount);
        result.put("minOrderAmount", merchant.getMinOrderAmount());
        result.put("canCheckout", !itemViews.isEmpty()
            && itemAmount >= merchant.getMinOrderAmount()
            && Boolean.TRUE.equals(merchant.getOpen()));
        return result;
    }

    public Map<String, Object> updateQuantity(Long accountId, Long itemId, int quantity) {
        CartItem item = requireOwned(accountId, itemId);
        if (quantity <= 0) {
            cartItemMapper.deleteById(itemId);
        } else if (quantity > 99) {
            throw ApiException.badRequest("单个商品数量不能超过 99");
        } else {
            item.setQuantity(quantity);
            item.setUpdatedAt(LocalDateTime.now());
            cartItemMapper.updateById(item);
        }
        return view(accountId, item.getMerchantId());
    }

    public Map<String, Object> remove(Long accountId, Long itemId) {
        CartItem item = requireOwned(accountId, itemId);
        cartItemMapper.deleteById(itemId);
        return view(accountId, item.getMerchantId());
    }

    public void clear(Long accountId, Long merchantId) {
        cartItemMapper.delete(
            Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getAccountId, accountId)
                .eq(CartItem::getMerchantId, merchantId)
        );
    }

    public List<CartItem> rawItems(Long accountId, Long merchantId) {
        return cartItemMapper.selectList(
            Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getAccountId, accountId)
                .eq(CartItem::getMerchantId, merchantId)
                .orderByAsc(CartItem::getCreatedAt)
        );
    }

    private CartItem requireOwned(Long accountId, Long itemId) {
        CartItem item = cartItemMapper.selectById(itemId);
        if (item == null || !accountId.equals(item.getAccountId())) {
            throw ApiException.notFound("购物车商品不存在");
        }
        return item;
    }

    public record AddCartItemRequest(Long merchantId, Long productId, Integer quantity, List<Long> optionIds) {
    }
}
