package com.shike.delivery.config;

import com.shike.delivery.domain.Account;
import com.shike.delivery.domain.DomainEnums.AccountRole;
import com.shike.delivery.domain.Merchant;
import com.shike.delivery.domain.Product;
import com.shike.delivery.domain.ProductCategory;
import com.shike.delivery.domain.ProductOption;
import com.shike.delivery.domain.ProductOptionGroup;
import com.shike.delivery.domain.UserAddress;
import com.shike.delivery.mapper.AccountMapper;
import com.shike.delivery.mapper.MerchantMapper;
import com.shike.delivery.mapper.ProductCategoryMapper;
import com.shike.delivery.mapper.ProductMapper;
import com.shike.delivery.mapper.ProductOptionGroupMapper;
import com.shike.delivery.mapper.ProductOptionMapper;
import com.shike.delivery.mapper.UserAddressMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DemoDataInitializer implements ApplicationRunner {

    private final AccountMapper accountMapper;
    private final MerchantMapper merchantMapper;
    private final ProductCategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final ProductOptionGroupMapper optionGroupMapper;
    private final ProductOptionMapper optionMapper;
    private final UserAddressMapper addressMapper;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(
        AccountMapper accountMapper,
        MerchantMapper merchantMapper,
        ProductCategoryMapper categoryMapper,
        ProductMapper productMapper,
        ProductOptionGroupMapper optionGroupMapper,
        ProductOptionMapper optionMapper,
        UserAddressMapper addressMapper,
        PasswordEncoder passwordEncoder
    ) {
        this.accountMapper = accountMapper;
        this.merchantMapper = merchantMapper;
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
        this.optionGroupMapper = optionGroupMapper;
        this.optionMapper = optionMapper;
        this.addressMapper = addressMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (accountMapper.selectCount(null) > 0) {
            return;
        }

        Merchant sichuan = merchant(
            "川味小馆",
            "现炒川味，锅气到家",
            "川湘菜",
            "https://images.unsplash.com/photo-1552566626-52f8b828add9?auto=format&fit=crop&w=1200&q=85",
            "杭州市西湖区文三路 188 号",
            30.2762,
            120.1281,
            "4.9",
            2380,
            2000,
            300,
            200,
            28,
            "本店默认中辣，不能吃辣请在备注中说明。"
        );
        Merchant light = merchant(
            "轻食研究所",
            "认真吃饭，也认真生活",
            "轻食沙拉",
            "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=1200&q=85",
            "杭州市西湖区黄姑山路 29 号",
            30.2791,
            120.1319,
            "4.8",
            1460,
            1500,
            200,
            100,
            25,
            "每日现做，酱汁默认分装。"
        );
        Merchant noodles = merchant(
            "牛市面馆",
            "一碗热汤面，认真对待每一餐",
            "面食",
            "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?auto=format&fit=crop&w=1200&q=85",
            "杭州市西湖区教工路 77 号",
            30.2729,
            120.1236,
            "4.7",
            990,
            1200,
            200,
            100,
            22,
            "面条现煮，高峰期出餐约 20 分钟。"
        );
        Merchant rice = merchant(
            "炙味烤肉饭",
            "炭火香气，一人食也满足",
            "快餐简餐",
            "https://images.unsplash.com/photo-1550966871-3ed3cdb5ed0c?auto=format&fit=crop&w=1200&q=85",
            "杭州市西湖区学院路 50 号",
            30.2738,
            120.1348,
            "4.8",
            1830,
            1800,
            300,
            100,
            26,
            "米饭可免费续一次，请在备注中注明。"
        );

        seedSichuanMenu(sichuan);
        seedLightMenu(light);
        seedNoodleMenu(noodles);
        seedRiceMenu(rice);

        String password = passwordEncoder.encode("demo123");
        account("13800000001", password, "王小明", AccountRole.USER, null);
        account("13800000002", password, "陈店长", AccountRole.MERCHANT, sichuan.getId());
        account("13800000003", password, "李师傅", AccountRole.RIDER, null);
        account("13800000004", password, "平台运营", AccountRole.ADMIN, null);
        account("13800000005", password, "周师傅", AccountRole.RIDER, null);
        account("13800000006", password, "林店长", AccountRole.MERCHANT, light.getId());

        seedAddress();
    }

    private Merchant merchant(
        String name,
        String description,
        String category,
        String cover,
        String address,
        double latitude,
        double longitude,
        String rating,
        int monthlySales,
        int minOrder,
        int deliveryFee,
        int packingFee,
        int eta,
        String notice
    ) {
        Merchant merchant = new Merchant();
        merchant.setName(name);
        merchant.setDescription(description);
        merchant.setCategory(category);
        merchant.setCoverUrl(cover);
        merchant.setLogoUrl(cover);
        merchant.setAddress(address);
        merchant.setLatitude(latitude);
        merchant.setLongitude(longitude);
        merchant.setRating(new BigDecimal(rating));
        merchant.setMonthlySales(monthlySales);
        merchant.setMinOrderAmount(minOrder);
        merchant.setDeliveryFee(deliveryFee);
        merchant.setPackingFee(packingFee);
        merchant.setEtaMinutes(eta);
        merchant.setNotice(notice);
        merchant.setOpen(true);
        merchant.setCreatedAt(LocalDateTime.now());
        merchantMapper.insert(merchant);
        return merchant;
    }

    private void seedSichuanMenu(Merchant merchant) {
        ProductCategory popular = category(merchant.getId(), "招牌热销", 1);
        ProductCategory mains = category(merchant.getId(), "下饭好菜", 2);

        Product kungPao = product(
            merchant.getId(), popular.getId(), "宫保鸡丁",
            "鲜嫩鸡腿肉配花生与干辣椒，咸甜微辣。",
            "https://images.unsplash.com/photo-1525755662778-989d0524087e?auto=format&fit=crop&w=800&q=85",
            2800, 3200, 588, 1
        );
        Product beef = product(
            merchant.getId(), popular.getId(), "水煮牛肉",
            "现片牛肉，麻辣鲜香，配豆芽和莴笋。",
            "https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=800&q=85",
            4200, 4600, 366, 2
        );
        Product tofu = product(
            merchant.getId(), mains.getId(), "麻婆豆腐",
            "软嫩豆腐配牛肉末，麻辣下饭。",
            "https://images.unsplash.com/photo-1563379926898-05f4575a45d8?auto=format&fit=crop&w=800&q=85",
            1800, null, 402, 1
        );
        product(
            merchant.getId(), mains.getId(), "米饭",
            "东北大米，当日现蒸。",
            "https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=800&q=85",
            200, null, 956, 2
        );

        spicyGroup(kungPao.getId());
        spicyGroup(beef.getId());
        portionGroup(tofu.getId());
    }

    private void seedLightMenu(Merchant merchant) {
        ProductCategory salad = category(merchant.getId(), "能量沙拉", 1);
        ProductCategory drinks = category(merchant.getId(), "饮品", 2);
        Product avocado = product(
            merchant.getId(), salad.getId(), "牛油果鸡胸沙拉",
            "香煎鸡胸、牛油果、时蔬和油醋汁。",
            "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=800&q=85",
            3200, 3600, 321, 1
        );
        product(
            merchant.getId(), salad.getId(), "黑椒牛肉卷",
            "全麦饼皮包裹黑椒牛肉和新鲜蔬菜。",
            "https://images.unsplash.com/photo-1540189549336-e6e99c3679fe?auto=format&fit=crop&w=800&q=85",
            3600, null, 208, 2
        );
        product(
            merchant.getId(), drinks.getId(), "冷萃美式",
            "当日冷萃，清爽不酸涩。",
            "https://images.unsplash.com/photo-1517701604599-bb29b565090c?auto=format&fit=crop&w=800&q=85",
            1500, null, 175, 1
        );
        portionGroup(avocado.getId());
    }

    private void seedNoodleMenu(Merchant merchant) {
        ProductCategory noodles = category(merchant.getId(), "招牌面食", 1);
        Product beefNoodle = product(
            merchant.getId(), noodles.getId(), "红烧牛肉面",
            "慢炖牛腩、手工面和大块萝卜。",
            "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?auto=format&fit=crop&w=800&q=85",
            2800, 3000, 490, 1
        );
        Product tomato = product(
            merchant.getId(), noodles.getId(), "番茄鸡蛋面",
            "熟成番茄熬制汤底，酸甜开胃。",
            "https://images.unsplash.com/photo-1552611052-33e04de081de?auto=format&fit=crop&w=800&q=85",
            2000, null, 310, 2
        );
        product(
            merchant.getId(), noodles.getId(), "卤蛋",
            "每日卤制，入味不咸。",
            "https://images.unsplash.com/photo-1587486913049-53fc88980cfc?auto=format&fit=crop&w=800&q=85",
            300, null, 620, 3
        );
        portionGroup(beefNoodle.getId());
        portionGroup(tomato.getId());
    }

    private void seedRiceMenu(Merchant merchant) {
        ProductCategory rice = category(merchant.getId(), "人气套餐", 1);
        Product chicken = product(
            merchant.getId(), rice.getId(), "黑椒鸡腿饭",
            "去骨鸡腿、温泉蛋、时蔬和秘制黑椒汁。",
            "https://images.unsplash.com/photo-1603133872878-684f208fb84b?auto=format&fit=crop&w=800&q=85",
            2500, 2900, 760, 1
        );
        product(
            merchant.getId(), rice.getId(), "照烧肥牛饭",
            "肥牛卷配洋葱和照烧汁，咸甜下饭。",
            "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=800&q=85",
            3200, null, 540, 2
        );
        portionGroup(chicken.getId());
    }

    private ProductCategory category(Long merchantId, String name, int sort) {
        ProductCategory category = new ProductCategory();
        category.setMerchantId(merchantId);
        category.setName(name);
        category.setSortOrder(sort);
        categoryMapper.insert(category);
        return category;
    }

    private Product product(
        Long merchantId,
        Long categoryId,
        String name,
        String description,
        String image,
        int price,
        Integer originalPrice,
        int monthlySales,
        int sort
    ) {
        Product product = new Product();
        product.setMerchantId(merchantId);
        product.setCategoryId(categoryId);
        product.setName(name);
        product.setDescription(description);
        product.setImageUrl(image);
        product.setPrice(price);
        product.setOriginalPrice(originalPrice);
        product.setSoldOut(false);
        product.setMonthlySales(monthlySales);
        product.setSortOrder(sort);
        product.setCreatedAt(LocalDateTime.now());
        productMapper.insert(product);
        return product;
    }

    private void spicyGroup(Long productId) {
        ProductOptionGroup group = optionGroup(productId, "辣度", true, 1);
        option(group.getId(), "微辣", 0, 1);
        option(group.getId(), "中辣", 0, 2);
        option(group.getId(), "特辣", 0, 3);
    }

    private void portionGroup(Long productId) {
        ProductOptionGroup group = optionGroup(productId, "份量", true, 1);
        option(group.getId(), "标准份", 0, 1);
        option(group.getId(), "加量份", 400, 2);
    }

    private ProductOptionGroup optionGroup(Long productId, String name, boolean required, int sort) {
        ProductOptionGroup group = new ProductOptionGroup();
        group.setProductId(productId);
        group.setName(name);
        group.setRequiredFlag(required);
        group.setSortOrder(sort);
        optionGroupMapper.insert(group);
        return group;
    }

    private void option(Long groupId, String name, int priceDelta, int sort) {
        ProductOption option = new ProductOption();
        option.setGroupId(groupId);
        option.setName(name);
        option.setPriceDelta(priceDelta);
        option.setSortOrder(sort);
        optionMapper.insert(option);
    }

    private Account account(
        String phone,
        String passwordHash,
        String name,
        AccountRole role,
        Long merchantId
    ) {
        Account account = new Account();
        account.setPhone(phone);
        account.setPasswordHash(passwordHash);
        account.setDisplayName(name);
        account.setRole(role);
        account.setMerchantId(merchantId);
        account.setActive(true);
        account.setCreatedAt(LocalDateTime.now());
        accountMapper.insert(account);
        return account;
    }

    private void seedAddress() {
        UserAddress address = new UserAddress();
        address.setAccountId(1L);
        address.setLabel("家");
        address.setContactName("王小明");
        address.setPhone("13800000001");
        address.setAddress("云栖公寓 2 幢");
        address.setDetail("502 室");
        address.setLatitude(30.2748);
        address.setLongitude(120.1272);
        address.setDefaultAddress(true);
        address.setCreatedAt(LocalDateTime.now());
        addressMapper.insert(address);
    }
}
