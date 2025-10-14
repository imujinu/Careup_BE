package com.careup.ordering.domain.member.dto.response;

import com.careup.ordering.domain.member.entity.Gender;
import com.careup.ordering.domain.member.entity.Member;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberListDto {
    private Long memberId;
    private String email;
    private String nickname;
    private String name;
    private String phone;
    private Gender gender;
    private LocalDate birthday;
    private String isDelYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MemberListDto from(Member m) {
        return MemberListDto.builder()
                .memberId(m.getId())
                .email(m.getEmail())
                .nickname(m.getNickname())
                .name(m.getName())
                .phone(m.getPhone())
                .gender(m.getGender())
                .birthday(m.getBirthday())
                .isDelYn(m.getIsDelYn())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}
