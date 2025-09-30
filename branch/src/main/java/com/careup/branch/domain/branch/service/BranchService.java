package com.careup.branch.domain.branch.service;

import com.careup.branch.domain.branch.dto.BranchDetailDto;
import com.careup.branch.domain.branch.dto.BranchListResDto;
import com.careup.branch.domain.branch.dto.BranchRegisterReqDto;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
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
        if (branchRepository.existsByCorporationNumber(dto.getCorporationNumber())) {
            throw new IllegalArgumentException("이미 등록된 법인등록번호입니다.");
        }
        if (branchRepository.existsByPhone(dto.getPhone())) {
            throw new IllegalArgumentException("이미 등록된 전화번호입니다.");
        }
        Branch branch = dto.toEntity();
        return branchRepository.save(branch);
    }

    // 지점 목록 조회 (페이징)
    @Transactional(readOnly = true)
    public BranchListResDto getBranchList(int page) {
        // 페이지는 0부터 시작하므로 사용자가 입력한 페이지에서 1을 빼줌
        int adjustedPage = Math.max(0, page - 1);

        // 최신순 정렬 (페이지당 최대 갯수: 10개, 생성일 기준 내림차순)
        Pageable pageable = PageRequest.of(adjustedPage, 10, Sort.by(Sort.Direction.DESC, "createdTime"));

        Page<Branch> findBranches = branchRepository.findAll(pageable);

        // Entity를 DTO로 변환
        Page<BranchDetailDto> branchDtoPage = findBranches.map(branch -> BranchDetailDto.fromEntity(branch));

        return BranchListResDto.fromPage(branchDtoPage);
    }
}
