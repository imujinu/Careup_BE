package com.careup.ordering.domain.recomendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderedItemEvent {
    @JsonProperty("ordered_item_id")
    private Long orderedItemId;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("total_price")
    private Integer totalPrice;

    @JsonProperty("unit_price")
    private Integer unitPrice;

    @JsonProperty("branch_product_id")
    private Long branchProductId;

    @JsonProperty("order_id")
    private Long orderId;
}
