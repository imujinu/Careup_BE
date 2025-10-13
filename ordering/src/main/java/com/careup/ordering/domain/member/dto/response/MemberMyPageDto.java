package com.careup.ordering.domain.member.dto.response;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberMyPageDto {
    private Long memberId;
    private String email;
    private String nickname;
    private String name;
    private String phone;
    private String gender;
}
