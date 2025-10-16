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
import com.careup.branch.domain.employee.dto.request.ScheduleMassItemDto;
import com.careup.branch.domain.employee.dto.request.WorkTypeUpsertDto;
import com.careup.branch.domain.employee.dto.request.LeaveTypeUpsertDto;
import com.careup.branch.domain.employee.dto.response.JobGradeListDto;
import com.careup.branch.domain.employee.dto.response.WorkTypeDetailDto;
import com.careup.branch.domain.employee.dto.response.LeaveTypeDetailDto;
import com.careup.branch.domain.employee.entity.AuthorityType;
import com.careup.branch.domain.employee.entity.AttendanceTemplate;
import com.careup.branch.domain.employee.entity.EmploymentStatus;
import com.careup.branch.domain.employee.entity.EmploymentType;
import com.careup.branch.domain.employee.entity.Gender;
import com.careup.branch.domain.employee.entity.Relationship;
import com.careup.branch.domain.employee.repository.AttendanceTemplateRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.employee.repository.JobGradeRepository;
import com.careup.branch.domain.employee.repository.LeaveTypeRepository;
import com.careup.branch.domain.employee.repository.ScheduleRepository;
import com.careup.branch.domain.employee.repository.WorkTypeRepository;
import com.careup.branch.domain.employee.service.AttendanceService;
import com.careup.branch.domain.employee.service.EmployeeService;
import com.careup.branch.domain.employee.service.JobGradeService;
import com.careup.branch.domain.employee.service.LeaveTypeService;
import com.careup.branch.domain.employee.service.ScheduleService;
import com.careup.branch.domain.employee.service.WorkTypeService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
@Profile({"local","dev"})
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final BranchService branchService;
    private final BranchRepository branchRepository;
    private final EmployeeService employeeService;
    private final EmployeeRepository employeeRepository;
    private final JobGradeService jobGradeService;
    private final JobGradeRepository jobGradeRepository;
    private final ScheduleService scheduleService;
    private final WorkTypeService workTypeService;
    private final LeaveTypeService leaveTypeService;
    private final WorkTypeRepository workTypeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final AttendanceTemplateRepository attendanceTemplateRepository;
    private final AttendanceService attendanceService;
    private final ScheduleRepository scheduleRepository;

    private static final String DEFAULT_PROFILE_URL =
            "https://beyond-16-care-up.s3.ap-northeast-2.amazonaws.com/image/employee/profile/default/default_user.png";

    @Override
    @Transactional
    public void run(String... args) {
        if (employeeRepository.count() > 0L) return;

        runAsSystem(() -> {
            Long hqId = ensureBranch(
                    "본점", OwnershipType.NO, "101-10-00001", "110101-1000001",
                    "서울특별시 중구 을지로 100", "본관 15층",
                    "02-1577-0001", "hq@careup.com",
                    "본사/관리", LocalDate.of(2017, 1, 1),
                    37.5665, 126.9780, 400, "중앙 관제/정산 총괄"
            );
            Long dongjakId = ensureBranch(
                    "동작점", OwnershipType.NO, "102-20-00002", "220202-2000002",
                    "서울특별시 동작구 상도로 12길 7", "1층",
                    "02-826-0202", "dongjak@careup.com",
                    "카페/음료", LocalDate.of(2021, 1, 1),
                    37.5124, 126.9399, 300, "대학가 상권 중심, 테이크아웃 강세"
            );
            Long boramaeId = ensureBranch(
                    "보라매점", OwnershipType.YES, "103-30-00003", "330303-3000003",
                    "서울특별시 동작구 보라매로5가길 21", "A동 102호",
                    "02-834-0303", "boramae@careup.com",
                    "카페/디저트", LocalDate.of(2023, 1, 1),
                    37.4924, 126.9237, 250, "공원 상권, 주말 패밀리 비중 높음"
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
                    LocalDate.of(2003, 1, 1), LocalDate.of(2020, 1, 1),
                    DEFAULT_PROFILE_URL, "본사 총괄 관리자",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(hqId).assignedFrom(LocalDate.of(2021,1,1))
                            .assignedTo(LocalDate.of(2031,1,1)).placementYn("N").build())
            );
            createEmployee(
                    "B2025002", "김상환", "branch.dj@starbucks.co.kr", "010-1234-2222", Gender.MALE,
                    gradeIds.get("점장"),
                    AuthorityType.BRANCH_ADMIN, EmploymentStatus.ACTIVE, EmploymentType.FULL_TIME,
                    "서울특별시 동작구 상도로 22", "302호", "06970",
                    "010-1234-3333", "김윤아", Relationship.SIBLING,
                    LocalDate.of(1988, 11, 2), LocalDate.of(2022, 1, 1),
                    DEFAULT_PROFILE_URL, "동작점 직영 지점장",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(dongjakId).assignedFrom(LocalDate.of(2022,1,1))
                            .assignedTo(LocalDate.of(2030,1,1)).placementYn("N").build())
            );
            createEmployee(
                    "O2025003", "최재혁", "branch.br@starbucks.co.kr", "010-1234-5555", Gender.MALE,
                    gradeIds.get("점장"),
                    AuthorityType.FRANCHISE_OWNER, EmploymentStatus.ACTIVE, EmploymentType.FULL_TIME,
                    "서울특별시 동작구 보라매로 30", "상가동 1층", "07060",
                    "010-7222-3333", "최민수", Relationship.PARENT,
                    LocalDate.of(1970, 5, 1), LocalDate.of(2023, 1, 1),
                    DEFAULT_PROFILE_URL, "보라매점 가맹점주",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(boramaeId).assignedFrom(LocalDate.of(2023,1,1))
                            .assignedTo(LocalDate.of(2033,1,1)).placementYn("N").build())
            );
            createEmployee(
                    "S2025004", "정세윤", "dj-staff1@starbucks.co.kr", "010-1234-7777", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 관악구 봉천로 120", "202호", "08780",
                    "010-2222-3333", "박은주", Relationship.FRIEND,
                    LocalDate.of(1998, 1, 19), LocalDate.of(2024, 6, 1),
                    DEFAULT_PROFILE_URL, "동작점 직영 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(dongjakId).assignedFrom(LocalDate.of(2024,6,1))
                            .assignedTo(LocalDate.of(2027,6,1)).placementYn("N").build())
            );
            createEmployee(
                    "S2025005", "김도윤", "br-staff1@starbucks.co.kr", "010-9055-5005", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 영등포구 당산로 50", "1512호", "07220",
                    "010-7000-5555", "김민아", Relationship.NEIGHBOR,
                    LocalDate.of(2000, 9, 23), LocalDate.of(2024, 9, 1),
                    DEFAULT_PROFILE_URL, "보라매점 가맹 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(boramaeId).assignedFrom(LocalDate.of(2024,9,1))
                            .assignedTo(LocalDate.of(2026,9,1)).placementYn("N").build())
            );
            createEmployee(
                    "H2025006", "임진우", "hq.lim@careup.com", "010-1111-6606", Gender.MALE,
                    gradeIds.get("본사매니저"),
                    AuthorityType.HQ_ADMIN, EmploymentStatus.ACTIVE, EmploymentType.FULL_TIME,
                    "서울특별시 성동구 성수이로 55", "501호", "04700",
                    "010-9000-6606", "임수현", Relationship.SIBLING,
                    LocalDate.of(1990, 3, 5), LocalDate.of(2024, 1, 1),
                    DEFAULT_PROFILE_URL, "본사 운영지원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(hqId).assignedFrom(LocalDate.of(2024,1,1))
                            .assignedTo(LocalDate.of(2029,1,1)).placementYn("N").build())
            );
            createEmployee(
                    "S2025007", "이우영", "dj-staff2@starbucks.co.kr", "010-2222-7707", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 동작구 노량진로 100", "302호", "06990",
                    "010-9111-7707", "이정훈", Relationship.PARENT,
                    LocalDate.of(1999, 7, 11), LocalDate.of(2024, 11, 1),
                    DEFAULT_PROFILE_URL, "동작점 파트타이머",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(dongjakId).assignedFrom(LocalDate.of(2024,11,1))
                            .assignedTo(LocalDate.of(2026,11,1)).placementYn("N").build())
            );
            createEmployee(
                    "S2025008", "윤세진", "br-staff2@starbucks.co.kr", "010-3333-8808", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 영등포구 국제금융로 20", "808호", "07320",
                    "010-9222-8808", "윤나래", Relationship.SPOUSE,
                    LocalDate.of(2001, 12, 2), LocalDate.of(2024, 12, 1),
                    DEFAULT_PROFILE_URL, "보라매점 파트타이머",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(boramaeId).assignedFrom(LocalDate.of(2024,12,1))
                            .assignedTo(LocalDate.of(2027,12,1)).placementYn("N").build())
            );

            Map<String, Long> workTypeIds = ensureWorkTypes(List.of("일반근무", "야간근무", "재택근무", "외근"));
            Map<String, Long> leaveTypeIds = ensureLeaveTypes(List.of("연차", "무급휴가", "특별휴가"));

            Map<String, Long> templateIds = ensureAttendanceTemplates(List.of(
                    tmpl("주간", LocalTime.of(9, 0),  LocalTime.of(12,30), LocalTime.of(13,30), LocalTime.of(18, 0)),
                    tmpl("석간", LocalTime.of(14, 0), LocalTime.of(18, 0),  LocalTime.of(19, 0),  LocalTime.of(22, 0)),
                    tmpl("야간", LocalTime.of(22, 0), LocalTime.of(2, 0),   LocalTime.of(3, 0),   LocalTime.of(6, 0))
            ));

            seedSchedules(hqId, dongjakId, boramaeId, workTypeIds, leaveTypeIds, templateIds);
            seedAttendanceRecordsFixed();
        });
    }

    private void runAsSystem(Runnable task) {
        Claims claims = Jwts.claims().setSubject("system@careup.com");
        claims.put("role", "HQ_ADMIN");
        claims.put("employeeId", 0L);

        var auth = new UsernamePasswordAuthenticationToken(
                "system@careup.com", null, List.of(new SimpleGrantedAuthority("ROLE_HQ_ADMIN"))
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

    private Long ensureBranch(String name, OwnershipType ownershipType, String businessNumber, String corporationNumber,
                              String address, String addressDetail, String phone, String email,
                              String businessDomain, LocalDate openDate,
                              Double latitude, Double longitude, Integer geofenceRadius, String remark) {

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
                .email(email)
                .latitude(latitude)
                .longitude(longitude)
                .geofenceRadius(Objects.requireNonNullElse(geofenceRadius, 200))
                .remark(remark)
                .attorneyName(null)
                .attorneyPhoneNumber(null)
                .build();
        Branch saved = branchService.registerBranch(dto, null);
        return saved.getId();
    }

    private Map<String, Long> ensureJobGradesInTable(List<String> names) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (String n : names) {
            jobGradeRepository.findByName(n).ifPresentOrElse(
                    j -> result.put(n, j.getId()),
                    () -> {
                        JobGradeListDto created = jobGradeService.create(JobGradeCreateDto.builder().name(n).build());
                        result.put(n, created.getId());
                    }
            );
        }
        return result;
    }

    private void createEmployee(String employeeNumber, String name, String email, String mobile, Gender gender,
                                Long jobGradeId, AuthorityType role, EmploymentStatus status, EmploymentType type,
                                String address, String addressDetail, String zipcode,
                                String emergencyTel, String emergencyName, Relationship relationship,
                                LocalDate dateOfBirth, LocalDate hireDate,
                                String profileImageUrl, String remark,
                                List<DispatchAssignmentDto> dispatches) {

        if (employeeRepository.findByEmailIgnoreCase(email).isPresent()) return;

        EmployeeCreateDto dto = EmployeeCreateDto.builder()
                .employeeNumber(employeeNumber).name(name).jobGradeId(jobGradeId)
                .dateOfBirth(dateOfBirth).gender(gender).email(email)
                .zipcode(zipcode).address(address).addressDetail(addressDetail)
                .mobile(mobile).emergencyTel(emergencyTel).emergencyName(emergencyName).relationship(relationship)
                .hireDate(hireDate).terminateDate(null)
                .authorityType(role).employmentStatus(status).employmentType(type)
                .profileImageUrl(profileImageUrl).remark(remark)
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

    private Map<String, Long> ensureWorkTypes(List<String> names) {
        Map<String, Long> result = new LinkedHashMap<>();
        var all = workTypeRepository.findAll();

        for (String name : names) {
            Long id = all.stream()
                    .filter(w -> w.getName().equals(name))
                    .map(w -> w.getId())
                    .findFirst()
                    .orElseGet(() -> {
                        boolean geofenceRequired =
                                switch (name) {
                                    case "재택근무" -> false;
                                    default -> true;
                                };
                        WorkTypeDetailDto created = workTypeService.create(
                                WorkTypeUpsertDto.builder()
                                        .name(name)
                                        .geofenceRequired(geofenceRequired)
                                        .build()
                        );
                        return created.getId();
                    });
            result.put(name, id);
        }
        return result;
    }

    private Map<String, Long> ensureLeaveTypes(List<String> names) {
        Map<String, Long> result = new LinkedHashMap<>();
        var all = leaveTypeRepository.findAll();

        Set<String> paidSet = Set.of("연차", "특별휴가");

        for (String name : names) {
            Long id = all.stream()
                    .filter(l -> l.getName().equals(name))
                    .map(l -> l.getId())
                    .findFirst()
                    .orElseGet(() -> {
                        boolean paid = paidSet.contains(name);
                        LeaveTypeDetailDto created = leaveTypeService.create(
                                LeaveTypeUpsertDto.builder()
                                        .name(name)
                                        .paid(paid)
                                        .build()
                        );
                        return created.getId();
                    });
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
            var existing = all.stream().filter(x -> x.getName().equals(t.name())).findFirst();
            Long id = existing.map(AttendanceTemplate::getId).orElseGet(() -> {
                AttendanceTemplate saved = attendanceTemplateRepository.save(
                        AttendanceTemplate.builder()
                                .name(t.name())
                                .defaultClockIn(t.in()).defaultBreakStart(t.bs())
                                .defaultBreakEnd(t.be()).defaultClockOut(t.out())
                                .build()
                );
                return saved.getId();
            });
            result.put(t.name(), id);
        }
        return result;
    }

    private boolean hasSchedule(Long employeeId, LocalDate date) {
        return scheduleRepository.findByEmployeeIdAndRegisteredDate(employeeId, date).isPresent();
    }

    private void seedSchedules(Long hqId, Long dongjakId, Long boramaeId,
                               Map<String, Long> workTypeIds, Map<String, Long> leaveTypeIds, Map<String, Long> templateIds) {

        LocalDate D0 = LocalDate.of(2025, 1, 13);
        LocalDate Dm1 = D0.minusDays(1);
        LocalDate Dp1 = D0.plusDays(1);
        LocalDate Dp2 = D0.plusDays(2);
        LocalDate Dp3 = D0.plusDays(3);
        LocalDate Dp4 = D0.plusDays(4);
        LocalDate Dp5 = D0.plusDays(5);
        LocalDate Dp6 = D0.plusDays(6);
        LocalDate Dp7 = D0.plusDays(7);
        LocalDate Dp8 = D0.plusDays(8);
        LocalDate Dp9 = D0.plusDays(9);
        LocalDate Dp10 = D0.plusDays(10);
        LocalDate Dp11 = D0.plusDays(11);
        LocalDate Dp12 = D0.plusDays(12);

        Long empHQ  = employeeRepository.findByEmployeeNumber("H2025001").orElseThrow().getId();
        Long empDJ  = employeeRepository.findByEmployeeNumber("S2025004").orElseThrow().getId();
        Long empBR  = employeeRepository.findByEmployeeNumber("S2025005").orElseThrow().getId();
        Long empHQ2 = employeeRepository.findByEmployeeNumber("H2025006").orElseThrow().getId();
        Long empDJ2 = employeeRepository.findByEmployeeNumber("S2025007").orElseThrow().getId();
        Long empBR2 = employeeRepository.findByEmployeeNumber("S2025008").orElseThrow().getId();

        Long wtWork  = Objects.requireNonNull(workTypeIds.get("일반근무"));
        Long wtNight = Objects.requireNonNull(workTypeIds.get("야간근무"));
        Long wtWFH   = Objects.requireNonNull(workTypeIds.get("재택근무"));
        Long wtField = Objects.requireNonNull(workTypeIds.get("외근"));

        Long ltAnnual  = Objects.requireNonNull(leaveTypeIds.get("연차"));
        Long ltUnpaid  = Objects.requireNonNull(leaveTypeIds.get("무급휴가"));
        Long ltSpecial = Objects.requireNonNull(leaveTypeIds.get("특별휴가"));

        Long tplDay   = Objects.requireNonNull(templateIds.get("주간"));
        Long tplEve   = Objects.requireNonNull(templateIds.get("석간"));
        Long tplNight = Objects.requireNonNull(templateIds.get("야간"));

        if (!hasSchedule(empHQ, D0)) {
            scheduleService.create(ScheduleCreateDto.builder()
                    .employeeId(empHQ)
                    .workTypeId(wtWork).attendanceTemplateId(tplDay).branchId(hqId)
                    .registeredDate(D0)
                    .registeredClockIn(LocalDateTime.of(D0, LocalTime.of(9, 0)))
                    .registeredBreakStart(LocalDateTime.of(D0, LocalTime.of(12, 30)))
                    .registeredBreakEnd(LocalDateTime.of(D0, LocalTime.of(13, 30)))
                    .registeredClockOut(LocalDateTime.of(D0, LocalTime.of(18, 0)))
                    .build());
        }

        if (!hasSchedule(empDJ, Dm1)) {
            scheduleService.create(ScheduleCreateDto.builder()
                    .employeeId(empDJ)
                    .workTypeId(wtNight).attendanceTemplateId(tplNight).branchId(dongjakId)
                    .registeredDate(Dm1)
                    .registeredClockIn(LocalDateTime.of(Dm1, LocalTime.of(22, 0)))
                    .registeredBreakStart(LocalDateTime.of(D0, LocalTime.of(2, 0)))
                    .registeredBreakEnd(LocalDateTime.of(D0, LocalTime.of(3, 0)))
                    .registeredClockOut(LocalDateTime.of(D0, LocalTime.of(6, 0)))
                    .build());
        }

        if (!hasSchedule(empBR, Dp1)) {
            scheduleService.create(ScheduleCreateDto.builder()
                    .employeeId(empBR)
                    .leaveTypeId(ltAnnual).branchId(boramaeId)
                    .registeredDate(Dp1)
                    .build());
        }

        var blockDates = List.of(Dp2, Dp3);
        if (blockDates.stream().anyMatch(d -> !hasSchedule(empDJ, d))) {
            scheduleService.massCreate(ScheduleMassCreateDto.builder()
                    .blocks(List.of(
                            ScheduleMassBlockDto.builder()
                                    .branchId(dongjakId)
                                    .workTypeId(wtWork)
                                    .attendanceTemplateId(tplEve)
                                    .employeeIds(List.of(empDJ))
                                    .dates(blockDates.stream().filter(d -> !hasSchedule(empDJ, d)).toList())
                                    .registeredClockInTime(LocalTime.of(14, 0))
                                    .registeredBreakStartTime(LocalTime.of(18, 0))
                                    .registeredBreakEndTime(LocalTime.of(19, 0))
                                    .registeredClockOutTime(LocalTime.of(22, 0))
                                    .build()
                    ))
                    .build());
        }

        var items = new ArrayList<ScheduleMassItemDto>();
        if (!hasSchedule(empDJ, Dp4)) {
            items.add(ScheduleMassItemDto.builder()
                    .employeeId(empDJ).branchId(dongjakId)
                    .workTypeId(wtNight)
                    .attendanceTemplateId(tplNight).date(Dp4)
                    .registeredClockInTime(LocalTime.of(22, 0))
                    .registeredBreakStartTime(LocalTime.of(2, 0))
                    .registeredBreakEndTime(LocalTime.of(3, 0))
                    .registeredClockOutTime(LocalTime.of(6, 0))
                    .build());
        }
        if (!hasSchedule(empDJ, Dp5)) {
            items.add(ScheduleMassItemDto.builder()
                    .employeeId(empDJ).branchId(dongjakId)
                    .workTypeId(wtNight)
                    .attendanceTemplateId(tplNight).date(Dp5)
                    .registeredClockInTime(LocalTime.of(22, 0))
                    .registeredBreakStartTime(LocalTime.of(2, 0))
                    .registeredBreakEndTime(LocalTime.of(3, 0))
                    .registeredClockOutTime(LocalTime.of(6, 0))
                    .build());
        }
        if (!items.isEmpty()) {
            scheduleService.massCreate(ScheduleMassCreateDto.builder().items(items).build());
        }

        if (!hasSchedule(empHQ2, Dp1)) {
            scheduleService.create(ScheduleCreateDto.builder()
                    .employeeId(empHQ2)
                    .workTypeId(wtWork).attendanceTemplateId(tplDay).branchId(hqId)
                    .registeredDate(Dp1)
                    .registeredClockIn(LocalDateTime.of(Dp1, LocalTime.of(9, 0)))
                    .registeredBreakStart(LocalDateTime.of(Dp1, LocalTime.of(12, 30)))
                    .registeredBreakEnd(LocalDateTime.of(Dp1, LocalTime.of(13, 30)))
                    .registeredClockOut(LocalDateTime.of(Dp1, LocalTime.of(18, 0)))
                    .build());
        }

        if (!hasSchedule(empDJ2, Dp1)) {
            scheduleService.create(ScheduleCreateDto.builder()
                    .employeeId(empDJ2)
                    .workTypeId(wtWork).attendanceTemplateId(tplEve).branchId(dongjakId)
                    .registeredDate(Dp1)
                    .registeredClockIn(LocalDateTime.of(Dp1, LocalTime.of(14, 0)))
                    .registeredBreakStart(LocalDateTime.of(Dp1, LocalTime.of(18, 0)))
                    .registeredBreakEnd(LocalDateTime.of(Dp1, LocalTime.of(19, 0)))
                    .registeredClockOut(LocalDateTime.of(Dp1, LocalTime.of(22, 0)))
                    .build());
        }

        if (!hasSchedule(empBR2, D0)) {
            scheduleService.create(ScheduleCreateDto.builder()
                    .employeeId(empBR2)
                    .workTypeId(wtNight).attendanceTemplateId(tplNight).branchId(boramaeId)
                    .registeredDate(D0)
                    .registeredClockIn(LocalDateTime.of(D0, LocalTime.of(22, 0)))
                    .registeredBreakStart(LocalDateTime.of(Dp1, LocalTime.of(2, 0)))
                    .registeredBreakEnd(LocalDateTime.of(Dp1, LocalTime.of(3, 0)))
                    .registeredClockOut(LocalDateTime.of(Dp1, LocalTime.of(6, 0)))
                    .build());
        }

        if (!hasSchedule(empHQ2, Dp2)) {
            scheduleService.create(ScheduleCreateDto.builder()
                    .employeeId(empHQ2)
                    .workTypeId(wtWFH).attendanceTemplateId(tplDay).branchId(hqId)
                    .registeredDate(Dp2)
                    .registeredClockIn(LocalDateTime.of(Dp2, LocalTime.of(9, 30)))
                    .registeredBreakStart(LocalDateTime.of(Dp2, LocalTime.of(12, 0)))
                    .registeredBreakEnd(LocalDateTime.of(Dp2, LocalTime.of(13, 0)))
                    .registeredClockOut(LocalDateTime.of(Dp2, LocalTime.of(18, 30)))
                    .build());
        }

        if (!hasSchedule(empDJ2, D0)) {
            scheduleService.create(ScheduleCreateDto.builder()
                    .employeeId(empDJ2)
                    .workTypeId(wtWFH).attendanceTemplateId(tplDay).branchId(dongjakId)
                    .registeredDate(D0)
                    .registeredClockIn(LocalDateTime.of(D0, LocalTime.of(10, 0)))
                    .registeredBreakStart(LocalDateTime.of(D0, LocalTime.of(13, 0)))
                    .registeredBreakEnd(LocalDateTime.of(D0, LocalTime.of(14, 0)))
                    .registeredClockOut(LocalDateTime.of(D0, LocalTime.of(19, 0)))
                    .build());
        }

        if (!hasSchedule(empHQ, Dp3)) {
            scheduleService.create(ScheduleCreateDto.builder()
                    .employeeId(empHQ)
                    .workTypeId(wtField).attendanceTemplateId(tplDay).branchId(hqId)
                    .registeredDate(Dp3)
                    .registeredClockIn(LocalDateTime.of(Dp3, LocalTime.of(10, 0)))
                    .registeredBreakStart(LocalDateTime.of(Dp3, LocalTime.of(12, 30)))
                    .registeredBreakEnd(LocalDateTime.of(Dp3, LocalTime.of(13, 30)))
                    .registeredClockOut(LocalDateTime.of(Dp3, LocalTime.of(17, 0)))
                    .build());
        }

        if (!hasSchedule(empBR, Dp6)) {
            scheduleService.create(ScheduleCreateDto.builder()
                    .employeeId(empBR)
                    .leaveTypeId(ltUnpaid).branchId(boramaeId)
                    .registeredDate(Dp6)
                    .build());
        }

        if (!hasSchedule(empDJ, Dp7)) {
            scheduleService.create(ScheduleCreateDto.builder()
                    .employeeId(empDJ)
                    .leaveTypeId(ltSpecial).branchId(dongjakId)
                    .registeredDate(Dp7)
                    .build());
        }

        var wfhDates = List.of(Dp8, Dp9, Dp10);
        if (wfhDates.stream().anyMatch(d -> !hasSchedule(empHQ2, d))) {
            scheduleService.massCreate(ScheduleMassCreateDto.builder()
                    .blocks(List.of(
                            ScheduleMassBlockDto.builder()
                                    .branchId(hqId)
                                    .workTypeId(wtWFH)
                                    .attendanceTemplateId(tplDay)
                                    .employeeIds(List.of(empHQ2))
                                    .dates(wfhDates.stream().filter(d -> !hasSchedule(empHQ2, d)).toList())
                                    .registeredClockInTime(LocalTime.of(9, 0))
                                    .registeredBreakStartTime(LocalTime.of(12, 30))
                                    .registeredBreakEndTime(LocalTime.of(13, 30))
                                    .registeredClockOutTime(LocalTime.of(18, 0))
                                    .build()
                    ))
                    .build());
        }

        var dj2Items = new ArrayList<ScheduleMassItemDto>();
        if (!hasSchedule(empDJ2, Dp11)) {
            dj2Items.add(ScheduleMassItemDto.builder()
                    .employeeId(empDJ2).branchId(dongjakId)
                    .workTypeId(wtField)
                    .attendanceTemplateId(tplDay).date(Dp11)
                    .registeredClockInTime(LocalTime.of(11, 0))
                    .registeredBreakStartTime(LocalTime.of(14, 0))
                    .registeredBreakEndTime(LocalTime.of(15, 0))
                    .registeredClockOutTime(LocalTime.of(17, 0))
                    .build());
        }
        if (!hasSchedule(empDJ2, Dp12)) {
            dj2Items.add(ScheduleMassItemDto.builder()
                    .employeeId(empDJ2).branchId(dongjakId)
                    .workTypeId(wtField)
                    .attendanceTemplateId(tplDay).date(Dp12)
                    .registeredClockInTime(LocalTime.of(13, 0))
                    .registeredBreakStartTime(LocalTime.of(17, 0))
                    .registeredBreakEndTime(LocalTime.of(18, 0))
                    .registeredClockOutTime(LocalTime.of(20, 0))
                    .build());
        }
        if (!dj2Items.isEmpty()) {
            scheduleService.massCreate(ScheduleMassCreateDto.builder().items(dj2Items).build());
        }
    }

    private void seedAttendanceRecordsFixed() {
        LocalDate D0 = LocalDate.of(2025, 1, 13);
        LocalDate Dm1 = D0.minusDays(1);

        var optHQ  = employeeRepository.findByEmployeeNumber("H2025001");
        var optDJ  = employeeRepository.findByEmployeeNumber("S2025004");
        var optDJ2 = employeeRepository.findByEmployeeNumber("S2025007");

        optHQ.flatMap(e -> scheduleRepository.findByEmployeeIdAndRegisteredDate(e.getId(), D0)).ifPresent(s -> {
            double lat = Optional.ofNullable(s.getBranch().getLatitude()).orElse(37.5665);
            double lng = Optional.ofNullable(s.getBranch().getLongitude()).orElse(126.9780);
            attendanceService.clockInAt (s.getId(), lat, lng, LocalDateTime.of(D0, LocalTime.of(9, 2)));
            attendanceService.breakStartAt(s.getId(), lat, lng, LocalDateTime.of(D0, LocalTime.of(12, 31)));
            attendanceService.breakEndAt  (s.getId(), lat, lng, LocalDateTime.of(D0, LocalTime.of(13, 29)));
            attendanceService.clockOutAt  (s.getId(), lat, lng, LocalDateTime.of(D0, LocalTime.of(18, 3)));
        });

        optDJ.flatMap(e -> scheduleRepository.findByEmployeeIdAndRegisteredDate(e.getId(), Dm1)).ifPresent(s -> {
            double lat = Optional.ofNullable(s.getBranch().getLatitude()).orElse(37.5124);
            double lng = Optional.ofNullable(s.getBranch().getLongitude()).orElse(126.9399);
            attendanceService.clockInAt (s.getId(), lat, lng, LocalDateTime.of(Dm1, LocalTime.of(22, 1)));
            attendanceService.breakStartAt(s.getId(), lat, lng, LocalDateTime.of(D0,  LocalTime.of(2, 5)));
            attendanceService.breakEndAt  (s.getId(), lat, lng, LocalDateTime.of(D0,  LocalTime.of(3, 4)));
            attendanceService.clockOutAt  (s.getId(), lat, lng, LocalDateTime.of(D0,  LocalTime.of(6, 2)));
        });

        optDJ2.flatMap(e -> scheduleRepository.findByEmployeeIdAndRegisteredDate(e.getId(), D0)).ifPresent(s -> {
            double lat = Optional.ofNullable(s.getBranch().getLatitude()).orElse(37.5124);
            double lng = Optional.ofNullable(s.getBranch().getLongitude()).orElse(126.9399);
            attendanceService.clockInAt (s.getId(), lat, lng, LocalDateTime.of(D0, LocalTime.of(10, 0)));
            attendanceService.breakStartAt(s.getId(), lat, lng, LocalDateTime.of(D0, LocalTime.of(13, 0)));
            attendanceService.breakEndAt  (s.getId(), lat, lng, LocalDateTime.of(D0, LocalTime.of(13, 59)));
            attendanceService.clockOutAt  (s.getId(), lat, lng, LocalDateTime.of(D0, LocalTime.of(19, 0)));
        });
    }
}
