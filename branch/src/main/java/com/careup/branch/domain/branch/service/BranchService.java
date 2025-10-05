package com.careup.branch.domain.branch.service;

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

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BranchService {

    private final BranchRepository branchRepository;

    // 지점 등록
    public Branch registerBranch(BranchRegisterReqDto dto) {
        if (branchRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("이미 등록된 지점명입니다.");
        }
        if (branchRepository.existsByBusinessNumber(dto.getBusinessNumber())) {
            throw new IllegalArgumentException("이미 등록된 사업자등록번호입니다.");
        }
        if (branchRepository.existsByPhone(dto.getPhone())) {
            throw new IllegalArgumentException("이미 등록된 전화번호입니다.");
        }
        Branch branch = dto.toEntity();
        log.info("지점 등록 정보: {}", branch);

        return branchRepository.save(branch);
    }

    // 지점 상세 조회
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
    public Branch updateBranch(Long branchId, @Valid BranchUpdateDto request) {
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
        // 법인등록번호 중복 체크
        if (!branch.getCorporationNumber().equals(request.getCorporationNumber()) && branchRepository.existsByCorporationNumber(request.getCorporationNumber())) {
            throw new IllegalArgumentException("이미 등록된 법인등록번호입니다.");
        }
        // 전화번호 중복 체크
        if (!branch.getPhone().equals(request.getPhone()) && branchRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("이미 등록된 전화번호입니다.");
        }

        log.info("지점 변경 전: {}", branch);

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

        branchRepository.delete(branch);
    }


}
