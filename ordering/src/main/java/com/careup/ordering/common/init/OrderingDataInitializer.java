package com.careup.ordering.common.init;

import com.careup.ordering.common.util.PhoneUtils;
import com.careup.ordering.domain.member.entity.Gender;
import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.member.repository.MemberRepository;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.entity.BranchProduct;
import com.careup.ordering.domain.product.entity.Category;
import com.careup.ordering.domain.product.entity.Visibility;
import com.careup.ordering.domain.product.entity.AttributeType;
import com.careup.ordering.domain.product.entity.AttributeValue;
import com.careup.ordering.domain.product.entity.CategoryAttribute;
import com.careup.ordering.domain.product.entity.ProductAttributeValue;
import com.careup.ordering.domain.product.repository.ProductRepository;
import com.careup.ordering.domain.product.repository.BranchProductRepository;
import com.careup.ordering.domain.product.repository.CategoryRepository;
import com.careup.ordering.domain.product.repository.AttributeTypeRepository;
import com.careup.ordering.domain.product.repository.AttributeValueRepository;
import com.careup.ordering.domain.product.repository.CategoryAttributeRepository;
import com.careup.ordering.domain.product.repository.ProductAttributeValueRepository;
import com.careup.ordering.domain.order.entity.Order;
import com.careup.ordering.domain.order.entity.OrderedItem;
import com.careup.ordering.domain.order.entity.OrderStatus;
import com.careup.ordering.domain.order.entity.OrderType;
import com.careup.ordering.domain.order.repository.OrderRepository;
import com.careup.ordering.domain.order.repository.OrderedItemRepository;
import com.careup.ordering.domain.order.event.OrderStatisticsEvent;
import com.careup.ordering.domain.order.kafka.OrderStatisticsProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderingDataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductRepository productRepository;
    private final BranchProductRepository branchProductRepository;
    private final CategoryRepository categoryRepository;
    private final AttributeTypeRepository attributeTypeRepository;
    private final AttributeValueRepository attributeValueRepository;
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;
    private final OrderRepository orderRepository;
    private final OrderedItemRepository orderedItemRepository;

    @Value("${app.product-image-base-url:}")
    private String productImageBaseUrl;

    @Autowired(required = false)
    private com.careup.ordering.domain.product.event.ProductEventProducer productEventProducer;

    @Autowired(required = false)
    private OrderStatisticsProducer orderStatisticsProducer;

    @Override
    @Transactional
    public void run(String... args) {
        // 김채원 / 닉네임: 도도독
        ensureMember(
                "chaewon.kim@careup.com",
                "@care1234",
                "도도독",
                "김채원",
                LocalDate.of(2000, 3, 4),
                "010-1010-1001",
                Gender.W,
                "06236",
                "서울특별시 강남구 테헤란로 152",
                "아크플레이스 20층"
        );

        // 설윤아 / 닉네임: 설장군
        ensureMember(
                "yuna.seol@careup.com",
                "@care1234",
                "설장군",
                "설윤아",
                LocalDate.of(1999, 7, 12),
                "010-2020-2002",
                Gender.W,
                "04799",
                "서울특별시 성동구 성수이로 78",
                "201동 903호"
        );

        // 장원영 / 닉네임: 원영적 사고
        ensureMember(
                "wonyoung.jang@careup.com",
                "@care1234",
                "원영적 사고",
                "장원영",
                LocalDate.of(2002, 8, 31),
                "010-3030-3003",
                Gender.W,
                "04158",
                "서울특별시 마포구 양화로 45",
                "3층 카페존"
        );

        // 속성 타입 및 값 생성
        createAttributeTypesAndValues();
        
        // 카테고리 및 상품/재고 생성
        createCategoriesAndProducts();

        // 주문 데이터 생성 (매출 데이터)
        createOrders();
    }

    /**
     * 속성 타입 및 값 초기 데이터 생성
     */
    private void createAttributeTypesAndValues() {
        // 이미 데이터가 있으면 생성 스킵
        if (attributeTypeRepository.count() > 0) {
            System.out.println("속성 타입이 이미 존재합니다. 초기 데이터 생성을 건너뜁니다.");
            return;
        }

        // 1. 속성 타입 생성
        AttributeType colorType = AttributeType.builder()
                .name("색상")
                .description("상품의 색상")
                .isRequired(true)
                .displayOrder(1)
                .build();
        attributeTypeRepository.save(colorType);

        AttributeType sizeType = AttributeType.builder()
                .name("사이즈")
                .description("상품의 크기")
                .isRequired(false)
                .displayOrder(2)
                .build();
        attributeTypeRepository.save(sizeType);

        AttributeType temperatureType = AttributeType.builder()
                .name("온도")
                .description("음료 온도")
                .isRequired(false)
                .displayOrder(3)
                .build();
        attributeTypeRepository.save(temperatureType);

        // 2. 속성 값 생성
        // 색상 속성 값
        AttributeValue white = AttributeValue.builder()
                .attributeType(colorType)
                .displayName("화이트")
                .displayOrder(1)
                .isActive(true)
                .build();
        attributeValueRepository.save(white);

        AttributeValue black = AttributeValue.builder()
                .attributeType(colorType)
                .displayName("블랙")
                .displayOrder(2)
                .isActive(true)
                .build();
        attributeValueRepository.save(black);

        AttributeValue brown = AttributeValue.builder()
                .attributeType(colorType)
                .displayName("브라운")
                .displayOrder(3)
                .isActive(true)
                .build();
        attributeValueRepository.save(brown);

        // 사이즈 속성 값
        AttributeValue small = AttributeValue.builder()
                .attributeType(sizeType)
                .displayName("S")
                .displayOrder(1)
                .isActive(true)
                .build();
        attributeValueRepository.save(small);

        AttributeValue medium = AttributeValue.builder()
                .attributeType(sizeType)
                .displayName("M")
                .displayOrder(2)
                .isActive(true)
                .build();
        attributeValueRepository.save(medium);

        AttributeValue large = AttributeValue.builder()
                .attributeType(sizeType)
                .displayName("L")
                .displayOrder(3)
                .isActive(true)
                .build();
        attributeValueRepository.save(large);

        // 온도 속성 값
        AttributeValue hot = AttributeValue.builder()
                .attributeType(temperatureType)
                .displayName("핫")
                .displayOrder(1)
                .isActive(true)
                .build();
        attributeValueRepository.save(hot);

        AttributeValue ice = AttributeValue.builder()
                .attributeType(temperatureType)
                .displayName("아이스")
                .displayOrder(2)
                .isActive(true)
                .build();
        attributeValueRepository.save(ice);

        System.out.println("속성 타입 및 값 생성 완료");
    }

    private void createCategoriesAndProducts() {
        // 이미 데이터가 있으면 생성 스킵
        if (categoryRepository.count() > 0) {
            System.out.println("카테고리가 이미 존재합니다. 더미데이터 생성을 건너뜁니다.");
            return;
        }

        // 1. 카테고리 생성
        Category beverageCategory = Category.builder()
                .name("음료")
                .description("각종 음료 및 주스")
                .build();
        categoryRepository.save(beverageCategory);

        Category dessertCategory = Category.builder()
                .name("디저트")
                .description("케이크, 쿠키 등 디저트")
                .build();
        categoryRepository.save(dessertCategory);

        Category sandwichCategory = Category.builder()
                .name("샌드위치")
                .description("신선한 재료로 만든 샌드위치")
                .build();
        categoryRepository.save(sandwichCategory);

        Category bakeryCategory = Category.builder()
                .name("베이커리")
                .description("빵 및 베이글 등")
                .build();
        categoryRepository.save(bakeryCategory);

        Category saladCategory = Category.builder()
                .name("샐러드")
                .description("신선한 채소와 과일 샐러드")
                .build();
        categoryRepository.save(saladCategory);

        Category snackCategory = Category.builder()
                .name("스낵")
                .description("간편하게 즐기는 스낵류")
                .build();
        categoryRepository.save(snackCategory);

        System.out.println("카테고리 생성 완료");

        // 카테고리에 속성 타입 연결
        connectAttributesToCategories();

        // 2. 상품 생성 (음료 - 대표 상품 3개만)
        createProduct(beverageCategory, "아메리카노", "고소한 원두향이 가득한 아메리카노", 2500L, 3500L, 4500L, "/images/americano.jpg");
        createProduct(beverageCategory, "카페라떼", "부드러운 우유와 에스프레소의 조화", 3000L, 4000L, 5000L, "/images/latte.jpg");
        createProduct(beverageCategory, "카라멜마끼아또", "달콤한 카라멜과 에스프레소의 만남", 3500L, 5000L, 6000L, "/images/caramel_macchiato.jpg");

        // 3. 상품 생성 (디저트 - 대표 상품 3개만)
        createProduct(dessertCategory, "초코케이크", "진한 초콜릿 케이크", 4000L, 5500L, 6500L, "/images/choco_cake.jpg");
        createProduct(dessertCategory, "치즈케이크", "부드러운 뉴욕 스타일 치즈케이크", 4500L, 6000L, 7000L, "/images/cheese_cake.jpg");
        createProduct(dessertCategory, "마카롱 세트", "다양한 맛의 마카롱 5개", 6000L, 8000L, 10000L, "/images/macaron_set.jpg");

        // 4. 상품 생성 (샌드위치 - 대표 상품 2개만)
        createProduct(sandwichCategory, "BLT 샌드위치", "베이컨, 상추, 토마토 샌드위치", 4000L, 5500L, 6500L, "/images/blt_sandwich.jpg");
        createProduct(sandwichCategory, "햄치즈 샌드위치", "햄과 치즈가 듬뿍", 3500L, 5000L, 6000L, "/images/ham_cheese_sandwich.jpg");

        // 5. 상품 생성 (베이커리 - 대표 상품 3개만)
        createProduct(bakeryCategory, "크루아상", "버터가 풍부한 크루아상", 2500L, 3500L, 4500L, "/images/croissant.jpg");
        createProduct(bakeryCategory, "베이글", "쫄깃한 플레인 베이글", 2000L, 3000L, 4000L, "/images/bagel.jpg");
        createProduct(bakeryCategory, "소금빵", "고소한 소금빵", 1800L, 2500L, 3500L, "/images/salt_bread.jpg");

        // 6. 상품 생성 (샐러드 - 대표 상품 2개만)
        createProduct(saladCategory, "시저샐러드", "로메인 상추와 시저 드레싱", 5000L, 7000L, 8000L, "/images/caesar_salad.jpg");
        createProduct(saladCategory, "치킨샐러드", "그릴드 치킨이 들어간 샐러드", 6000L, 8000L, 9000L, "/images/chicken_salad.jpg");

        // 7. 상품 생성 (스낵 - 대표 상품 2개만)
        createProduct(snackCategory, "감자칩", "바삭한 감자칩", 1500L, 2000L, 3000L, "/images/potato_chips.jpg");
        createProduct(snackCategory, "견과류 믹스", "건강한 견과류 세트", 3000L, 4000L, 5000L, "/images/nuts_mix.jpg");

        System.out.println("상품 생성 완료: 총 " + productRepository.count() + "개 (데이터 최소화)");

        // 상품에 속성 값 연결
        connectAttributeValuesToProducts();

        // 8. 지점별 재고 생성 (branchId 1~5 가정)
        createBranchInventory();
        System.out.println("지점별 재고 생성 완료");
    }

    private void createProduct(Category category, String name, String description,
                               Long supplyPrice, Long minPrice, Long maxPrice, String imageName) {
        if (productRepository.existsByName(name)) {
            return;
        }

        // 이미지 URL 구성: Base URL이 설정되어 있으면 전체 URL 생성, 아니면 상대 경로 유지
        String imageUrl = imageName;
        if (imageName != null && !imageName.startsWith("http") &&
            productImageBaseUrl != null && !productImageBaseUrl.isEmpty()) {
            // S3 Base URL이 있으면 전체 URL 생성
            imageUrl = productImageBaseUrl + "/" + imageName.replaceFirst("^/images/", "");
        }

        Product product = Product.builder()
                .category(category)
                .name(name)
                .description(description)
                .supplyPrice(supplyPrice)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .imageUrl(imageUrl)
                .visibility(Visibility.ALL)
                .build();

        Product savedProduct = productRepository.save(product);

        // Kafka 이벤트 발행 (Elasticsearch 동기화)
        if (productEventProducer != null) {
            com.careup.ordering.domain.product.event.ProductEvent event =
                com.careup.ordering.domain.product.event.ProductEvent.builder()
                    .eventType(com.careup.ordering.domain.product.event.ProductEvent.EventType.CREATED)
                    .productId(savedProduct.getId())
                    .name(savedProduct.getName())
                    .description(savedProduct.getDescription())
                    .categoryName(savedProduct.getCategory().getName())
                    .categoryId(savedProduct.getCategory().getId())
                    .price(savedProduct.getMinPrice())
                    .imageUrl(savedProduct.getImageUrl())  // S3 URL 또는 상대 경로
                    .status(savedProduct.getStatus().name())
                    .visibility(savedProduct.getVisibility().name())
                    .build();
            productEventProducer.sendProductEvent(event);
        }
    }

    private void createBranchInventory() {
        // 모든 상품 조회
        var allProducts = productRepository.findAll();

        // 지점 ID 1~5번에 대해 재고 생성
        for (Long branchId = 1L; branchId <= 5L; branchId++) {
            for (Product product : allProducts) {
                // 지점마다 다른 재고 수량 설정 (랜덤하게)
                Long stockQuantity = getRandomStock(branchId, product.getId());
                Long safetyStock = stockQuantity / 3; // 안전재고는 재고의 1/3

                ensureBranchProduct(branchId, product, stockQuantity, safetyStock);
            }
        }
    }

    private Long getRandomStock(Long branchId, Long productId) {
        // 지점과 상품 ID를 조합하여 고정된 재고 수량 생성 (실행마다 동일하도록)
        int seed = (int) (branchId * 1000 + productId);
        int[] stockLevels = {50, 80, 100, 120, 150};
        return (long) stockLevels[seed % stockLevels.length];
    }

    private void ensureBranchProduct(Long branchId, Product product, Long stockQuantity, Long safetyStock) {
        if (branchProductRepository.existsByBranchIdAndProductId(branchId, product.getId())) {
            return;
        }

        BranchProduct branchProduct = BranchProduct.builder()
                .branchId(branchId)
                .product(product)
                .stockQuantity(stockQuantity)
                .safetystock(safetyStock)
                .serialNumber("BR" + String.format("%03d", branchId) + "-PRD" + String.format("%03d", product.getId()))
                .price(product.getSupplyPrice())
                .build();

        branchProductRepository.save(branchProduct);
    }

    private void ensureMember(String email,
                              String rawPassword,
                              String nickname,
                              String name,
                              LocalDate birthday,
                              String phone,
                              Gender gender,
                              String zipcode,
                              String address,
                              String addressDetail) {

        // 이미 이메일이나 닉네임이 존재하면 생성 스킵
        if (memberRepository.existsByEmailIgnoreCase(email) || memberRepository.existsByNickname(nickname)) {
            return;
        }

        Member m = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .nickname(nickname)
                .name(name)
                .birthday(birthday)
                .phone(PhoneUtils.normalize(phone))
                .gender(gender)
                // ▼ NOT NULL 컬럼 채우기
                .zipcode(zipcode)
                .address(address)
                .addressDetail(addressDetail)
                // .isDelYn("N") // 빌더에 없으므로 제거 (엔티티 @PrePersist 등에서 기본값 처리 가정)
                .build();

        memberRepository.save(m);
    }


    /**
     * 카테고리에 속성 타입 연결
     */
    private void connectAttributesToCategories() {
        // 속성 타입 조회
        AttributeType colorType = attributeTypeRepository.findByName("색상")
                .orElseThrow(() -> new RuntimeException("색상 속성 타입을 찾을 수 없습니다."));
        AttributeType sizeType = attributeTypeRepository.findByName("사이즈")
                .orElseThrow(() -> new RuntimeException("사이즈 속성 타입을 찾을 수 없습니다."));
        AttributeType temperatureType = attributeTypeRepository.findByName("온도")
                .orElseThrow(() -> new RuntimeException("온도 속성 타입을 찾을 수 없습니다."));

        // 카테고리 조회
        Category beverageCategory = categoryRepository.findByName("음료");
        if (beverageCategory == null) {
            throw new RuntimeException("음료 카테고리를 찾을 수 없습니다.");
        }
        Category dessertCategory = categoryRepository.findByName("디저트");
        if (dessertCategory == null) {
            throw new RuntimeException("디저트 카테고리를 찾을 수 없습니다.");
        }
        Category bakeryCategory = categoryRepository.findByName("베이커리");
        if (bakeryCategory == null) {
            throw new RuntimeException("베이커리 카테고리를 찾을 수 없습니다.");
        }

        // 음료 카테고리에 온도 속성 추가
        CategoryAttribute beverageTemperature = CategoryAttribute.builder()
                .category(beverageCategory)
                .attributeType(temperatureType)
                .isRequired(false)
                .displayOrder(1)
                .build();
        categoryAttributeRepository.save(beverageTemperature);

        // 디저트 카테고리에 색상 속성 추가
        CategoryAttribute dessertColor = CategoryAttribute.builder()
                .category(dessertCategory)
                .attributeType(colorType)
                .isRequired(false)
                .displayOrder(1)
                .build();
        categoryAttributeRepository.save(dessertColor);

        // 베이커리 카테고리에 사이즈 속성 추가
        CategoryAttribute bakerySize = CategoryAttribute.builder()
                .category(bakeryCategory)
                .attributeType(sizeType)
                .isRequired(false)
                .displayOrder(1)
                .build();
        categoryAttributeRepository.save(bakerySize);

        System.out.println("카테고리-속성 연결 완료");
    }

    /**
     * 상품에 속성 값 연결
     */
    private void connectAttributeValuesToProducts() {
        // 속성 타입 및 값 조회
        AttributeType temperatureType = attributeTypeRepository.findByName("온도")
                .orElseThrow(() -> new RuntimeException("온도 속성 타입을 찾을 수 없습니다."));
        AttributeValue hot = attributeValueRepository.findByAttributeTypeAndDisplayName(temperatureType, "핫")
                .orElseThrow(() -> new RuntimeException("핫 속성 값을 찾을 수 없습니다."));
        AttributeValue ice = attributeValueRepository.findByAttributeTypeAndDisplayName(temperatureType, "아이스")
                .orElseThrow(() -> new RuntimeException("아이스 속성 값을 찾을 수 없습니다."));

        // 음료 카테고리의 상품들에 온도 속성 값 추가
        Category beverageCategory = categoryRepository.findByName("음료");
        if (beverageCategory == null) {
            throw new RuntimeException("음료 카테고리를 찾을 수 없습니다.");
        }
        
        List<Product> beverageProducts = productRepository.findByCategoryId(beverageCategory.getId(), Pageable.unpaged()).getContent();
        
        // 일부 음료에 핫 속성, 일부에 아이스 속성 추가
        for (int i = 0; i < beverageProducts.size(); i++) {
            Product product = beverageProducts.get(i);
            
            // 첫 번째 상품은 핫만, 나머지는 아이스만 추가 (예시)
            if (i == 0) {
                ProductAttributeValue pav = ProductAttributeValue.builder()
                        .product(product)
                        .attributeValue(hot)
                        .customValue(null)
                        .build();
                productAttributeValueRepository.save(pav);
            } else {
                ProductAttributeValue pav = ProductAttributeValue.builder()
                        .product(product)
                        .attributeValue(ice)
                        .customValue(null)
                        .build();
                productAttributeValueRepository.save(pav);
            }
        }

        System.out.println("상품-속성 값 연결 완료");
    }

    /**
     * 주문 데이터 생성 (최근 30일간, 지점별 매출 데이터)
     */
    private void createOrders() {
        // 이미 주문 데이터가 있으면 생성 스킵
        if (orderRepository.count() > 0) {
            System.out.println("주문 데이터가 이미 존재합니다. 생성을 건너뜁니다.");
            return;
        }

        System.out.println("주문 데이터 생성 시작...");

        // 기준 날짜: 현재 시간 (동적으로 설정)
        LocalDateTime today = LocalDateTime.now();

        // 회원 목록 조회
        List<Member> members = memberRepository.findAll();
        if (members.isEmpty()) {
            System.out.println("회원 데이터가 없어 주문을 생성할 수 없습니다.");
            return;
        }

        // 상품 목록 조회
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            System.out.println("상품 데이터가 없어 주문을 생성할 수 없습니다.");
            return;
        }

        // 지점별 주문 생성 (branchId 1~5: 본점, 동작점, 보라매점, 고양삼송점, 을지로점)
        Long[] branchIds = {1L, 2L, 3L, 4L, 5L};

        int totalOrders = 0;
        java.util.Random random = new java.util.Random(20251109); // 고정 시드로 재현 가능하게

        // 각 지점별 매출 편차를 두기 위한 승수 (동작점 > 을지로점 > 보라매점 > 고양삼송점 > 본점)
        double[] branchMultipliers = {0.3, 1.5, 1.0, 0.8, 1.3}; // 본점은 매출이 적음 (관리 위주)

        for (int idx = 0; idx < branchIds.length; idx++) {
            Long branchId = branchIds[idx];
            double multiplier = branchMultipliers[idx];

            String branchNameLog = branchId == 1L ? "본점" :
                                   branchId == 2L ? "동작점" :
                                   branchId == 3L ? "보라매점" :
                                   branchId == 4L ? "고양삼송점" :
                                   branchId == 5L ? "을지로점" : "지점" + branchId;

            System.out.println("=== " + branchNameLog + " (branchId=" + branchId + ") 주문 생성 시작 (매출 비율: " + multiplier + ") ===");
            int branchOrderCount = 0;
            long branchTotalAmount = 0L;

            // 각 지점별로 최근 30일간 주문 생성 (매출 조회 기간 확대)
            for (int day = 0; day < 30; day++) {
                LocalDateTime orderDate = today.minusDays(day);

                // 하루에 1~2건만 생성 (총 주문 약 150건 이내로 제한)
                int baseOrdersPerDay = 1 + random.nextInt(2);
                int ordersPerDay = Math.max(1, (int) (baseOrdersPerDay * multiplier));

                for (int i = 0; i < ordersPerDay; i++) {
                    // 랜덤 회원 선택
                    Member member = members.get(random.nextInt(members.size()));

                    // 하루 중 랜덤한 시간 설정 (09:00 ~ 20:00)
                    int hour = 9 + random.nextInt(12);
                    int minute = random.nextInt(60);
                    LocalDateTime orderDateTime = orderDate.withHour(hour).withMinute(minute);

                    // 주문 생성
                    Order order = Order.builder()
                            .member(member)
                            .branchId(branchId)
                            .totalAmount(0L) // 나중에 계산
                            .orderType(OrderType.ONLINE)
                            .build();

                    Order savedOrder = orderRepository.save(order);

                    // createdAt 수정 (테스트 데이터이므로 리플렉션 사용)
                    try {
                        java.lang.reflect.Field createdAtField = savedOrder.getClass().getSuperclass().getDeclaredField("createdAt");
                        createdAtField.setAccessible(true);
                        createdAtField.set(savedOrder, orderDateTime);
                    } catch (Exception e) {
                        // 리플렉션 실패 시 무시
                    }

                    // 주문 상세 생성 (1~2개 상품만 - 데이터 최소화)
                    int itemCount = 1 + random.nextInt(2);
                    long calculatedTotalAmount = 0L;

                    for (int j = 0; j < itemCount; j++) {
                        // 해당 지점의 BranchProduct 중 랜덤 선택
                        List<BranchProduct> branchProducts = branchProductRepository.findByBranchId(branchId);
                        if (branchProducts.isEmpty()) {
                            System.out.println("경고: 지점 " + branchId + "에 BranchProduct가 없습니다!");
                            continue;
                        }
                        BranchProduct branchProduct = branchProducts.get(random.nextInt(branchProducts.size()));
                        Long quantity = (long) (1 + random.nextInt(3));
                        Long unitPrice = branchProduct.getPrice();

                        OrderedItem item = OrderedItem.builder()
                                .order(savedOrder)
                                .branchProduct(branchProduct)
                                .quantity(quantity)
                                .unitPrice(unitPrice)
                                .build();

                        orderedItemRepository.save(item);

                        // totalPrice는 OrderedItem 빌더에서 자동 계산됨 (quantity * unitPrice)
                        calculatedTotalAmount += (quantity * unitPrice);
                    }

                    // Order의 totalAmount 업데이트
                    savedOrder.updateTotalAmount(calculatedTotalAmount);

                    // 90% 확률로 승인 처리, 10%는 PENDING 상태 유지 (실제 환경 시뮬레이션)
                    boolean shouldApprove = random.nextDouble() < 0.9;
                    if (shouldApprove) {
                        savedOrder.approve(1L); // CONFIRMED 상태로 변경
                    }
                    orderRepository.save(savedOrder);

                    // Kafka 이벤트 발행 (Redis 캐시 업데이트를 위해)
                    if (orderStatisticsProducer != null) {
                        try {
                            OrderStatisticsEvent event = OrderStatisticsEvent.builder()
                                    .orderId(savedOrder.getId())
                                    .branchId(savedOrder.getBranchId())
                                    .totalAmount(savedOrder.getTotalAmount())
                                    .orderStatus(savedOrder.getOrderStatus().name())
                                    .previousStatus(null)
                                    .orderDateTime(orderDateTime)
                                    .eventType("CREATE")
                                    .build();
                            orderStatisticsProducer.sendOrderStatisticsEvent(event);
                        } catch (Exception e) {
                            System.out.println("경고: 주문 통계 이벤트 발행 실패 - orderId=" + savedOrder.getId());
                        }
                    }

                    // 통계 집계 (CONFIRMED 주문만)
                    if (savedOrder.getOrderStatus() == OrderStatus.CONFIRMED) {
                        totalOrders++;
                        branchOrderCount++;
                        branchTotalAmount += calculatedTotalAmount;
                    }

                    // 첫 번째 주문 샘플 로그
                    if (branchOrderCount == 1) {
                        System.out.println("  [샘플 주문] orderId=" + savedOrder.getId() + ", branchId=" + branchId +
                                ", totalAmount=" + savedOrder.getTotalAmount() + ", status=" + savedOrder.getOrderStatus() +
                                ", createdAt=" + orderDateTime);
                    }
                }
            }

            System.out.println("=== " + branchNameLog + " (branchId=" + branchId + ") 주문 생성 완료 === 생성 주문 수: " + branchOrderCount +
                    ", 총 매출액: " + branchTotalAmount + "원");
        }

        System.out.println("주문 데이터 생성 완료: 총 " + totalOrders + "건");
        System.out.println("주문 상세 데이터 생성 완료: 총 " + orderedItemRepository.count() + "건");

        if (orderStatisticsProducer != null) {
            System.out.println("✅ Kafka 이벤트 발행 완료: Redis 캐시가 업데이트됩니다.");
        } else {
            System.out.println("⚠️ 경고: OrderStatisticsProducer가 null입니다. Kafka가 비활성화되어 있을 수 있습니다.");
            System.out.println("   Redis 캐시가 업데이트되지 않아 대시보드에서 0이 표시될 수 있습니다.");
        }
    }

    private void createDummyProductsAndInventory() {
        // 이 메서드는 더 이상 사용하지 않습니다. createCategoriesAndProducts()로 통합되었습니다.
    }
}
