package com.careup.ordering.domain.member.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FindCustomerIdResponse {
    private String email;
    private String phone;
}
