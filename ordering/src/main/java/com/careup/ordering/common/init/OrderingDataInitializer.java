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

        // 상품 및 재고 더미 데이터 생성
        createDummyProductsAndInventory();
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
        // 카테고리 생성
        Category beverageCategory = ensureCategory("음료", "음료 카테고리");
        Category dessertCategory  = ensureCategory("디저트", "디저트 카테고리");
        Category breadCategory    = ensureCategory("빵", "빵 카테고리");

        Product americano = ensureProduct(
                beverageCategory,
                "아메리카노",
                "시그니처 아메리카노",
                3000L,  // supplyPrice
                4000L,  // minPrice
                5000L,  // maxPrice
                "https://example.com/americano.jpg",
                "ALL"
        );

        Product cookie = ensureProduct(
                dessertCategory,
                "초콜릿 쿠키",
                "달콤한 초콜릿 쿠키",
                2000L,
                3000L,
                3500L,
                "https://example.com/cookie.jpg",
                "ALL"
        );

        Product croissant = ensureProduct(
                breadCategory,
                "크로와상",
                "바삭한 크로와상",
                2500L,
                3500L,
                4000L,
                "https://example.com/croissant.jpg",
                "ALL"
        );

        // 본사 재고만 생성 (가맹점은 빈 상태로 시작)
        ensureBranchProduct(1L, americano.getId(), 100L, 50L);
        ensureBranchProduct(1L, cookie.getId(),     80L,  30L);
        ensureBranchProduct(1L, croissant.getId(),  60L,  25L);
    }

    private Category ensureCategory(String name, String description) {
        Category existingCategory = categoryRepository.findByName(name);
        if (existingCategory != null) return existingCategory;

        Category category = Category.builder()
                .name(name)
                .description(description)
                .build();
        return categoryRepository.save(category);
    }

    private Product ensureProduct(Category category, String name, String description,
                                  Long supplyPrice, Long minPrice, Long maxPrice, String imageUrl,
                                  String visibility) {
        if (productRepository.existsByName(name)) {
            return productRepository.findFirstByName(name).orElse(null);
        }

        Product product = Product.builder()
                .category(category)
                .name(name)
                .description(description)
                .supplyPrice(supplyPrice)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .imageUrl(imageUrl)
                .visibility(Visibility.valueOf(visibility))
                .build();

        return productRepository.save(product);
    }

    private void ensureBranchProduct(Long branchId, Long productId, Long stockQuantity, Long safetyStock) {
        if (branchProductRepository.existsByBranchIdAndProductId(branchId, productId)) return;

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return;

        BranchProduct branchProduct = BranchProduct.builder()
                .branchId(branchId)
                .product(product)
                .stockQuantity(stockQuantity)
                .safetystock(safetyStock) // 엔티티 빌더명이 이렇다면 그대로 사용
                .serialNumber("SN-" + branchId + "-" + productId)
                .price(product.getSupplyPrice())
                .build();

        branchProductRepository.save(branchProduct);
    }
}
