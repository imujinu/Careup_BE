package com.careup.branch.domain.branch.dto.branch;

import com.careup.branch.domain.branch.entity.BranchUpdateRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchUpdateRequestDto {

    private Long id;
    private Long branchId;
    private String branchName; // 현재 지점명
    private Long requesterId;
    private String requesterName; // 요청자 이름
    private String requestedName; // 요청된 지점명
    private String requestedProfileImageUrl; // 요청된 프로필 이미지 URL
    private BranchUpdateRequest.RequestStatus status;
    private LocalDateTime createdAt;

    public static BranchUpdateRequestDto fromEntity(BranchUpdateRequest request) {
        return BranchUpdateRequestDto.builder()
                .id(request.getId())
                .branchId(request.getBranch().getId())
                .branchName(request.getBranch().getName())
                .requesterId(request.getRequester().getId())
                .requesterName(request.getRequester().getName())
                .requestedName(request.getRequestedName())
                .requestedProfileImageUrl(request.getRequestedProfileImageUrl())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .build();
    }
}

