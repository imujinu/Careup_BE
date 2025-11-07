package com.careup.branch.domain.chat.dto.stock;

import com.careup.branch.common.client.OrderingInventoryClient;
import com.careup.branch.domain.chat.dto.BaseResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class StockResponseDto extends BaseResponseDto {

    private List<OrderingInventoryClient.BranchProductResponseDto> products;

    public StockResponseDto makeDto(String intent, String action, List<OrderingInventoryClient.BranchProductResponseDto> products){
        return new StockResponseDto().builder()
                .intent(intent)
                .action(action)
                .products(products)
                .build();
    }
}
