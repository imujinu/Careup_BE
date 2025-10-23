package com.careup.branch.domain.chat.dto.req;

import com.careup.branch.domain.chat.dto.res.ChatDailySalesDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ChatDailyPaymentsDto {
    private Long totalPrice;
    private List<ChatDailySalesDto.ProductLIst> productList;
}
