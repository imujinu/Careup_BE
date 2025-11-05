package com.careup.branch.domain.branch.service;

import com.careup.branch.common.file.AwsS3Uploader;
import com.careup.branch.domain.branch.dto.branch.*;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.employee.entity.AuthorityType;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BranchService {

    private final BranchRepository branchRepository;
    private final AwsS3Uploader awsS3Uploader;
    private final EmployeeRepository employeeRepository;
    private final DispatchStatusRepository dispatchStatusRepository;

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

        // 점주 정보 조회
        LocalDate today = LocalDate.now();
        BranchDto.OwnerInfoDto ownerInfo = findBranchOwnerForDto(findBranch, today);

        return BranchDto.fromEntity(findBranch, ownerInfo);
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

    /**
     * Branch ID 목록으로 Branch 정보 조회 (ordering 서비스용)
     */
    public List<BranchSimpleDto> getBranchesByIds(List<Long> branchIds) {
        List<Branch> branches = branchRepository.findAllById(branchIds);

        return branches.stream()
                .map(branch -> BranchSimpleDto.builder()
                        .id(branch.getId())
                        .name(branch.getName())
                        .address(branch.getAddress())
                        .addressDetail(branch.getAddressDetail())
                        .latitude(branch.getLatitude())
                        .longitude(branch.getLongitude())
                        .phone(branch.getPhone())
                        .email(branch.getEmail())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 특정 지점의 인근 지점 조회 (위치 기반)
     */
    public List<NearbyBranchDto> getNearbyBranches(Long branchId, Double radiusKm) {
        Branch targetBranch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지점입니다."));

        log.info("인근 지점 조회 시작 - 기준 지점 ID: {}, 이름: {}, 위도: {}, 경도: {}, 반경: {}km",
                branchId, targetBranch.getName(), targetBranch.getLatitude(), targetBranch.getLongitude(), radiusKm);

        if (targetBranch.getLatitude() == null || targetBranch.getLongitude() == null) {
            log.warn("지점 ID {}의 위치 정보가 없습니다. (위도: {}, 경도: {})",
                    branchId, targetBranch.getLatitude(), targetBranch.getLongitude());
            throw new IllegalArgumentException("해당 지점의 위치 정보가 없습니다. 지점 정보를 업데이트해주세요.");
        }

        // 모든 지점 조회 (자신 제외)
        List<Branch> allBranches = branchRepository.findAll();
        log.info("전체 지점 수: {}", allBranches.size());

        List<Branch> branchesWithLocation = allBranches.stream()
                .filter(branch -> !branch.getId().equals(branchId))
                .filter(branch -> branch.getLatitude() != null && branch.getLongitude() != null)
                .collect(Collectors.toList());

        log.info("위치 정보가 있는 지점 수 (자신 제외): {}", branchesWithLocation.size());

        // 거리 계산 및 반경 내 지점 필터링
        List<NearbyBranchDto> nearbyBranches = branchesWithLocation.stream()
                .map(branch -> {
                    double distance = calculateDistance(
                            targetBranch.getLatitude(), targetBranch.getLongitude(),
                            branch.getLatitude(), branch.getLongitude()
                    );

                    log.debug("지점 ID: {}, 이름: {}, 거리: {}km", branch.getId(), branch.getName(), distance);

                    return NearbyBranchDto.builder()
                            .id(branch.getId())
                            .name(branch.getName())
                            .address(branch.getAddress())
                            .latitude(branch.getLatitude())
                            .longitude(branch.getLongitude())
                            .distance(distance)
                            .build();
                })
                .filter(dto -> dto.getDistance() <= radiusKm)
                .sorted(Comparator.comparing(NearbyBranchDto::getDistance))
                .collect(Collectors.toList());

        log.info("반경 {}km 내 인근 지점 수: {}", radiusKm, nearbyBranches.size());

        return nearbyBranches;
    }

    /**
     * Haversine 공식을 사용한 두 지점 간 거리 계산 (km)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371; // 지구 반경 (km)

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    // ==================== 고객용 공개 API ====================

    /**
     * 고객용 전체 지점 목록 조회 (권한 제한 없음)
     */
    @Transactional(readOnly = true)
    public List<BranchSimpleDto> getPublicBranchList() {
        List<Branch> branches = branchRepository.findAll();

        log.info("고객용 지점 목록 조회 - 총 {}개", branches.size());

        return branches.stream()
                .map(branch -> BranchSimpleDto.builder()
                        .id(branch.getId())
                        .name(branch.getName())
                        .address(branch.getAddress())
                        .addressDetail(branch.getAddressDetail())
                        .latitude(branch.getLatitude())
                        .longitude(branch.getLongitude())
                        .phone(branch.getPhone())
                        .email(branch.getEmail())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 고객용 위치 기반 인근 지점 조회
     */
    @Transactional(readOnly = true)
    public List<NearbyBranchDto> getNearbyBranchesByCoordinate(Double latitude, Double longitude, Double radiusKm) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("위도와 경도 정보가 필요합니다.");
        }

        List<Branch> allBranches = branchRepository.findAll().stream()
                .filter(branch -> branch.getLatitude() != null && branch.getLongitude() != null)
                .collect(Collectors.toList());

        // 거리 계산 및 반경 내 지점 필터링
        List<NearbyBranchDto> nearbyBranches = allBranches.stream()
                .map(branch -> {
                    double distance = calculateDistance(
                            latitude, longitude,
                            branch.getLatitude(), branch.getLongitude()
                    );

                    return NearbyBranchDto.builder()
                            .id(branch.getId())
                            .name(branch.getName())
                            .address(branch.getAddress())
                            .latitude(branch.getLatitude())
                            .longitude(branch.getLongitude())
                            .distance(distance)
                            .build();
                })
                .filter(dto -> dto.getDistance() <= radiusKm)
                .sorted(Comparator.comparing(NearbyBranchDto::getDistance))
                .collect(Collectors.toList());

        log.info("인근 지점 조회 - 위치: ({}, {}), 반경: {}km, 결과: {}개",
                latitude, longitude, radiusKm, nearbyBranches.size());

        return nearbyBranches;
    }

    /**
     * 내 소속 지점 조회 (지점/가맹점 관리자 및 직원용)
     * JWT 토큰에서 employeeId를 추출하여 해당 직원의 현재 배치된 지점 정보와 점주 정보를 조회
     *
     * @return MyBranchDto - 지점 상세 정보 + 점주 정보
     * @throws IllegalArgumentException - 직원 정보가 없거나 배치된 지점이 없는 경우
     */
    @Transactional(readOnly = true)
    public MyBranchDto getMyBranch() {
        // 1. 인증 정보 추출
        Auth auth = readAuth();

        // 2. 직원 정보 조회
        Employee employee = employeeRepository.findById(auth.employeeId())
                .orElseThrow(() -> new IllegalArgumentException("직원 정보를 찾을 수 없습니다."));

        // 3. 현재 배치된 지점 조회
        LocalDate today = LocalDate.now();
        Optional<DispatchStatus> currentDispatch = dispatchStatusRepository
                .findFirstByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqualOrderByAssignedFromDesc(
                        employee, "N", today, today
                );

        if (currentDispatch.isEmpty()) {
            throw new IllegalArgumentException("현재 배치된 지점이 없습니다.");
        }

        Branch branch = currentDispatch.get().getBranch();

        log.info("내 소속 지점 조회 - 직원: {} ({}), 지점: {} ({})",
                employee.getName(), employee.getId(), branch.getName(), branch.getId());

        // 4. 점주 정보 조회 (해당 지점의 BRANCH_ADMIN 또는 FRANCHISE_OWNER)
        MyBranchDto.OwnerInfoDto ownerInfo = findBranchOwner(branch, today);

        // 5. DTO 반환
        return MyBranchDto.fromEntity(branch, ownerInfo);
    }

    /**
     * 지점의 점주(BRANCH_ADMIN 또는 FRANCHISE_OWNER) 정보 조회 (MyBranchDto용)
     */
    private MyBranchDto.OwnerInfoDto findBranchOwner(Branch branch, LocalDate today) {
        // 해당 지점에 배치된 직원 중 BRANCH_ADMIN 또는 FRANCHISE_OWNER 권한을 가진 직원 조회
        List<DispatchStatus> dispatches = dispatchStatusRepository
                .findByBranchInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        List.of(branch), "N", today, today
                );

        Optional<Employee> owner = dispatches.stream()
                .map(DispatchStatus::getEmployee)
                .filter(emp -> emp.getAuthorityType() == AuthorityType.BRANCH_ADMIN
                            || emp.getAuthorityType() == AuthorityType.FRANCHISE_OWNER)
                .findFirst();

        if (owner.isEmpty()) {
            log.warn("지점 {} ({})에 점주 정보가 없습니다.", branch.getName(), branch.getId());
            return null;
        }

        Employee ownerEmployee = owner.get();
        return MyBranchDto.OwnerInfoDto.builder()
                .employeeId(ownerEmployee.getId())
                .employeeNumber(ownerEmployee.getEmployeeNumber())
                .name(ownerEmployee.getName())
                .email(ownerEmployee.getEmail())
                .mobile(ownerEmployee.getMobile())
                .authorityType(ownerEmployee.getAuthorityType().name())
                .profileImageUrl(ownerEmployee.getProfileImageUrl())
                .build();
    }

    /**
     * 지점의 점주(BRANCH_ADMIN 또는 FRANCHISE_OWNER) 정보 조회 (BranchDto용)
     */
    private BranchDto.OwnerInfoDto findBranchOwnerForDto(Branch branch, LocalDate today) {
        // 해당 지점에 배치된 직원 중 BRANCH_ADMIN 또는 FRANCHISE_OWNER 권한을 가진 직원 조회
        List<DispatchStatus> dispatches = dispatchStatusRepository
                .findByBranchInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        List.of(branch), "N", today, today
                );

        Optional<Employee> owner = dispatches.stream()
                .map(DispatchStatus::getEmployee)
                .filter(emp -> emp.getAuthorityType() == AuthorityType.BRANCH_ADMIN
                            || emp.getAuthorityType() == AuthorityType.FRANCHISE_OWNER)
                .findFirst();

        if (owner.isEmpty()) {
            log.warn("지점 {} ({})에 점주 정보가 없습니다.", branch.getName(), branch.getId());
            return null;
        }

        Employee ownerEmployee = owner.get();
        return BranchDto.OwnerInfoDto.builder()
                .employeeId(ownerEmployee.getId())
                .employeeNumber(ownerEmployee.getEmployeeNumber())
                .name(ownerEmployee.getName())
                .email(ownerEmployee.getEmail())
                .mobile(ownerEmployee.getMobile())
                .authorityType(ownerEmployee.getAuthorityType().name())
                .profileImageUrl(ownerEmployee.getProfileImageUrl())
                .build();
    }

    /**
     * 인증 정보 추출 헬퍼 메서드
     */
    private Auth readAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getDetails() == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        Object details = authentication.getDetails();
        if (!(details instanceof Claims claims)) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        Long employeeId = claims.get("employeeId", Long.class);
        String role = String.valueOf(claims.get("role"));
        if (employeeId == null || role == null) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        return new Auth(employeeId, role);
    }

    /**
     * 인증 정보 레코드
     */
    private record Auth(Long employeeId, String role) {
        public boolean isBranchOrFranchiseAdmin() {
            return "BRANCH_ADMIN".equals(role) || "FRANCHISE_OWNER".equals(role);
        }
    }
}
