package com.careup.branch.domain.branch.service;

import com.careup.branch.common.file.AwsS3Uploader;
import com.careup.branch.common.util.AuthenticationUtils;
import com.careup.branch.domain.branch.dto.branch.BranchDto;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.entity.BranchUpdateRequest;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.branch.repository.BranchUpdateRequestRepository;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchManagerService {

    private final DispatchStatusRepository dispatchStatusRepository;
    private final BranchRepository branchRepository;
    private final BranchUpdateRequestRepository branchUpdateRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final AwsS3Uploader awsS3Uploader;

    /**
     * 현재 로그인한 관리자(직영점장/가맹점주)의 지점 정보 조회
     */
    public BranchDto getMyBranch() {
        Long employeeId = AuthenticationUtils.getAuthenticatedEmployeeId();
        Branch myBranch = findMyBranchByEmployeeId(employeeId);
        return BranchDto.fromEntity(myBranch);
    }

    /**
     * 지점 정보(지점명, 프로필 사진) 수정 요청
     */
    @Transactional
    public void requestBranchUpdate(String name, MultipartFile profileImageFile) {
        Long employeeId = AuthenticationUtils.getAuthenticatedEmployeeId();
        Branch myBranch = findMyBranchByEmployeeId(employeeId);
        Employee requester = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("요청한 직원 정보를 찾을 수 없습니다. ID: " + employeeId));

        // 이미 처리 대기 중인 요청 있는지 확인
        branchUpdateRequestRepository.findByBranchAndStatus(myBranch, BranchUpdateRequest.RequestStatus.PENDING)
                .ifPresent(req -> {
                    throw new IllegalStateException("이미 처리 대기 중인 수정 요청이 존재합니다.");
                });

        String requestedProfileImageUrl = myBranch.getProfileImageUrl();

        // 새 프로필 이미지 파일이 제공된 경우 S3에 없로드하고 URL을 갱신
        if (profileImageFile != null && !profileImageFile.isEmpty()) {
            requestedProfileImageUrl = awsS3Uploader.uploadFile("branch", myBranch.getId(), profileImageFile);
        }

        // 수정 요청 엔티티 생성 및 저장
        BranchUpdateRequest updateRequest = BranchUpdateRequest.builder()
                .branch(myBranch)
                .requester(requester)
                .requestedName(name)
                .requestedProfileImageUrl(requestedProfileImageUrl)
                .status(BranchUpdateRequest.RequestStatus.PENDING)
                .build();

        branchUpdateRequestRepository.save(updateRequest);
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
