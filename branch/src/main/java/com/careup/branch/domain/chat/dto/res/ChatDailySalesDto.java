package com.careup.branch.domain.chat.dto.res;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.chat.dto.req.ChatDailyPaymentsDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class ChatDailySalesDto {
    private Long totalPrice;
    private LocalDateTime openingTime;
    private LocalDateTime currentTime;
    private List<ProductLIst> productLIstList;

    public static class ProductLIst{
        private String name;
        private Long quantity;
        private Long price;
    }

    public ChatDailySalesDto makeDto(LocalDateTime openingTime, LocalDateTime currentTime, ChatDailyPaymentsDto dto){
        return ChatDailySalesDto.builder()
                .totalPrice(dto.getTotalPrice())
                .openingTime(openingTime)
                .currentTime(currentTime)
                .productLIstList(dto.getProductList())
                .build();
    }
}
