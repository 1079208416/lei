package com.shike.delivery.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.shike.delivery.common.ApiException;
import com.shike.delivery.domain.Merchant;
import com.shike.delivery.domain.Product;
import com.shike.delivery.domain.ProductCategory;
import com.shike.delivery.domain.ProductOption;
import com.shike.delivery.domain.ProductOptionGroup;
import com.shike.delivery.mapper.MerchantMapper;
import com.shike.delivery.mapper.ProductCategoryMapper;
import com.shike.delivery.mapper.ProductMapper;
import com.shike.delivery.mapper.ProductOptionGroupMapper;
import com.shike.delivery.mapper.ProductOptionMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private final MerchantMapper merchantMapper;
    private final ProductCategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final ProductOptionGroupMapper optionGroupMapper;
    private final ProductOptionMapper optionMapper;

    public CatalogService(
        MerchantMapper merchantMapper,
        ProductCategoryMapper categoryMapper,
        ProductMapper productMapper,
        ProductOptionGroupMapper optionGroupMapper,
        ProductOptionMapper optionMapper
    ) {
        this.merchantMapper = merchantMapper;
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
        this.optionGroupMapper = optionGroupMapper;
        this.optionMapper = optionMapper;
    }

    public List<Map<String, Object>> listMerchants(String keyword, String category) {
        var query = Wrappers.<Merchant>lambdaQuery()
            .eq(Merchant::getOpen, true)
            .orderByDesc(Merchant::getMonthlySales);
        if (keyword != null && !keyword.isBlank()) {
            query.and(wrapper -> wrapper
                .like(Merchant::getName, keyword.trim())
                .or()
                .like(Merchant::getDescription, keyword.trim())
                .or()
                .like(Merchant::getCategory, keyword.trim())
            );
        }
        if (category != null && !category.isBlank() && !"全部".equals(category)) {
            query.eq(Merchant::getCategory, category);
        }
        return merchantMapper.selectList(query).stream().map(this::merchantSummary).toList();
    }

    public Map<String, Object> merchantDetail(Long merchantId) {
        Merchant merchant = requireMerchant(merchantId);
        List<ProductCategory> categories = categoryMapper.selectList(
            Wrappers.<ProductCategory>lambdaQuery()
                .eq(ProductCategory::getMerchantId, merchantId)
                .orderByAsc(ProductCategory::getSortOrder)
        );
        List<Product> products = productMapper.selectList(
            Wrappers.<Product>lambdaQuery()
                .eq(Product::getMerchantId, merchantId)
                .orderByAsc(Product::getSortOrder)
        );
        List<Long> productIds = products.stream().map(Product::getId).toList();
        List<ProductOptionGroup> groups = productIds.isEmpty()
            ? List.of()
            : optionGroupMapper.selectList(
                Wrappers.<ProductOptionGroup>lambdaQuery()
                    .in(ProductOptionGroup::getProductId, productIds)
                    .orderByAsc(ProductOptionGroup::getSortOrder)
            );
        List<Long> groupIds = groups.stream().map(ProductOptionGroup::getId).toList();
        List<ProductOption> options = groupIds.isEmpty()
            ? List.of()
            : optionMapper.selectList(
                Wrappers.<ProductOption>lambdaQuery()
                    .in(ProductOption::getGroupId, groupIds)
                    .orderByAsc(ProductOption::getSortOrder)
            );

        Map<Long, List<ProductOption>> optionsByGroup = options.stream()
            .collect(Collectors.groupingBy(ProductOption::getGroupId));
        Map<Long, List<ProductOptionGroup>> groupsByProduct = groups.stream()
            .collect(Collectors.groupingBy(ProductOptionGroup::getProductId));
        Map<Long, List<Product>> productsByCategory = products.stream()
            .collect(Collectors.groupingBy(Product::getCategoryId));

        Map<String, Object> result = merchantSummary(merchant);
        result.put("categories", categories.stream().map(category -> {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", category.getId());
            view.put("name", category.getName());
            view.put("products", productsByCategory.getOrDefault(category.getId(), List.of()).stream()
                .map(product -> productView(
                    product,
                    groupsByProduct.getOrDefault(product.getId(), List.of()),
                    optionsByGroup
                ))
                .toList());
            return view;
        }).toList());
        return result;
    }

    public Map<String, Object> merchantSummary(Merchant merchant) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", merchant.getId());
        view.put("name", merchant.getName());
        view.put("description", merchant.getDescription());
        view.put("category", merchant.getCategory());
        view.put("coverUrl", merchant.getCoverUrl());
        view.put("logoUrl", merchant.getLogoUrl());
        view.put("address", merchant.getAddress());
        view.put("rating", merchant.getRating());
        view.put("monthlySales", merchant.getMonthlySales());
        view.put("minOrderAmount", merchant.getMinOrderAmount());
        view.put("deliveryFee", merchant.getDeliveryFee());
        view.put("packingFee", merchant.getPackingFee());
        view.put("etaMinutes", merchant.getEtaMinutes());
        view.put("notice", merchant.getNotice());
        view.put("open", merchant.getOpen());
        return view;
    }

    public Map<String, Object> productView(
        Product product,
        List<ProductOptionGroup> groups,
        Map<Long, List<ProductOption>> optionsByGroup
    ) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", product.getId());
        view.put("merchantId", product.getMerchantId());
        view.put("categoryId", product.getCategoryId());
        view.put("name", product.getName());
        view.put("description", product.getDescription());
        view.put("imageUrl", product.getImageUrl());
        view.put("price", product.getPrice());
        view.put("originalPrice", product.getOriginalPrice());
        view.put("soldOut", product.getSoldOut());
        view.put("monthlySales", product.getMonthlySales());
        view.put("optionGroups", groups.stream()
            .sorted(Comparator.comparing(ProductOptionGroup::getSortOrder))
            .map(group -> {
                Map<String, Object> groupView = new LinkedHashMap<>();
                groupView.put("id", group.getId());
                groupView.put("name", group.getName());
                groupView.put("required", group.getRequiredFlag());
                groupView.put("options", optionsByGroup.getOrDefault(group.getId(), List.of()).stream()
                    .map(option -> Map.of(
                        "id", option.getId(),
                        "name", option.getName(),
                        "priceDelta", option.getPriceDelta()
                    ))
                    .toList());
                return groupView;
            })
            .toList());
        return view;
    }

    public Merchant requireMerchant(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) {
            throw ApiException.notFound("商户不存在");
        }
        return merchant;
    }

    public Map<String, Object> merchantMenu(Long merchantId) {
        Map<String, Object> detail = merchantDetail(merchantId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("merchant", merchantSummary(requireMerchant(merchantId)));
        result.put("categories", detail.get("categories"));
        return result;
    }

    public List<String> categories() {
        return new ArrayList<>(merchantMapper.selectList(
                Wrappers.<Merchant>lambdaQuery()
                    .select(Merchant::getCategory)
                    .eq(Merchant::getOpen, true)
            ).stream()
            .map(Merchant::getCategory)
            .distinct()
            .sorted()
            .toList());
    }
}
