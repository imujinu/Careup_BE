package com.careup.branch.domain.branch.service;

import com.careup.branch.common.file.AwsS3Uploader;
import com.careup.branch.common.util.AuthenticationUtils;
import com.careup.branch.domain.branch.dto.branch.BranchDto;
import com.careup.branch.domain.branch.dto.branch.BranchUpdateRequestDto;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.entity.BranchUpdateRequest;
import com.careup.branch.domain.branch.repository.BranchUpdateRequestRepository;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.notification.dto.SseNotificationResDto;
import com.careup.branch.domain.notification.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BranchManagerService {

    private final DispatchStatusRepository dispatchStatusRepository;
    private final BranchUpdateRequestRepository branchUpdateRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final AwsS3Uploader awsS3Uploader;
    private final NotificationService notificationService;

    /**
     * 현재 로그인한 관리자(직영점장/가맹점주)의 지점 정보 조회
     */
    public BranchDto getMyBranch() {
        Long employeeId = AuthenticationUtils.getAuthenticatedEmployeeId();
        Branch myBranch = findMyBranchByEmployeeId(employeeId);
        return BranchDto.fromEntity(myBranch);
    }

    /**
     * 지점 정보 전체 수정 요청
     */
    @Transactional
    public void requestBranchUpdate(BranchUpdateRequestDto requestDto, MultipartFile profileImageFile) {
        Long employeeId = AuthenticationUtils.getAuthenticatedEmployeeId();
        Branch myBranch = findMyBranchByEmployeeId(employeeId);
        Employee requester = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("요청한 직원 정보를 찾을 수 없습니다. ID: " + employeeId));

        // 이미 처리 대기 중인 요청 있는지 확인
        branchUpdateRequestRepository.findByBranchAndStatus(myBranch, BranchUpdateRequest.RequestStatus.PENDING)
                .ifPresent(req -> {
                    throw new IllegalStateException("이미 처리 대기 중인 수정 요청이 존재합니다.");
                });

        // 현재 지점 정보를 기반으로 수정 요청 생성 (팩토리 메서드 활용)
        BranchUpdateRequest updateRequest = BranchUpdateRequest.from(myBranch, requester);

        // DTO에서 받은 변경사항 적용
        updateRequest.updateRequestedData(
                requestDto.getName(),
                requestDto.getBusinessDomain(),
                requestDto.getOwnershipType(),
                requestDto.getOpenDate(),
                requestDto.getBusinessNumber(),
                requestDto.getCorporationNumber(),
                requestDto.getZipcode(),
                requestDto.getAddress(),
                requestDto.getAddressDetail(),
                requestDto.getPhone(),
                requestDto.getEmail(),
                requestDto.getLatitude(),
                requestDto.getLongitude(),
                requestDto.getGeofenceRadius(),
                requestDto.getRemark(),
                requestDto.getAttorneyName(),
                requestDto.getAttorneyPhoneNumber()
        );

        // 프로필 이미지가 업로드된 경우에만 처리
        if (profileImageFile != null && !profileImageFile.isEmpty()) {
            String uploadedImageUrl = awsS3Uploader.uploadFile("branch", myBranch.getId(), profileImageFile);
            updateRequest.updateProfileImageUrl(uploadedImageUrl);
        }

        BranchUpdateRequest savedRequest = branchUpdateRequestRepository.save(updateRequest);

        // 본사 관리자(HQ_ADMIN)들에게 알림 발송
        List<String> hqAdminEmails = employeeRepository.findAllHqAdminEmails();
        if (!hqAdminEmails.isEmpty()) {
            SseNotificationResDto notificationDto = SseNotificationResDto.branchUpdateRequested(
                    myBranch.getName(),
                    savedRequest.getId(),
                    requester.getName()
            );
            notificationService.sendNotificationToEmails(hqAdminEmails, notificationDto);
            log.info("[CAREUP][INFO] - BranchManagerService/requestBranchUpdate - 본사 관리자 {}명에게 지점 수정 요청 알림 발송 완료", hqAdminEmails.size());
        }
    }

    /**
     * 직원ID로 현재 관리중인 지점 엔티티를 찾는 메서드
     */
    private Branch findMyBranchByEmployeeId(Long employeeId) {
        DispatchStatus activeDispatch = dispatchStatusRepository.findActiveDispatchByEmployeeId(employeeId, LocalDate.now())
                .orElseThrow(() -> new EntityNotFoundException("현재 관리하고 있는 지점 정보가 없습니다."));

        return Optional.ofNullable(activeDispatch.getBranch())
                .orElseThrow(() -> new EntityNotFoundException("배치된 지점 정보를 찾을 수 없습니다."));
    }
}
