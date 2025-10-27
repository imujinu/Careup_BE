package com.careup.branch.domain.branch.service;

import com.careup.branch.common.file.AwsS3Uploader;
import com.careup.branch.common.util.AuthenticationUtils;
import com.careup.branch.domain.branch.dto.branch.BranchUpdateRequestDto;
import com.careup.branch.domain.branch.dto.branch.BranchUpdateRequestListResDto;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.entity.BranchUpdateRequest;
import com.careup.branch.domain.branch.repository.BranchUpdateRequestRepository;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BranchUpdateRequestService {

    private final BranchUpdateRequestRepository branchUpdateRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final AwsS3Uploader awsS3Uploader;

    /**
     * 모든 지점 수정 요청 목록 조회 (페이징)
     */
    public BranchUpdateRequestListResDto getAllRequests(Pageable pageable) {
        Page<BranchUpdateRequest> requests = branchUpdateRequestRepository.findAllByOrderByCreatedAtDesc(pageable);
        Page<BranchUpdateRequestDto> dtoPage = requests.map(BranchUpdateRequestDto::fromEntity);
        
        log.info("지점 수정 요청 목록 조회 - 페이지: {}, 총 요청 수: {}", pageable.getPageNumber() + 1, dtoPage.getTotalElements());
        
        return BranchUpdateRequestListResDto.fromPage(dtoPage);
    }

    /**
     * 특정 상태의 지점 수정 요청 목록 조회 (페이징)
     */
    public BranchUpdateRequestListResDto getRequestsByStatus(BranchUpdateRequest.RequestStatus status, Pageable pageable) {
        Page<BranchUpdateRequest> requests = branchUpdateRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        Page<BranchUpdateRequestDto> dtoPage = requests.map(BranchUpdateRequestDto::fromEntity);
        
        log.info("지점 수정 요청 목록 조회 (상태: {}) - 페이지: {}, 총 요청 수: {}", status, pageable.getPageNumber() + 1, dtoPage.getTotalElements());
        
        return BranchUpdateRequestListResDto.fromPage(dtoPage);
    }

    /**
     * 지점 수정 요청 상세 조회
     */
    public BranchUpdateRequestDto getRequest(Long requestId) {
        BranchUpdateRequest request = branchUpdateRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 수정 요청입니다. ID: " + requestId));
        
        log.info("지점 수정 요청 상세 조회: {}", requestId);
        
        return BranchUpdateRequestDto.fromEntity(request);
    }

    /**
     * 지점 수정 요청 승인 (본사 관리자)
     */
    @Transactional
    public void approveRequest(Long requestId) {
        Long employeeId = AuthenticationUtils.getAuthenticatedEmployeeId();
        Employee approver = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("승인자 정보를 찾을 수 없습니다. ID: " + employeeId));

        BranchUpdateRequest request = branchUpdateRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 수정 요청입니다. ID: " + requestId));

        if (request.getStatus() != BranchUpdateRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 요청입니다. 현재 상태: " + request.getStatus());
        }

        Branch branch = request.getBranch();
        
        log.info("지점 수정 요청 승인 처리 전 - 지점: {}, 요청ID: {}", branch.getName(), requestId);
        log.info("변경 전 지점명: {}, 변경 후 지점명: {}", branch.getName(), request.getRequestedName());
        log.info("변경 전 이미지 URL: {}, 변경 후 이미지 URL: {}", branch.getProfileImageUrl(), request.getRequestedProfileImageUrl());

        // 기존 이미지가 있고, 새 이미지가 기존 이미지와 다른 경우 기존 이미지 삭제
        if (branch.getProfileImageUrl() != null && 
            !branch.getProfileImageUrl().isEmpty() &&
            request.getRequestedProfileImageUrl() != null &&
            !branch.getProfileImageUrl().equals(request.getRequestedProfileImageUrl())) {
            try {
                awsS3Uploader.deleteByUrl(branch.getProfileImageUrl());
                log.info("기존 지점 프로필 이미지 삭제 완료: {}", branch.getProfileImageUrl());
            } catch (Exception e) {
                log.warn("기존 이미지 삭제 실패 (계속 진행): {}", e.getMessage());
            }
        }

        // 지점 정보 업데이트
        branch.changeName(request.getRequestedName());
        branch.changeProfileImageUrl(request.getRequestedProfileImageUrl());

        // 요청 상태를 승인으로 변경
        request.approve(approver);

        log.info("지점 수정 요청 승인 완료 - 지점ID: {}, 요청ID: {}, 승인자: {}", branch.getId(), requestId, approver.getName());
    }

    /**
     * 지점 수정 요청 거부 (본사 관리자)
     */
    @Transactional
    public void rejectRequest(Long requestId) {
        Long employeeId = AuthenticationUtils.getAuthenticatedEmployeeId();
        Employee approver = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("승인자 정보를 찾을 수 없습니다. ID: " + employeeId));

        BranchUpdateRequest request = branchUpdateRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 수정 요청입니다. ID: " + requestId));

        if (request.getStatus() != BranchUpdateRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 요청입니다. 현재 상태: " + request.getStatus());
        }

        log.info("지점 수정 요청 거부 처리 - 요청ID: {}, 거부자: {}", requestId, approver.getName());

        // 요청 시 업로드된 이미지가 있고, 현재 지점의 이미지와 다른 경우 삭제
        if (request.getRequestedProfileImageUrl() != null && 
            !request.getRequestedProfileImageUrl().isEmpty() &&
            !request.getRequestedProfileImageUrl().equals(request.getBranch().getProfileImageUrl())) {
            try {
                awsS3Uploader.deleteByUrl(request.getRequestedProfileImageUrl());
                log.info("요청 시 업로드된 이미지 삭제 완료: {}", request.getRequestedProfileImageUrl());
            } catch (Exception e) {
                log.warn("요청 이미지 삭제 실패 (계속 진행): {}", e.getMessage());
            }
        }

        // 요청 상태를 거부로 변경
        request.reject(approver);

        log.info("지점 수정 요청 거부 완료 - 요청ID: {}, 거부자: {}", requestId, approver.getName());
    }
}

