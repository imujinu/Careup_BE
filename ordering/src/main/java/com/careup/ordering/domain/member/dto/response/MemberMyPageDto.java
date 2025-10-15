package com.careup.ordering.domain.member.dto.response;

import com.careup.ordering.domain.member.entity.Gender;
import com.careup.ordering.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberMyPageDto {
    private Long memberId;
    private String email;
    private String nickname;
    private String name;
    private String phone;
    private Gender gender;

    public static MemberMyPageDto from(Member m) {
        return MemberMyPageDto.builder()
                .memberId(m.getId())
                .email(m.getEmail())
                .nickname(m.getNickname())
                .name(m.getName())
                .phone(m.getPhone())
                .gender(m.getGender())
                .build();
    }
}
