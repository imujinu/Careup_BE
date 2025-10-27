package com.careup.branch.domain.chat.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatStockDto {
    private String name;
    private Long quantity;
    private Long safetyQuantity;
    private Boolean orderYn;
}
