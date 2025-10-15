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

            Map<String, Long> gradeIds = ensureJobGradesInTable(List.of(
                    "바리스타", "시프트 슈퍼바이저", "부점장", "점장", "지역매니저", "본사매니저"
            ));

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
            createEmployee(
                    "H2025006", "임진우", "hq.lim@careup.com", "010-1111-6606", Gender.MALE,
                    gradeIds.get("본사매니저"),
                    AuthorityType.HQ_ADMIN, EmploymentStatus.ACTIVE, EmploymentType.FULL_TIME,
                    "서울특별시 성동구 성수이로 55", "501호", "04700",
                    "010-9000-6606", "임수현", Relationship.SIBLING,
                    LocalDate.of(1990, 3, 5), LocalDate.now().minusYears(1),
                    DEFAULT_PROFILE_URL, "본사 운영지원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(hqId)
                            .assignedFrom(LocalDate.now().minusYears(1))
                            .assignedTo(LocalDate.now().plusYears(5))
                            .placementYn("N")
                            .build())
            );
            createEmployee(
                    "S2025007", "이우영", "dj-staff2@starbucks.co.kr", "010-2222-7707", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 동작구 노량진로 100", "302호", "06990",
                    "010-9111-7707", "이정훈", Relationship.PARENT,
                    LocalDate.of(1999, 7, 11), LocalDate.now().minusMonths(2),
                    DEFAULT_PROFILE_URL, "동작점 파트타이머",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(dongjakId)
                            .assignedFrom(LocalDate.now().minusMonths(2))
                            .assignedTo(LocalDate.now().plusYears(2))
                            .placementYn("N")
                            .build())
            );
            createEmployee(
                    "S2025008", "윤세진", "br-staff2@starbucks.co.kr", "010-3333-8808", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 영등포구 국제금융로 20", "808호", "07320",
                    "010-9222-8808", "윤나래", Relationship.SPOUSE,
                    LocalDate.of(2001, 12, 2), LocalDate.now().minusMonths(1),
                    DEFAULT_PROFILE_URL, "보라매점 파트타이머",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(boramaeId)
                            .assignedFrom(LocalDate.now().minusMonths(1))
                            .assignedTo(LocalDate.now().plusYears(3))
                            .placementYn("N")
                            .build())
            );

            Map<String, Long> scheduleTypeIds = ensureScheduleTypes(Map.of(
                    "일반근무", ScheduleTypeCategory.WORK,
                    "야간근무", ScheduleTypeCategory.WORK,
                    "연차", ScheduleTypeCategory.LEAVE,
                    "무급휴가", ScheduleTypeCategory.LEAVE,
                    "특별휴가", ScheduleTypeCategory.LEAVE,
                    "재택근무", ScheduleTypeCategory.WORK,
                    "외근", ScheduleTypeCategory.WORK
            ));
            Map<String, Long> templateIds = ensureAttendanceTemplates(List.of(
                    tmpl("주간", LocalTime.of(9, 0), null, null, LocalTime.of(18, 0)),
                    tmpl("석간", LocalTime.of(14, 0), null, null, LocalTime.of(22, 0)),
                    tmpl("야간", LocalTime.of(22, 0), null, null, LocalTime.of(6, 0))
            ));

            seedSchedules(hqId, dongjakId, boramaeId, scheduleTypeIds, templateIds);
        });
    }

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

    private void seedSchedules(Long hqId,
                               Long dongjakId,
                               Long boramaeId,
                               Map<String, Long> scheduleTypeIds,
                               Map<String, Long> templateIds) {

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);

        Long empHQ = employeeRepository.findByEmployeeNumber("H2025001").orElseThrow().getId();
        Long empDJ = employeeRepository.findByEmployeeNumber("S2025004").orElseThrow().getId();
        Long empBR = employeeRepository.findByEmployeeNumber("S2025005").orElseThrow().getId();
        Long empHQ2 = employeeRepository.findByEmployeeNumber("H2025006").orElseThrow().getId();
        Long empDJ2 = employeeRepository.findByEmployeeNumber("S2025007").orElseThrow().getId();
        Long empBR2 = employeeRepository.findByEmployeeNumber("S2025008").orElseThrow().getId();

        Long stWork = scheduleTypeIds.get("일반근무");
        Long stNight = scheduleTypeIds.get("야간근무");
        Long stLeave = scheduleTypeIds.get("연차");
        Long stUnpaid = scheduleTypeIds.get("무급휴가");
        Long stSpecial = scheduleTypeIds.get("특별휴가");
        Long stWFH = scheduleTypeIds.get("재택근무");
        Long stField = scheduleTypeIds.get("외근");

        Long tplDay = templateIds.get("주간");
        Long tplEve = templateIds.get("석간");
        Long tplNight = templateIds.get("야간");

        scheduleService.create(
                ScheduleCreateDto.builder()
                        .employeeId(empHQ)
                        .scheduleTypeId(stWork)
                        .attendanceTemplateId(tplDay)
                        .branchId(hqId)
                        .registeredDate(today)
                        .registeredClockIn(LocalDateTime.of(today, LocalTime.of(9, 0)))
                        .registeredBreakStart(null)
                        .registeredBreakEnd(null)
                        .registeredClockOut(LocalDateTime.of(today, LocalTime.of(18, 0)))
                        .build()
        );

        scheduleService.create(
                ScheduleCreateDto.builder()
                        .employeeId(empDJ)
                        .scheduleTypeId(stNight)
                        .attendanceTemplateId(tplNight)
                        .branchId(dongjakId)
                        .registeredDate(yesterday)
                        .registeredClockIn(LocalDateTime.of(yesterday, LocalTime.of(22, 0)))
                        .registeredBreakStart(null)
                        .registeredBreakEnd(null)
                        .registeredClockOut(LocalDateTime.of(today, LocalTime.of(6, 0)))
                        .build()
        );

        scheduleService.create(
                ScheduleCreateDto.builder()
                        .employeeId(empBR)
                        .scheduleTypeId(stLeave)
                        .attendanceTemplateId(null)
                        .branchId(boramaeId)
                        .registeredDate(tomorrow)
                        .registeredClockIn(null)
                        .registeredBreakStart(null)
                        .registeredBreakEnd(null)
                        .registeredClockOut(null)
                        .build()
        );

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

        ScheduleMassCreateDto mass2 = ScheduleMassCreateDto.builder()
                .blocks(null)
                .items(List.of(
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

        scheduleService.create(
                ScheduleCreateDto.builder()
                        .employeeId(empHQ2)
                        .scheduleTypeId(stWork)
                        .attendanceTemplateId(tplDay)
                        .branchId(hqId)
                        .registeredDate(today.plusDays(1))
                        .registeredClockIn(LocalDateTime.of(today.plusDays(1), LocalTime.of(9, 0)))
                        .registeredBreakStart(null)
                        .registeredBreakEnd(null)
                        .registeredClockOut(LocalDateTime.of(today.plusDays(1), LocalTime.of(18, 0)))
                        .build()
        );

        scheduleService.create(
                ScheduleCreateDto.builder()
                        .employeeId(empDJ2)
                        .scheduleTypeId(stWork)
                        .attendanceTemplateId(tplEve)
                        .branchId(dongjakId)
                        .registeredDate(today.plusDays(1))
                        .registeredClockIn(LocalDateTime.of(today.plusDays(1), LocalTime.of(14, 0)))
                        .registeredBreakStart(null)
                        .registeredBreakEnd(null)
                        .registeredClockOut(LocalDateTime.of(today.plusDays(1), LocalTime.of(22, 0)))
                        .build()
        );

        scheduleService.create(
                ScheduleCreateDto.builder()
                        .employeeId(empBR2)
                        .scheduleTypeId(stNight)
                        .attendanceTemplateId(tplNight)
                        .branchId(boramaeId)
                        .registeredDate(today)
                        .registeredClockIn(LocalDateTime.of(today, LocalTime.of(22, 0)))
                        .registeredBreakStart(null)
                        .registeredBreakEnd(null)
                        .registeredClockOut(LocalDateTime.of(today.plusDays(1), LocalTime.of(6, 0)))
                        .build()
        );

        scheduleService.create(
                ScheduleCreateDto.builder()
                        .employeeId(empHQ2)
                        .scheduleTypeId(stWFH)
                        .attendanceTemplateId(tplDay)
                        .branchId(hqId)
                        .registeredDate(today.plusDays(2))
                        .registeredClockIn(LocalDateTime.of(today.plusDays(2), LocalTime.of(9, 30)))
                        .registeredBreakStart(null)
                        .registeredBreakEnd(null)
                        .registeredClockOut(LocalDateTime.of(today.plusDays(2), LocalTime.of(18, 30)))
                        .build()
        );

        scheduleService.create(
                ScheduleCreateDto.builder()
                        .employeeId(empDJ2)
                        .scheduleTypeId(stWFH)
                        .attendanceTemplateId(tplDay)
                        .branchId(dongjakId)
                        .registeredDate(today)
                        .registeredClockIn(LocalDateTime.of(today, LocalTime.of(10, 0)))
                        .registeredBreakStart(null)
                        .registeredBreakEnd(null)
                        .registeredClockOut(LocalDateTime.of(today, LocalTime.of(19, 0)))
                        .build()
        );

        scheduleService.create(
                ScheduleCreateDto.builder()
                        .employeeId(empHQ)
                        .scheduleTypeId(stField)
                        .attendanceTemplateId(tplDay)
                        .branchId(hqId)
                        .registeredDate(today.plusDays(3))
                        .registeredClockIn(LocalDateTime.of(today.plusDays(3), LocalTime.of(10, 0)))
                        .registeredBreakStart(null)
                        .registeredBreakEnd(null)
                        .registeredClockOut(LocalDateTime.of(today.plusDays(3), LocalTime.of(17, 0)))
                        .build()
        );

        scheduleService.create(
                ScheduleCreateDto.builder()
                        .employeeId(empBR)
                        .scheduleTypeId(stUnpaid)
                        .attendanceTemplateId(null)
                        .branchId(boramaeId)
                        .registeredDate(today.plusDays(6))
                        .registeredClockIn(null)
                        .registeredBreakStart(null)
                        .registeredBreakEnd(null)
                        .registeredClockOut(null)
                        .build()
        );

        scheduleService.create(
                ScheduleCreateDto.builder()
                        .employeeId(empDJ)
                        .scheduleTypeId(stSpecial)
                        .attendanceTemplateId(null)
                        .branchId(dongjakId)
                        .registeredDate(today.plusDays(7))
                        .registeredClockIn(null)
                        .registeredBreakStart(null)
                        .registeredBreakEnd(null)
                        .registeredClockOut(null)
                        .build()
        );

        ScheduleMassCreateDto massWFHWeek = ScheduleMassCreateDto.builder()
                .blocks(List.of(
                        ScheduleMassBlockDto.builder()
                                .branchId(hqId)
                                .scheduleTypeId(stWFH)
                                .attendanceTemplateId(tplDay)
                                .employeeIds(List.of(empHQ2))
                                .dates(List.of(today.plusDays(8), today.plusDays(9), today.plusDays(10)))
                                .registeredClockInTime(LocalTime.of(9, 0))
                                .registeredBreakStartTime(null)
                                .registeredBreakEndTime(null)
                                .registeredClockOutTime(LocalTime.of(18, 0))
                                .build()
                ))
                .items(null)
                .build();
        scheduleService.massCreate(massWFHWeek);

        ScheduleMassCreateDto massFieldDJ2 = ScheduleMassCreateDto.builder()
                .blocks(null)
                .items(List.of(
                        com.careup.branch.domain.employee.dto.request.ScheduleMassItemDto.builder()
                                .employeeId(empDJ2)
                                .branchId(dongjakId)
                                .scheduleTypeId(stField)
                                .attendanceTemplateId(tplDay)
                                .date(today.plusDays(11))
                                .registeredClockInTime(LocalTime.of(11, 0))
                                .registeredBreakStartTime(null)
                                .registeredBreakEndTime(null)
                                .registeredClockOutTime(LocalTime.of(17, 0))
                                .build(),
                        com.careup.branch.domain.employee.dto.request.ScheduleMassItemDto.builder()
                                .employeeId(empDJ2)
                                .branchId(dongjakId)
                                .scheduleTypeId(stField)
                                .attendanceTemplateId(tplDay)
                                .date(today.plusDays(12))
                                .registeredClockInTime(LocalTime.of(13, 0))
                                .registeredBreakStartTime(null)
                                .registeredBreakEndTime(null)
                                .registeredClockOutTime(LocalTime.of(20, 0))
                                .build()
                ))
                .build();
        scheduleService.massCreate(massFieldDJ2);
    }
}
