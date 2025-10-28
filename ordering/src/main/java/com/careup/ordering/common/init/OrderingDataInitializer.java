package com.careup.ordering.common.init;

import com.careup.ordering.common.util.PhoneUtils;
import com.careup.ordering.domain.member.entity.Gender;
import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.member.repository.MemberRepository;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.entity.BranchProduct;
import com.careup.ordering.domain.product.entity.Category;
import com.careup.ordering.domain.product.entity.Visibility;
import com.careup.ordering.domain.product.repository.ProductRepository;
import com.careup.ordering.domain.product.repository.BranchProductRepository;
import com.careup.ordering.domain.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class OrderingDataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductRepository productRepository;
    private final BranchProductRepository branchProductRepository;
    private final CategoryRepository categoryRepository;

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

        // 카테고리 및 상품/재고 생성
        createCategoriesAndProducts();
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

        // 2. 상품 생성 (음료)
        createProduct(beverageCategory, "아메리카노", "고소한 원두향이 가득한 아메리카노", 2500L, 3500L, 4500L, "/images/americano.jpg");
        createProduct(beverageCategory, "카페라떼", "부드러운 우유와 에스프레소의 조화", 3000L, 4000L, 5000L, "/images/latte.jpg");
        createProduct(beverageCategory, "카푸치노", "우유 거품이 풍성한 카푸치노", 3000L, 4000L, 5000L, "/images/cappuccino.jpg");
        createProduct(beverageCategory, "바닐라라떼", "달콤한 바닐라 시럽이 들어간 라떼", 3300L, 4500L, 5500L, "/images/vanilla_latte.jpg");
        createProduct(beverageCategory, "카라멜마끼아또", "달콤한 카라멜과 에스프레소의 만남", 3500L, 5000L, 6000L, "/images/caramel_macchiato.jpg");
        createProduct(beverageCategory, "그린티라떼", "고급 녹차와 우유의 조화", 3300L, 4500L, 5500L, "/images/greentea_latte.jpg");
        createProduct(beverageCategory, "딸기라떼", "상큼한 딸기와 우유", 3500L, 5000L, 6000L, "/images/strawberry_latte.jpg");
        createProduct(beverageCategory, "레몬에이드", "상큼한 레몬 음료", 3000L, 4000L, 5000L, "/images/lemonade.jpg");
        createProduct(beverageCategory, "자몽에이드", "상큼한 자몽 음료", 3300L, 4500L, 5500L, "/images/grapefruit_ade.jpg");
        createProduct(beverageCategory, "오렌지주스", "100% 생 오렌지 주스", 3300L, 4500L, 5500L, "/images/orange_juice.jpg");

        // 3. 상품 생성 (디저트)
        createProduct(dessertCategory, "초코케이크", "진한 초콜릿 케이크", 4000L, 5500L, 6500L, "/images/choco_cake.jpg");
        createProduct(dessertCategory, "치즈케이크", "부드러운 뉴욕 스타일 치즈케이크", 4500L, 6000L, 7000L, "/images/cheese_cake.jpg");
        createProduct(dessertCategory, "티라미수", "이탈리아 정통 티라미수", 5000L, 6500L, 7500L, "/images/tiramisu.jpg");
        createProduct(dessertCategory, "마카롱 세트", "다양한 맛의 마카롱 5개", 6000L, 8000L, 10000L, "/images/macaron_set.jpg");
        createProduct(dessertCategory, "쿠키 세트", "홈메이드 쿠키 6개", 3500L, 5000L, 6000L, "/images/cookie_set.jpg");
        createProduct(dessertCategory, "와플", "바삭한 벨기에 와플", 4000L, 5500L, 6500L, "/images/waffle.jpg");
        createProduct(dessertCategory, "브라우니", "진한 초콜릿 브라우니", 3300L, 4500L, 5500L, "/images/brownie.jpg");
        createProduct(dessertCategory, "휘낭시에", "프랑스 전통 휘낭시에 3개", 4500L, 6000L, 7000L, "/images/financier.jpg");

        // 4. 상품 생성 (샌드위치)
        createProduct(sandwichCategory, "BLT 샌드위치", "베이컨, 상추, 토마토 샌드위치", 4000L, 5500L, 6500L, "/images/blt_sandwich.jpg");
        createProduct(sandwichCategory, "햄치즈 샌드위치", "햄과 치즈가 듬뿍", 3500L, 5000L, 6000L, "/images/ham_cheese_sandwich.jpg");
        createProduct(sandwichCategory, "에그마요 샌드위치", "부드러운 에그마요", 3300L, 4500L, 5500L, "/images/egg_mayo_sandwich.jpg");
        createProduct(sandwichCategory, "치킨샐러드 샌드위치", "신선한 치킨 샐러드", 4500L, 6000L, 7000L, "/images/chicken_salad_sandwich.jpg");
        createProduct(sandwichCategory, "참치샐러드 샌드위치", "참치와 채소의 조화", 4000L, 5500L, 6500L, "/images/tuna_salad_sandwich.jpg");

        // 5. 상품 생성 (베이커리)
        createProduct(bakeryCategory, "크루아상", "버터가 풍부한 크루아상", 2500L, 3500L, 4500L, "/images/croissant.jpg");
        createProduct(bakeryCategory, "베이글", "쫄깃한 플레인 베이글", 2000L, 3000L, 4000L, "/images/bagel.jpg");
        createProduct(bakeryCategory, "소금빵", "고소한 소금빵", 1800L, 2500L, 3500L, "/images/salt_bread.jpg");
        createProduct(bakeryCategory, "단팥빵", "달콤한 팥이 가득", 2000L, 3000L, 4000L, "/images/red_bean_bread.jpg");
        createProduct(bakeryCategory, "크림빵", "부드러운 크림이 들어간 빵", 2000L, 3000L, 4000L, "/images/cream_bread.jpg");
        createProduct(bakeryCategory, "식빵", "부드러운 식빵 한 덩이", 3000L, 4000L, 5000L, "/images/white_bread.jpg");
        createProduct(bakeryCategory, "바게트", "프랑스 전통 바게트", 3300L, 4500L, 5500L, "/images/baguette.jpg");

        // 6. 상품 생성 (샐러드)
        createProduct(saladCategory, "시저샐러드", "로메인 상추와 시저 드레싱", 5000L, 7000L, 8000L, "/images/caesar_salad.jpg");
        createProduct(saladCategory, "그릭샐러드", "페타치즈와 올리브가 들어간 그릭 샐러드", 5500L, 7500L, 8500L, "/images/greek_salad.jpg");
        createProduct(saladCategory, "치킨샐러드", "그릴드 치킨이 들어간 샐러드", 6000L, 8000L, 9000L, "/images/chicken_salad.jpg");
        createProduct(saladCategory, "과일샐러드", "신선한 계절 과일 샐러드", 4800L, 6500L, 7500L, "/images/fruit_salad.jpg");

        // 7. 상품 생성 (스낵)
        createProduct(snackCategory, "감자칩", "바삭한 감자칩", 1500L, 2000L, 3000L, "/images/potato_chips.jpg");
        createProduct(snackCategory, "팝콘", "버터향 팝콘", 1800L, 2500L, 3500L, "/images/popcorn.jpg");
        createProduct(snackCategory, "견과류 믹스", "건강한 견과류 세트", 3000L, 4000L, 5000L, "/images/nuts_mix.jpg");
        createProduct(snackCategory, "치즈스틱", "바삭한 치즈스틱", 2000L, 3000L, 4000L, "/images/cheese_stick.jpg");
        createProduct(snackCategory, "프레즐", "고소한 프레즐", 1800L, 2500L, 3500L, "/images/pretzel.jpg");
        createProduct(snackCategory, "초코바", "달콤한 초코바", 1500L, 2000L, 3000L, "/images/choco_bar.jpg");

        System.out.println("상품 생성 완료: 총 " + productRepository.count() + "개");

        // 8. 지점별 재고 생성 (branchId 1~5 가정)
        createBranchInventory();
        System.out.println("지점별 재고 생성 완료");
    }

    private void createProduct(Category category, String name, String description, 
                               Long supplyPrice, Long minPrice, Long maxPrice, String imageUrl) {
        if (productRepository.existsByName(name)) {
            return;
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

        productRepository.save(product);
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


    private void createDummyProductsAndInventory() {
        // 이 메서드는 더 이상 사용하지 않습니다. createCategoriesAndProducts()로 통합되었습니다.
    }
}
