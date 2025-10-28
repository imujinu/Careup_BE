package com.careup.ordering.common.init;

import com.careup.ordering.common.util.PhoneUtils;
import com.careup.ordering.domain.member.entity.Gender;
import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.member.repository.MemberRepository;
import com.careup.ordering.domain.product.entity.Category;
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

        // 카테고리 생성
        createCategoriesOnly();
    }

    private void createCategoriesOnly() {
        // 카테고리가 없으면 생성
        if (categoryRepository.count() == 0) {
            Category category1 = Category.builder()
                    .name("음료")
                    .description("음료 카테고리")
                    .build();

            Category category2 = Category.builder()
                    .name("디저트")
                    .description("디저트 카테고리")
                    .build();

            Category category3 = Category.builder()
                    .name("식품")
                    .description("식품 카테고리")
                    .build();

            categoryRepository.save(category1);
            categoryRepository.save(category2);
            categoryRepository.save(category3);

            System.out.println("카테고리 생성 완료: 음료, 디저트, 식품");
        }
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



}
