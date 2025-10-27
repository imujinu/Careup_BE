package com.careup.branch.domain.chat.dto.req;

import com.careup.branch.domain.chat.entity.ChatBotStock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatBotReqDto {
    private Long branchId;
    private String message;
}
