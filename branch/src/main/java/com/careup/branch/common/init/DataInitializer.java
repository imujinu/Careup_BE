package com.careup.branch.common.init;

import com.careup.branch.domain.branch.dto.branch.BranchRegisterReqDto;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.entity.OwnershipType;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.branch.service.BranchService;
import com.careup.branch.domain.employee.dto.request.DispatchAssignmentDto;
import com.careup.branch.domain.employee.dto.request.EmployeeCreateDto;
import com.careup.branch.domain.employee.dto.request.JobGradeCreateDto;
import com.careup.branch.domain.employee.dto.request.ScheduleCreateDto;
import com.careup.branch.domain.employee.dto.request.ScheduleMassBlockDto;
import com.careup.branch.domain.employee.dto.request.ScheduleMassCreateDto;
import com.careup.branch.domain.employee.dto.response.JobGradeListDto;
import com.careup.branch.domain.employee.entity.AuthorityType;
import com.careup.branch.domain.employee.entity.AttendanceTemplate;
import com.careup.branch.domain.employee.entity.EmploymentStatus;
import com.careup.branch.domain.employee.entity.EmploymentType;
import com.careup.branch.domain.employee.entity.Gender;
import com.careup.branch.domain.employee.entity.JobGrade;
import com.careup.branch.domain.employee.entity.Relationship;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import com.careup.branch.domain.employee.repository.AttendanceTemplateRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.employee.repository.JobGradeRepository;
import com.careup.branch.domain.employee.repository.ScheduleTypeRepository;
import com.careup.branch.domain.employee.service.EmployeeService;
import com.careup.branch.domain.employee.service.JobGradeService;
import com.careup.branch.domain.employee.service.ScheduleService;
import com.careup.branch.domain.employee.service.ScheduleTypeService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final BranchService branchService;
    private final BranchRepository branchRepository;
    private final EmployeeService employeeService;
    private final EmployeeRepository employeeRepository;
    private final JobGradeService jobGradeService;
    private final JobGradeRepository jobGradeRepository;

    // 스케줄 관련 추가 의존성
    private final ScheduleService scheduleService;
    private final ScheduleTypeService scheduleTypeService;
    private final ScheduleTypeRepository scheduleTypeRepository;
    private final AttendanceTemplateRepository attendanceTemplateRepository;

    private static final String DEFAULT_PROFILE_URL =
            "https://beyond-16-care-up.s3.ap-northeast-2.amazonaws.com/image/employee/profile/default/default_user.png";

    @Override
    @Transactional
    public void run(String... args) {
        runAsSystem(() -> {
            // 1) 지점 보장
            Long hqId = ensureBranch(
                    "본점", OwnershipType.NO, "101-10-00001", "110101-1000001",
                    "서울특별시 중구 을지로 100", "본관 15층",
                    "02-1577-0001", "hq@careup.com",
                    "본사/관리", LocalDate.now().minusYears(8),
                    "37.5665,126.9780", 400, "중앙 관제/정산 총괄"
            );
            Long dongjakId = ensureBranch(
                    "동작점", OwnershipType.NO, "102-20-00002", "220202-2000002",
                    "서울특별시 동작구 상도로 12길 7", "1층",
                    "02-826-0202", "dongjak@careup.com",
                    "카페/음료", LocalDate.now().minusYears(4),
                    "37.5124,126.9399", 300, "대학가 상권 중심, 테이크아웃 강세"
            );
            Long boramaeId = ensureBranch(
                    "보라매점", OwnershipType.YES, "103-30-00003", "330303-3000003",
                    "서울특별시 동작구 보라매로5가길 21", "A동 102호",
                    "02-834-0303", "boramae@careup.com",
                    "카페/디저트", LocalDate.now().minusYears(2),
                    "37.4924,126.9237", 250, "공원 상권, 주말 패밀리 비중 높음"
            );

            // 2) 직급 보장
            Map<String, Long> gradeIds = ensureJobGradesInTable(List.of(
                    "바리스타", "시프트 슈퍼바이저", "부점장", "점장", "지역매니저", "본사매니저"
            ));

            // 3) 직원 보장
            createEmployee(
                    "H2025001", "이승지", "dev.s3lim@gmail.com", "010-2331-4132", Gender.FEMALE,
                    gradeIds.get("본사매니저"),
                    AuthorityType.HQ_ADMIN, EmploymentStatus.ACTIVE, EmploymentType.FULL_TIME,
                    "서울특별시 광진구 아차산로 200", "1203호", "05010",
                    "010-1234-1111", "차은우", Relationship.SPOUSE,
                    LocalDate.of(2003, 1, 1), LocalDate.now().minusYears(5),
                    DEFAULT_PROFILE_URL, "본사 총괄 관리자",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(hqId)
                            .assignedFrom(LocalDate.now().minusYears(4))
                            .assignedTo(LocalDate.now().plusYears(6))
                            .placementYn("N")
                            .build())
            );
            createEmployee(
                    "B2025002", "김상환", "branch.dj@starbucks.co.kr", "010-1234-2222", Gender.MALE,
                    gradeIds.get("점장"),
                    AuthorityType.BRANCH_ADMIN, EmploymentStatus.ACTIVE, EmploymentType.FULL_TIME,
                    "서울특별시 동작구 상도로 22", "302호", "06970",
                    "010-1234-3333", "김윤아", Relationship.SIBLING,
                    LocalDate.of(1988, 11, 2), LocalDate.now().minusYears(3),
                    DEFAULT_PROFILE_URL, "동작점 직영 지점장",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(dongjakId)
                            .assignedFrom(LocalDate.now().minusYears(3))
                            .assignedTo(LocalDate.now().plusYears(5))
                            .placementYn("N")
                            .build())
            );
            createEmployee(
                    "O2025003", "최재혁", "branch.br@starbucks.co.kr", "010-1234-5555", Gender.MALE,
                    gradeIds.get("점장"),
                    AuthorityType.FRANCHISE_OWNER, EmploymentStatus.ACTIVE, EmploymentType.FULL_TIME,
                    "서울특별시 동작구 보라매로 30", "상가동 1층", "07060",
                    "010-7222-3333", "최민수", Relationship.PARENT,
                    LocalDate.of(1970, 5, 1), LocalDate.now().minusYears(2),
                    DEFAULT_PROFILE_URL, "보라매점 가맹점주",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(boramaeId)
                            .assignedFrom(LocalDate.now().minusYears(2))
                            .assignedTo(LocalDate.now().plusYears(8))
                            .placementYn("N")
                            .build())
            );
            createEmployee(
                    "S2025004", "정세윤", "dj-staff1@starbucks.co.kr", "010-1234-7777", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 관악구 봉천로 120", "202호", "08780",
                    "010-2222-3333", "박은주", Relationship.FRIEND,
                    LocalDate.of(1998, 1, 19), LocalDate.now().minusMonths(7),
                    DEFAULT_PROFILE_URL, "동작점 직영 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(dongjakId)
                            .assignedFrom(LocalDate.now().minusMonths(6))
                            .assignedTo(LocalDate.now().plusYears(2))
                            .placementYn("N")
                            .build())
            );
            createEmployee(
                    "S2025005", "김도윤", "br-staff1@starbucks.co.kr", "010-9055-5005", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 영등포구 당산로 50", "1512호", "07220",
                    "010-7000-5555", "김민아", Relationship.NEIGHBOR,
                    LocalDate.of(2000, 9, 23), LocalDate.now().minusMonths(4),
                    DEFAULT_PROFILE_URL, "보라매점 가맹 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(boramaeId)
                            .assignedFrom(LocalDate.now().minusMonths(4))
                            .assignedTo(LocalDate.now().plusYears(1))
                            .placementYn("N")
                            .build())
            );

            // 4) 스케줄 종류 / 템플릿 보장
            Map<String, Long> scheduleTypeIds = ensureScheduleTypes(Map.of(
                    "일반근무", ScheduleTypeCategory.WORK,
                    "야간근무", ScheduleTypeCategory.WORK,
                    "연차",   ScheduleTypeCategory.LEAVE
            ));
            Map<String, Long> templateIds = ensureAttendanceTemplates(List.of(
                    tmpl("주간", LocalTime.of(9,0),  null, null, LocalTime.of(18,0)),
                    tmpl("석간", LocalTime.of(14,0), null, null, LocalTime.of(22,0)),
                    tmpl("야간", LocalTime.of(22,0), null, null, LocalTime.of(6,0))
            ));

            // 5) 여러 경우의 수 스케줄 생성
            seedSchedules(hqId, dongjakId, boramaeId, scheduleTypeIds, templateIds);
        });
    }

    /* ----------------------------- 권한 컨텍스트 ----------------------------- */

    private void runAsSystem(Runnable task) {
        Claims claims = Jwts.claims().setSubject("system@careup.com");
        claims.put("role", "HQ_ADMIN");
        claims.put("employeeId", 0L);

        var auth = new UsernamePasswordAuthenticationToken(
                "system@careup.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_HQ_ADMIN"))
        );
        auth.setDetails(claims);

        var ctx = SecurityContextHolder.getContext();
        var before = ctx.getAuthentication();
        try {
            ctx.setAuthentication(auth);
            task.run();
        } finally {
            if (before != null) ctx.setAuthentication(before);
            else SecurityContextHolder.clearContext();
        }
    }

    /* ----------------------------- 지점/직급/직원 보장 ----------------------------- */

    private Long ensureBranch(String name,
                              OwnershipType ownershipType,
                              String businessNumber,
                              String corporationNumber,
                              String address,
                              String addressDetail,
                              String phone,
                              String email,
                              String businessDomain,
                              LocalDate openDate,
                              String location,
                              Integer geofenceRadius,
                              String remark) {
        Optional<Long> existingId = branchRepository.findAll().stream()
                .filter(b -> name.equals(b.getName()))
                .map(Branch::getId)
                .findFirst();
        if (existingId.isPresent()) return existingId.get();

        BranchRegisterReqDto dto = BranchRegisterReqDto.builder()
                .name(name)
                .businessDomain(businessDomain)
                .ownershipType(ownershipType)
                .openDate(openDate)
                .businessNumber(businessNumber)
                .corporationNumber(corporationNumber)
                .zipcode(guessZip(address))
                .address(address)
                .addressDetail(addressDetail)
                .phone(phone)
                .profileImageUrl(null)
                .email(email)
                .location(location)
                .geofenceRadius(geofenceRadius)
                .remark(remark)
                .attorneyName(null)
                .attorneyPhoneNumber(null)
                .build();
        Branch saved = branchService.registerBranch(dto);
        return saved.getId();
    }

    private Map<String, Long> ensureJobGradesInTable(List<String> names) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (String n : names) {
            Optional<JobGrade> existing = jobGradeRepository.findByName(n);
            if (existing.isPresent()) {
                result.put(n, existing.get().getId());
            } else {
                JobGradeListDto created = jobGradeService.create(JobGradeCreateDto.builder().name(n).build());
                result.put(n, created.getId());
            }
        }
        return result;
    }

    private void createEmployee(String employeeNumber,
                                String name,
                                String email,
                                String mobile,
                                Gender gender,
                                Long jobGradeId,
                                AuthorityType role,
                                EmploymentStatus status,
                                EmploymentType type,
                                String address,
                                String addressDetail,
                                String zipcode,
                                String emergencyTel,
                                String emergencyName,
                                Relationship relationship,
                                LocalDate dateOfBirth,
                                LocalDate hireDate,
                                String profileImageUrl,
                                String remark,
                                List<DispatchAssignmentDto> dispatches) {
        if (employeeRepository.findByEmailIgnoreCase(email).isPresent()) return;

        EmployeeCreateDto dto = EmployeeCreateDto.builder()
                .employeeNumber(employeeNumber)
                .name(name)
                .jobGradeId(jobGradeId)
                .dateOfBirth(dateOfBirth)
                .gender(gender)
                .email(email)
                .zipcode(zipcode)
                .address(address)
                .addressDetail(addressDetail)
                .mobile(mobile)
                .emergencyTel(emergencyTel)
                .emergencyName(emergencyName)
                .relationship(relationship)
                .hireDate(hireDate)
                .terminateDate(null)
                .authorityType(role)
                .employmentStatus(status)
                .employmentType(type)
                .profileImageUrl(profileImageUrl)
                .remark(remark)
                .rawPassword("care1234")
                .dispatches(dispatches)
                .build();

        employeeService.create(dto, null);
    }

    private String guessZip(String address) {
        if (address.contains("중구")) return "04500";
        if (address.contains("동작구")) return "07000";
        if (address.contains("영등포구")) return "07200";
        if (address.contains("관악구")) return "08700";
        return "04000";
    }

    /* ----------------------------- 스케줄 종류 / 템플릿 보장 ----------------------------- */

    private Map<String, Long> ensureScheduleTypes(Map<String, ScheduleTypeCategory> nameToCategory) {
        Map<String, Long> result = new LinkedHashMap<>();
        var all = scheduleTypeRepository.findAll();
        for (var e : nameToCategory.entrySet()) {
            String name = e.getKey();
            ScheduleTypeCategory cat = e.getValue();

            Long id = all.stream()
                    .filter(st -> st.getName().equals(name))
                    .map(st -> st.getId())
                    .findFirst()
                    .orElseGet(() -> scheduleTypeService.create(
                            com.careup.branch.domain.employee.dto.request.ScheduleTypeCreateDto.builder()
                                    .name(name)
                                    .category(cat)
                                    .build()
                    ).getId());
            result.put(name, id);
        }
        return result;
    }

    private static AttendanceTemplateSeed tmpl(String name, LocalTime in, LocalTime bs, LocalTime be, LocalTime out) {
        return new AttendanceTemplateSeed(name, in, bs, be, out);
    }

    private record AttendanceTemplateSeed(String name, LocalTime in, LocalTime bs, LocalTime be, LocalTime out) {}

    private Map<String, Long> ensureAttendanceTemplates(List<AttendanceTemplateSeed> templates) {
        Map<String, Long> result = new LinkedHashMap<>();
        List<AttendanceTemplate> all = attendanceTemplateRepository.findAll();

        for (AttendanceTemplateSeed t : templates) {
            Optional<AttendanceTemplate> existing = all.stream()
                    .filter(x -> x.getName().equals(t.name()))
                    .findFirst();

            Long id = existing.map(AttendanceTemplate::getId).orElseGet(() -> {
                AttendanceTemplate saved = attendanceTemplateRepository.save(
                        AttendanceTemplate.builder()
                                .name(t.name())
                                .defaultClockIn(t.in())
                                .defaultBreakStart(t.bs())
                                .defaultBreakEnd(t.be())
                                .defaultClockOut(t.out())
                                .build()
                );
                return saved.getId();
            });
            result.put(t.name(), id);
        }
        return result;
    }

    /* ----------------------------- 스케줄 생성 (여러 케이스) ----------------------------- */

    private void seedSchedules(Long hqId,
                               Long dongjakId,
                               Long boramaeId,
                               Map<String, Long> scheduleTypeIds,
                               Map<String, Long> templateIds) {

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);

        // 직원/지점 ID
        Long empHQ   = employeeRepository.findByEmployeeNumber("H2025001").orElseThrow().getId(); // HQ 관리자
        Long empDJ   = employeeRepository.findByEmployeeNumber("S2025004").orElseThrow().getId(); // 동작 직원
        Long empBR   = employeeRepository.findByEmployeeNumber("S2025005").orElseThrow().getId(); // 보라매 직원

        Long stWork  = scheduleTypeIds.get("일반근무");
        Long stNight = scheduleTypeIds.get("야간근무");
        Long stLeave = scheduleTypeIds.get("연차");

        Long tplDay  = templateIds.get("주간");
        Long tplEve  = templateIds.get("석간");
        Long tplNight= templateIds.get("야간");

        /* 1) 단건 등록: 본점 HQ 관리자 - 오늘 09:00~18:00 (주간 템플릿, 등록 값은 LocalDateTime 사용)
              - 단건 create/update는 "퇴근 > 출근"이어야 하므로 당일 09->18 로 입력 */
        scheduleService.create(
                ScheduleCreateDto.builder()
                        .employeeId(empHQ)
                        .scheduleTypeId(stWork)
                        .attendanceTemplateId(tplDay)
                        .branchId(hqId)
                        .registeredDate(today)
                        .registeredClockIn(LocalDateTime.of(today, LocalTime.of(9,0)))
                        .registeredBreakStart(null)
                        .registeredBreakEnd(null)
                        .registeredClockOut(LocalDateTime.of(today, LocalTime.of(18,0)))
                        .build()
        );

        /* 2) 단건 등록(크로스데이): 동작 직원 - 어제 22:00 ~ 오늘 06:00 (야간)
              - 단건은 LocalDateTime 비교에서 out > in 이어야 하므로 out을 "익일 06:00"로 명시 */
        scheduleService.create(
                ScheduleCreateDto.builder()
                        .employeeId(empDJ)
                        .scheduleTypeId(stNight)
                        .attendanceTemplateId(tplNight)
                        .branchId(dongjakId)
                        .registeredDate(yesterday)
                        .registeredClockIn(LocalDateTime.of(yesterday, LocalTime.of(22,0)))
                        .registeredBreakStart(null)
                        .registeredBreakEnd(null)
                        .registeredClockOut(LocalDateTime.of(today, LocalTime.of(6,0))) // 익일 06:00
                        .build()
        );

        /* 3) 단건 등록: 보라매 직원 - 내일 연차(종일) */
        scheduleService.create(
                ScheduleCreateDto.builder()
                        .employeeId(empBR)
                        .scheduleTypeId(stLeave)
                        .attendanceTemplateId(null)
                        .branchId(boramaeId)
                        .registeredDate(tomorrow)
                        .registeredClockIn(null)   // LEAVE는 all-day로 처리
                        .registeredBreakStart(null)
                        .registeredBreakEnd(null)
                        .registeredClockOut(null)
                        .build()
        );

        /* 4) 대량 등록(Block): 동작 직원에게 모레/그다음날 "석간(14~22)" 두 건
              - mass-create에서는 LocalTime만 넘겨도 크로스데이 자동 정규화 지원(우리 로직)
              - 여기선 당일 내(14~22)라 정규화 이슈 없음 */
        ScheduleMassCreateDto mass1 = ScheduleMassCreateDto.builder()
                .blocks(List.of(
                        ScheduleMassBlockDto.builder()
                                .branchId(dongjakId)
                                .scheduleTypeId(stWork)
                                .attendanceTemplateId(tplEve)
                                .employeeIds(List.of(empDJ))
                                .dates(List.of(today.plusDays(2), today.plusDays(3)))
                                .registeredClockInTime(LocalTime.of(14, 0))
                                .registeredBreakStartTime(null)
                                .registeredBreakEndTime(null)
                                .registeredClockOutTime(LocalTime.of(22, 0))
                                .build()
                ))
                .items(null)
                .build();
        scheduleService.massCreate(mass1);

        /* 5) 대량 등록(Item): 동작 직원 야간 2건(22:00~06:00; 자동 익일 정규화)
              - mass-create는 출근>퇴근 시간 형태(21:00→06:00 등) 허용, 내부에서 익일로 정규화 */
        ScheduleMassCreateDto mass2 = ScheduleMassCreateDto.builder()
                .blocks(null)
                .items(List.of(
                        // 모레
                        com.careup.branch.domain.employee.dto.request.ScheduleMassItemDto.builder()
                                .employeeId(empDJ)
                                .branchId(dongjakId)
                                .scheduleTypeId(stNight)
                                .attendanceTemplateId(tplNight)
                                .date(today.plusDays(4))
                                .registeredClockInTime(LocalTime.of(22, 0))
                                .registeredBreakStartTime(null)
                                .registeredBreakEndTime(null)
                                .registeredClockOutTime(LocalTime.of(6, 0))
                                .build(),
                        // 그다음날
                        com.careup.branch.domain.employee.dto.request.ScheduleMassItemDto.builder()
                                .employeeId(empDJ)
                                .branchId(dongjakId)
                                .scheduleTypeId(stNight)
                                .attendanceTemplateId(tplNight)
                                .date(today.plusDays(5))
                                .registeredClockInTime(LocalTime.of(22, 0))
                                .registeredBreakStartTime(null)
                                .registeredBreakEndTime(null)
                                .registeredClockOutTime(LocalTime.of(6, 0))
                                .build()
                ))
                .build();
        scheduleService.massCreate(mass2);
    }
}
