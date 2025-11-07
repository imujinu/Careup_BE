package com.careup.branch.domain.chat.dto.purchase;

import com.careup.branch.domain.chat.dto.BaseResponseDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderListResponseDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class PurchaseResDto extends BaseResponseDto {
    List<PurchaseOrderListResponseDto> purchaseList;

    public PurchaseResDto makeDto( String intent, String action, List<PurchaseOrderListResponseDto> purchaseList){
        return  PurchaseResDto.builder()
                .intent(intent)
                .action(action)
                .purchaseList(purchaseList)
                .build();
    }
}
