package com.careup.ordering.domain.member.dto.response;

import com.careup.ordering.domain.member.entity.Gender;
import com.careup.ordering.domain.member.entity.Member;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDetailDto {
    private Long memberId;
    private String email;
    private String nickname;
    private String name;
    private String phone;
    private Gender gender;
    private LocalDate birthday;
    private String zipcode;
    private String address;
    private String addressDetail;
    private String isDelYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MemberDetailDto from(Member m) {
        return MemberDetailDto.builder()
                .memberId(m.getId())
                .email(m.getEmail())
                .nickname(m.getNickname())
                .name(m.getName())
                .phone(m.getPhone())
                .gender(m.getGender())
                .birthday(m.getBirthday())
                .zipcode(m.getZipcode())
                .address(m.getAddress())
                .addressDetail(m.getAddressDetail())
                .isDelYn(m.getIsDelYn())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}
