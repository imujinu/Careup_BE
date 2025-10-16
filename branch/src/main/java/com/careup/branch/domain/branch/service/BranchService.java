package com.careup.branch.domain.branch.service;

import com.careup.branch.common.file.AwsS3Uploader;
import com.careup.branch.domain.branch.dto.branch.BranchDto;
import com.careup.branch.domain.branch.dto.branch.BranchListResDto;
import com.careup.branch.domain.branch.dto.branch.BranchRegisterReqDto;
import com.careup.branch.domain.branch.dto.branch.BranchUpdateDto;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.repository.BranchRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BranchService {

    private final BranchRepository branchRepository;
    private final AwsS3Uploader awsS3Uploader;

    // 지점 등록
    public Branch registerBranch(BranchRegisterReqDto dto, MultipartFile profileImage) {
        if (branchRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("이미 등록된 지점명입니다.");
        }
        if (branchRepository.existsByBusinessNumber(dto.getBusinessNumber())) {
            throw new IllegalArgumentException("이미 등록된 사업자등록번호입니다.");
        }
        if (branchRepository.existsByPhone(dto.getPhone())) {
            throw new IllegalArgumentException("이미 등록된 전화번호입니다.");
        }
        if (branchRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }

        Branch branch = dto.toEntity();

        // 임시 저장하여 ID 생성
        Branch savedBranch = branchRepository.save(branch);

        // 프로필 이미지 업로드
        if (profileImage != null && !profileImage.isEmpty()) {
            String imageUrl = awsS3Uploader.uploadFile("branch", savedBranch.getId(), profileImage);
            savedBranch.changeProfileImageUrl(imageUrl);
            log.info("지점 프로필 이미지 업로드 완료: {}", imageUrl);
        }

        log.info("지점 등록 정보: {}", savedBranch);

        return savedBranch;
    }

    // 지점 상세 조회
    @Transactional(readOnly = true)
    public BranchDto getBranch(Long branchId) {
        Branch findBranch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지점입니다."));

        log.info("지점 상세 조회: {}", findBranch);

        return BranchDto.fromEntity(findBranch);
    }

    // 지점 목록 조회 (페이징)
    @Transactional(readOnly = true)
    public BranchListResDto getBranchList(Pageable pageable) {
        Page<Branch> findBranches = branchRepository.findAll(pageable);

        // Entity를 DTO로 변환
        Page<BranchDto> branchDtoPage = findBranches.map(BranchDto::fromEntity);

        log.info("지점 목록 조회 - 페이지: {}", pageable.getPageNumber() + 1);
        log.info("지점 목록 조회 - 총 페이지: {}", branchDtoPage.getTotalPages());
        log.info("지점 목록 조회 - 총 데이터 갯수: {}", branchDtoPage.getTotalElements());
        log.info("지점 목록 조회 - 현재 페이지 데이터 정보: {}", branchDtoPage.getContent());

        return BranchListResDto.fromPage(branchDtoPage);
    }

    // 지점 수정
    public Branch updateBranch(Long branchId, @Valid BranchUpdateDto request, MultipartFile profileImage) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지점입니다."));

        // 이름 중복 체크
        if (!branch.getName().equals(request.getName()) && branchRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("이미 등록된 지점명입니다.");
        }
        // 사업자등록번호 중복 체크
        if (!branch.getBusinessNumber().equals(request.getBusinessNumber()) && branchRepository.existsByBusinessNumber(request.getBusinessNumber())) {
            throw new IllegalArgumentException("이미 등록된 사업자등록번호입니다.");
        }
        // 전화번호 중복 체크
        if (!branch.getPhone().equals(request.getPhone()) && branchRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("이미 등록된 전화번호입니다.");
        }
        if (!branch.getEmail().equalsIgnoreCase(request.getEmail()) &&
                branchRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }

        log.info("지점 변경 전: {}", branch);

        // 프로필 이미지 업데이트
        if (profileImage != null && !profileImage.isEmpty()) {
            // 기존 이미지 삭제
            if (branch.getProfileImageUrl() != null && !branch.getProfileImageUrl().isEmpty()) {
                try {
                    awsS3Uploader.deleteByUrl(branch.getProfileImageUrl());
                    log.info("기존 지점 프로필 이미지 삭제 완료: {}", branch.getProfileImageUrl());
                } catch (Exception e) {
                    log.warn("기존 이미지 삭제 실패 (계속 진행): {}", e.getMessage());
                }
            }

            // 새 이미지 업로드
            String newImageUrl = awsS3Uploader.uploadFile("branch", branchId, profileImage);
            branch.changeProfileImageUrl(newImageUrl);
            log.info("새 지점 프로필 이미지 업로드 완료: {}", newImageUrl);
        }

        branch.updateBranch(request);
        branchRepository.save(branch);

        log.info("지점 변경 후: {}", branch);

        return branch;
    }

    // 지점 삭제
    public void deleteBranch(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지점입니다."));

        log.info("지점 삭제: {}", branch);

        // 프로필 이미지 삭제
        if (branch.getProfileImageUrl() != null && !branch.getProfileImageUrl().isEmpty()) {
            try {
                awsS3Uploader.deleteByUrl(branch.getProfileImageUrl());
                log.info("지점 프로필 이미지 삭제 완료: {}", branch.getProfileImageUrl());
            } catch (Exception e) {
                log.warn("프로필 이미지 삭제 실패 (계속 진행): {}", e.getMessage());
            }
        }

        branchRepository.delete(branch);
    }


}
