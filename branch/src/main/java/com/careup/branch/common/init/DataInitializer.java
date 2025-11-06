package com.careup.branch.common.init;

import com.careup.branch.domain.branch.dto.branch.BranchRegisterReqDto;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.entity.OwnershipType;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.branch.service.BranchService;
import com.careup.branch.domain.employee.dto.request.DispatchAssignmentDto;
import com.careup.branch.domain.employee.dto.request.EmployeeCreateDto;
import com.careup.branch.domain.employee.dto.request.JobGradeCreateDto;
import com.careup.branch.domain.employee.dto.request.JobGradeUpdateDto;
import com.careup.branch.domain.employee.dto.request.WorkTypeUpsertDto;
import com.careup.branch.domain.employee.dto.request.LeaveTypeUpsertDto;
import com.careup.branch.domain.employee.dto.response.JobGradeListDto;
import com.careup.branch.domain.employee.dto.response.WorkTypeDetailDto;
import com.careup.branch.domain.employee.dto.response.LeaveTypeDetailDto;
import com.careup.branch.domain.employee.entity.AttendanceTemplate;
import com.careup.branch.domain.employee.entity.AuthorityType;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.entity.EmploymentStatus;
import com.careup.branch.domain.employee.entity.EmploymentType;
import com.careup.branch.domain.employee.entity.Gender;
import com.careup.branch.domain.employee.entity.Relationship;
import com.careup.branch.domain.employee.repository.AttendanceTemplateRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.employee.repository.JobGradeRepository;
import com.careup.branch.domain.employee.repository.WorkTypeRepository;
import com.careup.branch.domain.employee.repository.LeaveTypeRepository;
import com.careup.branch.domain.employee.service.EmployeeService;
import com.careup.branch.domain.employee.service.JobGradeService;
import com.careup.branch.domain.employee.service.LeaveTypeService;
import com.careup.branch.domain.employee.service.WorkTypeService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final WorkTypeService workTypeService;
    private final LeaveTypeService leaveTypeService;
    private final WorkTypeRepository workTypeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final AttendanceTemplateRepository attendanceTemplateRepository;

    private static final String DEFAULT_PROFILE_URL =
            "https://beyond-16-care-up.s3.ap-northeast-2.amazonaws.com/image/employee/profile/default/default_user.png";

    // ===== 일련번호/이름 생성기 =====
    private int staffSeqCounter = 71; // 기존 마지막 S2025070 이후부터 시작
    private int nameIdx = 0;

    private static final List<String> NAME_POOL = List.of(
            "강서연", "김민주", "박시윤", "이도윤", "정하린", "최예린", "한서준", "오예진", "유나리", "장하늘",
            "배지훈", "서다인", "노하람", "문지우", "양민서", "권채윤", "신유담", "임서하", "차현우", "한지훈",
            "오지민", "송서윤", "백도현", "윤지호", "박하율", "최지우", "이서준", "백하린", "권도윤", "조수아",
            "이예진", "김지후", "박민서", "하윤우", "지연우", "이세린", "최하람", "우도하", "지수안", "조진세",
            "김영환", "최민우", "하영석", "정찬중", "문철우", "고아름", "신세아", "김하경", "유지혜", "김창현",
            "이시환", "하현경", "한송이", "박찬식", "최필우", "안준수", "오준영", "최경운", "김지연", "임연아",
            "기여운", "표송이", "채예린", "조은비", "장연우", "김혜수", "강동영", "김대호", "조진혁", "장세인",
            "구도환", "유민우", "하진영", "정봉식", "천송희", "고아라", "진세연", "박지성", "양동근", "안정환",
            "설기현", "이동국", "손흥민", "홍아름", "공대환", "연우진", "모희연", "전소미", "황동영", "임수연",
            "기세은", "이하린", "오은지", "소유진", "백은지", "박소영", "정보경", "유희진", "성민아", "곽라은",
            "서솔빈", "정희경", "하도경", "여경수", "전영훈", "강다나", "백아름", "백지민", "표가은", "신희호",
            "기수호", "오태영", "장성현", "민윤찬", "유연경", "김다은", "장영환", "조혜선", "백성호", "곽형식",
            "최가을", "오동혁", "우세경", "조동연", "유세율", "여라연", "윤예진", "신호창", "배희욱", "김시영",
            "장윤빈", "유동빈", "김태훈", "최진혁", "권도영", "서도훈", "남태희", "차세진", "김훈", "정도은",
            "김세훈", "모준호", "주형진", "임영훈", "배영식", "이예현", "차희준", "엄유리", "문채영", "유준혁",
            "장율진", "진세호", "정연주", "노현우", "허지윤", "홍지훈", "박남현", "남유리", "주희윤", "류현경",
            "한희준", "남지민", "유지현", "이경훈", "오채우", "김동훈", "문지훈", "권지후", "표라율", "홍은진"
    );

    private String nextEmployeeNumber() {
        return "S2025" + String.format("%03d", staffSeqCounter++);
    }

    private String nextName() {
        String n = NAME_POOL.get(nameIdx % NAME_POOL.size());
        nameIdx++;
        return n;
    }

    /**
     * 지점별 5명 자동 생성.
     * 비고(remark)는 반드시 한국어 지점명(remarkPrefixKor)을 사용하여 "{지점명} 추가 직원"으로 기록.
     */
    private void addFiveStaff(Long branchId,
                              String branchKey,
                              String remarkPrefixKor,
                              String address,
                              String addressDetail,
                              String zipcode,
                              LocalDate baseHireDate,
                              Long baristaGradeId) {

        Relationship[] rels = new Relationship[] {
                Relationship.PARENT, Relationship.SIBLING, Relationship.FRIEND, Relationship.SPOUSE, Relationship.NEIGHBOR
        };

        for (int i = 0; i < 5; i++) {
            String empNo = nextEmployeeNumber();
            int currentSeq = staffSeqCounter - 1;
            String name = nextName();
            Gender gender = (currentSeq % 2 == 0) ? Gender.MALE : Gender.FEMALE;

            String email = branchKey + ".staff" + empNo.substring(6) + "@careup.com";
            String mobile = String.format("010-%04d-%04d", 7000 + (currentSeq % 1000), 1000 + (currentSeq % 9000));
            String emergencyTel = String.format("010-%04d-%04d", 9000 + (currentSeq % 1000), 2000 + (currentSeq % 8000));
            String emergencyName = nextName();

            LocalDate hireDate = baseHireDate.plusDays(i);
            LocalDate dob = LocalDate.of(1996 + (currentSeq % 8), 1 + (currentSeq % 12), 1 + (currentSeq % 27));

            createEmployee(
                    empNo,
                    name,
                    email,
                    mobile,
                    gender,
                    baristaGradeId,
                    AuthorityType.STAFF,
                    EmploymentStatus.ACTIVE,
                    EmploymentType.PART_TIME,
                    address,
                    addressDetail,
                    zipcode,
                    emergencyTel,
                    emergencyName,
                    rels[i % rels.length],
                    dob,
                    hireDate,
                    DEFAULT_PROFILE_URL,
                    remarkPrefixKor + " 추가 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(branchId)
                            .assignedFrom(hireDate)
                            .assignedTo(hireDate.plusYears(3))
                            .placementYn("N")
                            .build())
            );
        }
    }

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
                    37.49716551719695, 126.92752643336487, 50, "대학가 상권 중심, 테이크아웃 강세"
            );
            Long boramaeId = ensureBranch(
                    "보라매점", OwnershipType.YES, "103-30-00003", "330303-3000003",
                    "서울특별시 동작구 보라매로5가길 21", "A동 102호",
                    "02-834-0303", "boramae@careup.com",
                    "카페/디저트", LocalDate.of(2023, 1, 1),
                    37.4924, 126.9237, 250, "공원 상권, 주말 패밀리 비중 높음"
            );

            Long samsongId = ensureBranch(
                    "고양삼송점", OwnershipType.YES, "104-40-00004", "440404-4000004",
                    "경기도 고양시 덕양구 삼송로 11", "1층",
                    "031-111-0004", "samsong@careup.com",
                    "카페/음료", LocalDate.of(2022, 3, 1),
                    37.6560, 126.8950, 250, "신도시 상권, 가족 단위 유동 인구"
            );
            Long euljiroId = ensureBranch(
                    "을지로점", OwnershipType.NO, "105-50-00005", "550505-5000005",
                    "서울특별시 중구 을지로 157", "지상 1층",
                    "02-2222-0005", "euljiro@careup.com",
                    "카페/디저트", LocalDate.of(2020, 6, 1),
                    37.5669, 127.0069, 300, "오피스 상권, 평일 점심 피크"
            );
            Long sindaebangId = ensureBranch(
                    "신대방삼거리점", OwnershipType.YES, "106-60-00006", "660606-6000006",
                    "서울특별시 동작구 보라매로 111", "1층",
                    "02-3333-0006", "sindaebang@careup.com",
                    "카페/음료", LocalDate.of(2021, 9, 1),
                    37.4990, 126.9280, 250, "역세권, 퇴근 시간 대기열"
            );
            Long gupabalId = ensureBranch(
                    "구파발점", OwnershipType.NO, "107-70-00007", "770707-7000007",
                    "서울특별시 은평구 진관2로 29", "1층",
                    "02-4444-0007", "gupabal@careup.com",
                    "카페/음료", LocalDate.of(2022, 11, 1),
                    37.6420, 126.9180, 250, "주거 상권, 주말 가족 고객 다수"
            );

            Long magokId = ensureBranch(
                    "마곡나루점", OwnershipType.NO, "108-80-00008", "880808-8000008",
                    "서울특별시 강서구 마곡동로 110", "1층",
                    "02-5000-0008", "magok@careup.com",
                    "카페/디저트", LocalDate.of(2023, 5, 1),
                    37.5687, 126.8292, 280, "마곡나루 오피스/주거 혼합 상권"
            );
            Long seongsuId = ensureBranch(
                    "성수점", OwnershipType.NO, "109-90-00009", "990909-9000009",
                    "서울특별시 성동구 아차산로 11", "지상 1층",
                    "02-5000-0009", "seongsu@careup.com",
                    "카페/디저트", LocalDate.of(2024, 4, 1),
                    37.5446, 127.0553, 260, "핫플 상권, 주말 대기 발생"
            );
            Long jamsilId = ensureBranch(
                    "잠실점", OwnershipType.YES, "110-00-00010", "000000-0000010",
                    "서울특별시 송파구 올림픽로 240", "상가동 2층",
                    "02-5000-0010", "jamsil@careup.com",
                    "카페/음료", LocalDate.of(2022, 8, 1),
                    37.5130, 127.1025, 300, "대형 상권/관광객 유입"
            );
            Long pangyoId = ensureBranch(
                    "판교점", OwnershipType.YES, "111-10-00011", "111111-1000011",
                    "경기도 성남시 분당구 판교역로 145", "B동 101호",
                    "031-500-0011", "pangyo@careup.com",
                    "카페/디저트", LocalDate.of(2024, 3, 1),
                    37.3943, 127.1116, 270, "IT밸리 직장인 상권"
            );
            Long seomyeonId = ensureBranch(
                    "부산서면점", OwnershipType.NO, "112-20-00012", "222222-2000012",
                    "부산광역시 부산진구 중앙대로 690", "1층",
                    "051-500-0012", "seomyeon@careup.com",
                    "카페/음료", LocalDate.of(2023, 9, 1),
                    35.1570, 129.0590, 300, "부산 핵심 상권, 회전율 높음"
            );

            Long gwanggyoId = ensureBranch(
                    "광교점", OwnershipType.YES, "113-30-00013", "333333-3000013",
                    "경기도 수원시 영통구 광교중앙로 248", "상가동 3층",
                    "031-500-0013", "gwanggyo@careup.com",
                    "카페/디저트", LocalDate.of(2024, 5, 1),
                    37.2880, 127.0560, 260, "신도시 주거/오피스 복합 상권"
            );
            Long dongseongnoId = ensureBranch(
                    "대구동성로점", OwnershipType.NO, "114-40-00014", "444444-4000014",
                    "대구광역시 중구 동성로 25", "1층",
                    "053-500-0014", "dongseongno@careup.com",
                    "카페/음료", LocalDate.of(2023, 10, 1),
                    35.8690, 128.5960, 280, "대구 중심 상권, 유동 인구 많음"
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

            createEmployee(
                    "O2025010", "임성후", "samsong.owner@careup.com", "010-1000-1010", Gender.MALE,
                    gradeIds.get("점장"),
                    AuthorityType.FRANCHISE_OWNER, EmploymentStatus.ACTIVE, EmploymentType.FULL_TIME,
                    "경기도 고양시 덕양구 삼송로 21", "101호", "10500",
                    "010-2000-1010", "임미정", Relationship.SPOUSE,
                    LocalDate.of(1985, 4, 3), LocalDate.of(2022, 3, 1),
                    DEFAULT_PROFILE_URL, "고양삼송점 가맹점주",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(samsongId).assignedFrom(LocalDate.of(2022,3,1))
                            .assignedTo(LocalDate.of(2032,3,1)).placementYn("N").build())
            );
            createEmployee(
                    "S2025011", "한예슬", "han.yeseul@careup.com", "010-1001-1111", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "경기도 고양시 덕양구 삼송로 25", "2층", "10501",
                    "010-3001-1111", "한지민", Relationship.SIBLING,
                    LocalDate.of(1993, 1, 1), LocalDate.of(2022, 3, 5),
                    DEFAULT_PROFILE_URL, "고양삼송점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(samsongId).assignedFrom(LocalDate.of(2022,3,5))
                            .assignedTo(LocalDate.of(2026,3,5)).placementYn("N").build())
            );
            createEmployee(
                    "S2025012", "고윤정", "ko.yunjung@careup.com", "010-1002-1212", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "경기도 고양시 덕양구 삼송로 27", "3층", "10502",
                    "010-3002-1212", "고다희", Relationship.PARENT,
                    LocalDate.of(1999, 9, 22), LocalDate.of(2022, 3, 5),
                    DEFAULT_PROFILE_URL, "고양삼송점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(samsongId).assignedFrom(LocalDate.of(2022,3,5))
                            .assignedTo(LocalDate.of(2026,3,5)).placementYn("N").build())
            );
            createEmployee(
                    "S2025013", "조보아", "cho.boah@careup.com", "010-1003-1313", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "경기도 고양시 덕양구 삼송로 29", "4층", "10503",
                    "010-3003-1313", "조지우", Relationship.FRIEND,
                    LocalDate.of(1995, 8, 22), LocalDate.of(2022, 3, 5),
                    DEFAULT_PROFILE_URL, "고양삼송점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(samsongId).assignedFrom(LocalDate.of(2022,3,5))
                            .assignedTo(LocalDate.of(2026,3,5)).placementYn("N").build())
            );

            createEmployee(
                    "B2025014", "김건동", "euljiro.mgr@careup.com", "010-2000-1414", Gender.MALE,
                    gradeIds.get("점장"),
                    AuthorityType.BRANCH_ADMIN, EmploymentStatus.ACTIVE, EmploymentType.FULL_TIME,
                    "서울특별시 중구 을지로 157", "201호", "04548",
                    "010-4000-1414", "김성우", Relationship.PARENT,
                    LocalDate.of(1987, 2, 2), LocalDate.of(2020, 6, 1),
                    DEFAULT_PROFILE_URL, "을지로점 직영 지점장",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(euljiroId).assignedFrom(LocalDate.of(2020,6,1))
                            .assignedTo(LocalDate.of(2030,6,1)).placementYn("N").build())
            );
            createEmployee(
                    "S2025015", "김찬진", "kim.chanjin@careup.com", "010-2001-1515", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 중구 을지로 160", "301호", "04549",
                    "010-5001-1515", "김하늘", Relationship.SIBLING,
                    LocalDate.of(1996, 5, 5), LocalDate.of(2020, 6, 3),
                    DEFAULT_PROFILE_URL, "을지로점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(euljiroId)
                            .assignedFrom(LocalDate.of(2020, 6, 3))
                            .assignedTo(LocalDate.of(2030, 6, 3))
                            .placementYn("N")
                            .build())
            );
            createEmployee(
                    "S2025016", "권수연", "kwon.suyeon@careup.com", "010-2002-1616", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 중구 을지로 162", "302호", "04549",
                    "010-5002-1616", "권민정", Relationship.PARENT,
                    LocalDate.of(1997, 3, 18), LocalDate.of(2020, 6, 3),
                    DEFAULT_PROFILE_URL, "을지로점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(euljiroId)
                            .assignedFrom(LocalDate.of(2020, 6, 3))
                            .assignedTo(LocalDate.of(2030, 6, 3))
                            .placementYn("N")
                            .build())
            );

            createEmployee(
                    "S2025017", "윤수오", "yoon.suoh@careup.com", "010-2003-1717", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 중구 을지로 164", "303호", "04549",
                    "010-5003-1717", "윤세라", Relationship.FRIEND,
                    LocalDate.of(1995, 12, 30), LocalDate.of(2020, 6, 3),
                    DEFAULT_PROFILE_URL, "을지로점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(euljiroId)
                            .assignedFrom(LocalDate.of(2020, 6, 3))
                            .assignedTo(LocalDate.of(2030, 6, 3))
                            .placementYn("N")
                            .build())
            );
            createEmployee(
                    "O2025018", "김선국", "sindaebang.owner@careup.com", "010-3000-1818", Gender.MALE,
                    gradeIds.get("점장"),
                    AuthorityType.FRANCHISE_OWNER, EmploymentStatus.ACTIVE, EmploymentType.FULL_TIME,
                    "서울특별시 동작구 보라매로 120", "101호", "07024",
                    "010-6000-1818", "김유정", Relationship.SPOUSE,
                    LocalDate.of(1982, 10, 1), LocalDate.of(2021, 9, 1),
                    DEFAULT_PROFILE_URL, "신대방삼거리점 가맹점주",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(sindaebangId).assignedFrom(LocalDate.of(2021,9,1))
                            .assignedTo(LocalDate.of(2031,9,1)).placementYn("N").build())
            );
            createEmployee(
                    "S2025019", "김형진", "kim.hyeongjin@careup.com", "010-3001-1919", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 동작구 보라매로 122", "2층", "07024",
                    "010-6001-1919", "김성훈", Relationship.PARENT,
                    LocalDate.of(2000, 1, 1), LocalDate.of(2021, 9, 3),
                    DEFAULT_PROFILE_URL, "신대방삼거리점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(sindaebangId).assignedFrom(LocalDate.of(2021,9,3))
                            .assignedTo(LocalDate.of(2026,9,3)).placementYn("N").build())
            );
            createEmployee(
                    "S2025020", "박혜성", "park.hyesung@careup.com", "010-3002-2020", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 동작구 보라매로 124", "3층", "07024",
                    "010-6002-2020", "박수연", Relationship.SIBLING,
                    LocalDate.of(1998, 6, 11), LocalDate.of(2021, 9, 3),
                    DEFAULT_PROFILE_URL, "신대방삼거리점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(sindaebangId).assignedFrom(LocalDate.of(2021,9,3))
                            .assignedTo(LocalDate.of(2026,9,3)).placementYn("N").build())
            );
            createEmployee(
                    "S2025021", "조은성", "cho.eunseong@careup.com", "010-3003-2121", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 동작구 보라매로 126", "4층", "07024",
                    "010-6003-2121", "조민수", Relationship.FRIEND,
                    LocalDate.of(1997, 3, 3), LocalDate.of(2021, 9, 3),
                    DEFAULT_PROFILE_URL, "신대방삼거리점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(sindaebangId).assignedFrom(LocalDate.of(2021,9,3))
                            .assignedTo(LocalDate.of(2026,9,3)).placementYn("N").build())
            );
            createEmployee(
                    "S2025022", "김영관", "kim.younggwan@careup.com", "010-3004-2222", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 동작구 보라매로 128", "5층", "07024",
                    "010-6004-2222", "김형민", Relationship.PARENT,
                    LocalDate.of(1996, 9, 9), LocalDate.of(2021, 9, 3),
                    DEFAULT_PROFILE_URL, "신대방삼거리점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(sindaebangId).assignedFrom(LocalDate.of(2021,9,3))
                            .assignedTo(LocalDate.of(2026,9,3)).placementYn("N").build())
            );
            createEmployee(
                    "S2025023", "김현지", "kim.hyunji@careup.com", "010-3005-2323", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 동작구 보라매로 130", "6층", "07024",
                    "010-6005-2323", "김다은", Relationship.SIBLING,
                    LocalDate.of(2001, 11, 1), LocalDate.of(2021, 9, 3),
                    DEFAULT_PROFILE_URL, "신대방삼거리점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(sindaebangId).assignedFrom(LocalDate.of(2021,9,3))
                            .assignedTo(LocalDate.of(2026,9,3)).placementYn("N").build())
            );

            createEmployee(
                    "B2025024", "김송옥", "gupabal.mgr@careup.com", "010-4000-2424", Gender.FEMALE,
                    gradeIds.get("점장"),
                    AuthorityType.BRANCH_ADMIN, EmploymentStatus.ACTIVE, EmploymentType.FULL_TIME,
                    "서울특별시 은평구 진관2로 29", "101호", "03381",
                    "010-7000-2424", "김소정", Relationship.PARENT,
                    LocalDate.of(1989, 8, 8), LocalDate.of(2022, 11, 1),
                    DEFAULT_PROFILE_URL, "구파발점 직영 지점장",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(gupabalId).assignedFrom(LocalDate.of(2022,11,1))
                            .assignedTo(LocalDate.of(2032,11,1)).placementYn("N").build())
            );
            createEmployee(
                    "S2025025", "김지현", "kim.jihyun@careup.com", "010-4001-2525", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 은평구 진관2로 31", "2층", "03381",
                    "010-7001-2525", "김태훈", Relationship.FRIEND,
                    LocalDate.of(1997, 7, 7), LocalDate.of(2022, 11, 3),
                    DEFAULT_PROFILE_URL, "구파발점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(gupabalId).assignedFrom(LocalDate.of(2022,11,3))
                            .assignedTo(LocalDate.of(2027,11,3)).placementYn("N").build())
            );
            createEmployee(
                    "S2025026", "정지완", "jung.jiwan@careup.com", "010-4002-2626", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 은평구 진관2로 33", "3층", "03381",
                    "010-7002-2626", "정다운", Relationship.PARENT,
                    LocalDate.of(1996, 3, 1), LocalDate.of(2022, 11, 3),
                    DEFAULT_PROFILE_URL, "구파발점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(gupabalId).assignedFrom(LocalDate.of(2022,11,3))
                            .assignedTo(LocalDate.of(2027,11,3)).placementYn("N").build())
            );
            createEmployee(
                    "S2025027", "위동길", "wi.donggil@careup.com", "010-4003-2727", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 은평구 진관2로 35", "4층", "03381",
                    "010-7003-2727", "위원석", Relationship.SIBLING,
                    LocalDate.of(1995, 4, 14), LocalDate.of(2022, 11, 3),
                    DEFAULT_PROFILE_URL, "구파발점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(gupabalId).assignedFrom(LocalDate.of(2022,11,3))
                            .assignedTo(LocalDate.of(2027,11,3)).placementYn("N").build())
            );
            createEmployee(
                    "S2025028", "최승휘", "choi.seunghwi@careup.com", "010-4004-2828", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 은평구 진관2로 37", "5층", "03381",
                    "010-7004-2828", "최도윤", Relationship.FRIEND,
                    LocalDate.of(1994, 12, 12), LocalDate.of(2022, 11, 3),
                    DEFAULT_PROFILE_URL, "구파발점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(gupabalId).assignedFrom(LocalDate.of(2022,11,3))
                            .assignedTo(LocalDate.of(2027,11,3)).placementYn("N").build())
            );

            createEmployee(
                    "B2025029", "박서준", "magok.mgr@careup.com", "010-5000-2929", Gender.MALE,
                    gradeIds.get("점장"),
                    AuthorityType.BRANCH_ADMIN, EmploymentStatus.ACTIVE, EmploymentType.FULL_TIME,
                    "서울특별시 강서구 마곡중앙로 150", "301호", "07550",
                    "010-8000-2929", "박민정", Relationship.SPOUSE,
                    LocalDate.of(1988, 3, 3), LocalDate.of(2023, 5, 1),
                    DEFAULT_PROFILE_URL, "마곡나루점 직영 지점장",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(magokId).assignedFrom(LocalDate.of(2023,5,1))
                            .assignedTo(LocalDate.of(2033,5,1)).placementYn("N").build())
            );
            createEmployee(
                    "S2025030", "이도현", "magok.staff1@careup.com", "010-5001-3030", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 강서구 공항대로 200", "1201호", "07551",
                    "010-8001-3030", "이수빈", Relationship.SIBLING,
                    LocalDate.of(1998, 2, 1), LocalDate.of(2023, 5, 3),
                    DEFAULT_PROFILE_URL, "마곡나루점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(magokId).assignedFrom(LocalDate.of(2023,5,3))
                            .assignedTo(LocalDate.of(2027,5,3)).placementYn("N").build())
            );
            createEmployee(
                    "S2025031", "한소희", "magok.staff2@careup.com", "010-5002-3131", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 강서구 마곡서로 80", "901호", "07552",
                    "010-8002-3131", "한서윤", Relationship.PARENT,
                    LocalDate.of(1997, 9, 9), LocalDate.of(2023, 5, 5),
                    DEFAULT_PROFILE_URL, "마곡나루점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(magokId).assignedFrom(LocalDate.of(2023,5,5))
                            .assignedTo(LocalDate.of(2027,5,5)).placementYn("N").build())
            );
            createEmployee(
                    "S2025032", "채수빈", "magok.staff3@careup.com", "010-5003-3232", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 강서구 양천로 400", "502호", "07553",
                    "010-8003-3232", "채지우", Relationship.FRIEND,
                    LocalDate.of(1996, 6, 6), LocalDate.of(2023, 5, 7),
                    DEFAULT_PROFILE_URL, "마곡나루점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(magokId).assignedFrom(LocalDate.of(2023,5,7))
                            .assignedTo(LocalDate.of(2027,5,7)).placementYn("N").build())
            );
            createEmployee(
                    "S2025033", "김태리", "magok.staff4@careup.com", "010-5004-3333", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 강서구 공항대로 210", "1502호", "07554",
                    "010-8004-3333", "김나연", Relationship.NEIGHBOR,
                    LocalDate.of(1995, 4, 20), LocalDate.of(2023, 5, 9),
                    DEFAULT_PROFILE_URL, "마곡나루점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(magokId).assignedFrom(LocalDate.of(2023,5,9))
                            .assignedTo(LocalDate.of(2027,5,9)).placementYn("N").build())
            );
            createEmployee(
                    "S2025054", "이성경", "magok.staff5@careup.com", "010-5005-5454", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 강서구 마곡중앙6로 70", "803호", "07555",
                    "010-8005-5454", "이서영", Relationship.SIBLING,
                    LocalDate.of(1994, 3, 3), LocalDate.of(2023, 5, 11),
                    DEFAULT_PROFILE_URL, "마곡나루점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(magokId).assignedFrom(LocalDate.of(2023,5,11))
                            .assignedTo(LocalDate.of(2027,5,11)).placementYn("N").build())
            );

            createEmployee(
                    "B2025034", "이준호", "seongsu.mgr@careup.com", "010-5100-3434", Gender.MALE,
                    gradeIds.get("점장"),
                    AuthorityType.BRANCH_ADMIN, EmploymentStatus.ACTIVE, EmploymentType.FULL_TIME,
                    "서울특별시 성동구 성수일로 10", "702호", "04794",
                    "010-8100-3434", "이서윤", Relationship.SPOUSE,
                    LocalDate.of(1989, 8, 8), LocalDate.of(2024, 4, 1),
                    DEFAULT_PROFILE_URL, "성수점 직영 지점장",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(seongsuId).assignedFrom(LocalDate.of(2024,4,1))
                            .assignedTo(LocalDate.of(2034,4,1)).placementYn("N").build())
            );
            createEmployee(
                    "S2025035", "박지후", "seongsu.staff1@careup.com", "010-5101-3535", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 성동구 성수이로 20", "301호", "04795",
                    "010-8101-3535", "박지현", Relationship.SIBLING,
                    LocalDate.of(1998, 5, 15), LocalDate.of(2024, 4, 3),
                    DEFAULT_PROFILE_URL, "성수점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(seongsuId).assignedFrom(LocalDate.of(2024,4,3))
                            .assignedTo(LocalDate.of(2028,4,3)).placementYn("N").build())
            );
            createEmployee(
                    "S2025036", "문가영", "seongsu.staff2@careup.com", "010-5102-3636", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 성동구 아차산로 17", "501호", "04796",
                    "010-8102-3636", "문미소", Relationship.PARENT,
                    LocalDate.of(1997, 7, 7), LocalDate.of(2024, 4, 5),
                    DEFAULT_PROFILE_URL, "성수점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(seongsuId).assignedFrom(LocalDate.of(2024,4,5))
                            .assignedTo(LocalDate.of(2028,4,5)).placementYn("N").build())
            );
            createEmployee(
                    "S2025037", "박은빈", "seongsu.staff3@careup.com", "010-5103-3737", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 성동구 성수이로 30", "902호", "04797",
                    "010-8103-3737", "박하늘", Relationship.FRIEND,
                    LocalDate.of(1996, 11, 1), LocalDate.of(2024, 4, 7),
                    DEFAULT_PROFILE_URL, "성수점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(seongsuId).assignedFrom(LocalDate.of(2024,4,7))
                            .assignedTo(LocalDate.of(2028,4,7)).placementYn("N").build())
            );
            createEmployee(
                    "S2025038", "위하준", "seongsu.staff4@careup.com", "010-5104-3838", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 성동구 연무장길 9", "402호", "04798",
                    "010-8104-3838", "위태훈", Relationship.NEIGHBOR,
                    LocalDate.of(1995, 4, 2), LocalDate.of(2024, 4, 9),
                    DEFAULT_PROFILE_URL, "성수점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(seongsuId).assignedFrom(LocalDate.of(2024,4,9))
                            .assignedTo(LocalDate.of(2028,4,9)).placementYn("N").build())
            );
            createEmployee(
                    "S2025055", "신예은", "seongsu.staff5@careup.com", "010-5105-5555", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 성동구 성수이로 18", "601호", "04795",
                    "010-8105-5555", "신가윤", Relationship.SIBLING,
                    LocalDate.of(1994, 9, 9), LocalDate.of(2024, 4, 11),
                    DEFAULT_PROFILE_URL, "성수점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(seongsuId).assignedFrom(LocalDate.of(2024,4,11))
                            .assignedTo(LocalDate.of(2028,4,11)).placementYn("N").build())
            );

            createEmployee(
                    "O2025039", "김민재", "jamsil.owner@careup.com", "010-5200-3939", Gender.MALE,
                    gradeIds.get("점장"),
                    AuthorityType.FRANCHISE_OWNER, EmploymentStatus.ACTIVE, EmploymentType.FULL_TIME,
                    "서울특별시 송파구 올림픽로 240", "2층", "05554",
                    "010-8200-3939", "김서연", Relationship.SPOUSE,
                    LocalDate.of(1986, 12, 12), LocalDate.of(2022, 8, 1),
                    DEFAULT_PROFILE_URL, "잠실점 가맹점주",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(jamsilId).assignedFrom(LocalDate.of(2022,8,1))
                            .assignedTo(LocalDate.of(2032,8,1)).placementYn("N").build())
            );
            createEmployee(
                    "S2025040", "신혜선", "jamsil.staff1@careup.com", "010-5201-4040", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 송파구 올림픽로 250", "1201호", "05555",
                    "010-8201-4040", "신다은", Relationship.SIBLING,
                    LocalDate.of(1998, 1, 10), LocalDate.of(2022, 8, 3),
                    DEFAULT_PROFILE_URL, "잠실점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(jamsilId).assignedFrom(LocalDate.of(2022,8,3))
                            .assignedTo(LocalDate.of(2026,8,3)).placementYn("N").build())
            );
            createEmployee(
                    "S2025041", "채정안", "jamsil.staff2@careup.com", "010-5202-4141", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 송파구 올림픽로 255", "901호", "05556",
                    "010-8202-4141", "채나래", Relationship.PARENT,
                    LocalDate.of(1997, 3, 3), LocalDate.of(2022, 8, 5),
                    DEFAULT_PROFILE_URL, "잠실점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(jamsilId).assignedFrom(LocalDate.of(2022,8,5))
                            .assignedTo(LocalDate.of(2026,8,5)).placementYn("N").build())
            );
            createEmployee(
                    "S2025042", "류혜영", "jamsil.staff3@careup.com", "010-5203-4242", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 송파구 올림픽로 260", "701호", "05557",
                    "010-8203-4242", "류세라", Relationship.FRIEND,
                    LocalDate.of(1996, 6, 1), LocalDate.of(2022, 8, 7),
                    DEFAULT_PROFILE_URL, "잠실점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(jamsilId).assignedFrom(LocalDate.of(2022,8,7))
                            .assignedTo(LocalDate.of(2026,8,7)).placementYn("N").build())
            );
            createEmployee(
                    "S2025043", "이상윤", "jamsil.staff4@careup.com", "010-5204-4343", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 송파구 잠실로 10", "401호", "05558",
                    "010-8204-4343", "이하늘", Relationship.NEIGHBOR,
                    LocalDate.of(1995, 11, 1), LocalDate.of(2022, 8, 9),
                    DEFAULT_PROFILE_URL, "잠실점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(jamsilId).assignedFrom(LocalDate.of(2022,8,9))
                            .assignedTo(LocalDate.of(2026,8,9)).placementYn("N").build())
            );
            createEmployee(
                    "S2025056", "김유정", "jamsil.staff5@careup.com", "010-5205-5656", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "서울특별시 송파구 올림픽로 245", "1501호", "05555",
                    "010-8205-5656", "김유나", Relationship.SIBLING,
                    LocalDate.of(1994, 4, 4), LocalDate.of(2022, 8, 11),
                    DEFAULT_PROFILE_URL, "잠실점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(jamsilId).assignedFrom(LocalDate.of(2022,8,11))
                            .assignedTo(LocalDate.of(2026,8,11)).placementYn("N").build())
            );

            createEmployee(
                    "O2025044", "박형식", "pangyo.owner@careup.com", "010-5300-4444", Gender.MALE,
                    gradeIds.get("점장"),
                    AuthorityType.FRANCHISE_OWNER, EmploymentStatus.ACTIVE, EmploymentType.FULL_TIME,
                    "경기도 성남시 분당구 판교역로 145", "B동 101호", "13494",
                    "010-8300-4444", "박예린", Relationship.SPOUSE,
                    LocalDate.of(1987, 7, 7), LocalDate.of(2024, 3, 1),
                    DEFAULT_PROFILE_URL, "판교점 가맹점주",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(pangyoId).assignedFrom(LocalDate.of(2024,3,1))
                            .assignedTo(LocalDate.of(2034,3,1)).placementYn("N").build())
            );
            createEmployee(
                    "S2025045", "이세영", "pangyo.staff1@careup.com", "010-5301-4545", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "경기도 성남시 분당구 대왕판교로 660", "501호", "13487",
                    "010-8301-4545", "이수진", Relationship.SIBLING,
                    LocalDate.of(1998, 8, 8), LocalDate.of(2024, 3, 3),
                    DEFAULT_PROFILE_URL, "판교점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(pangyoId).assignedFrom(LocalDate.of(2024,3,3))
                            .assignedTo(LocalDate.of(2028,3,3)).placementYn("N").build())
            );
            createEmployee(
                    "S2025046", "남지현", "pangyo.staff2@careup.com", "010-5302-4646", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "경기도 성남시 분당구 판교로 255", "902호", "13529",
                    "010-8302-4646", "남윤아", Relationship.PARENT,
                    LocalDate.of(1997, 2, 2), LocalDate.of(2024, 3, 5),
                    DEFAULT_PROFILE_URL, "판교점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(pangyoId).assignedFrom(LocalDate.of(2024,3,5))
                            .assignedTo(LocalDate.of(2028,3,5)).placementYn("N").build())
            );
            createEmployee(
                    "S2025047", "박보검", "pangyo.staff3@careup.com", "010-5303-4747", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "경기도 성남시 분당구 판교로 235", "701호", "13524",
                    "010-8303-4747", "박주환", Relationship.FRIEND,
                    LocalDate.of(1996, 5, 5), LocalDate.of(2024, 3, 7),
                    DEFAULT_PROFILE_URL, "판교점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(pangyoId).assignedFrom(LocalDate.of(2024,3,7))
                            .assignedTo(LocalDate.of(2028,3,7)).placementYn("N").build())
            );
            createEmployee(
                    "S2025048", "김지원", "pangyo.staff4@careup.com", "010-5304-4848", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "경기도 성남시 분당구 정자일로 95", "401호", "13560",
                    "010-8304-4848", "김다빈", Relationship.NEIGHBOR,
                    LocalDate.of(1995, 1, 1), LocalDate.of(2024, 3, 9),
                    DEFAULT_PROFILE_URL, "판교점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(pangyoId).assignedFrom(LocalDate.of(2024,3,9))
                            .assignedTo(LocalDate.of(2028,3,9)).placementYn("N").build())
            );
            createEmployee(
                    "S2025057", "김세정", "pangyo.staff5@careup.com", "010-5305-5757", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "경기도 성남시 분당구 판교역로 191", "1203호", "13496",
                    "010-8305-5757", "김세린", Relationship.SIBLING,
                    LocalDate.of(1994, 7, 7), LocalDate.of(2024, 3, 11),
                    DEFAULT_PROFILE_URL, "판교점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(pangyoId).assignedFrom(LocalDate.of(2024,3,11))
                            .assignedTo(LocalDate.of(2028,3,11)).placementYn("N").build())
            );

            createEmployee(
                    "B2025049", "손석구", "seomyeon.mgr@careup.com", "010-5400-4949", Gender.MALE,
                    gradeIds.get("점장"),
                    AuthorityType.BRANCH_ADMIN, EmploymentStatus.ACTIVE, EmploymentType.FULL_TIME,
                    "부산광역시 부산진구 중앙대로 692", "2층", "47260",
                    "010-8400-4949", "손유정", Relationship.SPOUSE,
                    LocalDate.of(1985, 9, 9), LocalDate.of(2023, 9, 1),
                    DEFAULT_PROFILE_URL, "부산서면점 직영 지점장",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(seomyeonId).assignedFrom(LocalDate.of(2023,9,1))
                            .assignedTo(LocalDate.of(2033,9,1)).placementYn("N").build())
            );
            createEmployee(
                    "S2025050", "이세돌", "seomyeon.staff1@careup.com", "010-5401-5050", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "부산광역시 부산진구 중앙대로 700", "1101호", "47261",
                    "010-8401-5050", "이세윤", Relationship.SIBLING,
                    LocalDate.of(1998, 10, 10), LocalDate.of(2023, 9, 3),
                    DEFAULT_PROFILE_URL, "부산서면점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(seomyeonId).assignedFrom(LocalDate.of(2023,9,3))
                            .assignedTo(LocalDate.of(2027,9,3)).placementYn("N").build())
            );
            createEmployee(
                    "S2025051", "정해인", "seomyeon.staff2@careup.com", "010-5402-5151", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "부산광역시 부산진구 중앙대로 710", "901호", "47262",
                    "010-8402-5151", "정하늘", Relationship.PARENT,
                    LocalDate.of(1997, 12, 12), LocalDate.of(2023, 9, 5),
                    DEFAULT_PROFILE_URL, "부산서면점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(seomyeonId).assignedFrom(LocalDate.of(2023,9,5))
                            .assignedTo(LocalDate.of(2027,9,5)).placementYn("N").build())
            );
            createEmployee(
                    "S2025052", "김다미", "seomyeon.staff3@careup.com", "010-5403-5252", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "부산광역시 부산진구 중앙대로 720", "701호", "47263",
                    "010-8403-5252", "김다연", Relationship.FRIEND,
                    LocalDate.of(1996, 2, 2), LocalDate.of(2023, 9, 7),
                    DEFAULT_PROFILE_URL, "부산서면점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(seomyeonId).assignedFrom(LocalDate.of(2023,9,7))
                            .assignedTo(LocalDate.of(2027,9,7)).placementYn("N").build())
            );
            createEmployee(
                    "S2025053", "신민아", "seomyeon.staff4@careup.com", "010-5404-5353", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "부산광역시 부산진구 중앙대로 730", "601호", "47264",
                    "010-8404-5353", "신유리", Relationship.NEIGHBOR,
                    LocalDate.of(1995, 3, 3), LocalDate.of(2023, 9, 9),
                    DEFAULT_PROFILE_URL, "부산서면점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(seomyeonId).assignedFrom(LocalDate.of(2023,9,9))
                            .assignedTo(LocalDate.of(2027,9,9)).placementYn("N").build())
            );
            createEmployee(
                    "S2025058", "박혜수", "seomyeon.staff5@careup.com", "010-5405-5858", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "부산광역시 부산진구 중앙대로 740", "701호", "47265",
                    "010-8405-5858", "박혜림", Relationship.SIBLING,
                    LocalDate.of(1994, 6, 6), LocalDate.of(2023, 9, 11),
                    DEFAULT_PROFILE_URL, "부산서면점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(seomyeonId).assignedFrom(LocalDate.of(2023,9,11))
                            .assignedTo(LocalDate.of(2027,9,11)).placementYn("N").build())
            );

            createEmployee(
                    "O2025059", "이광수", "gwanggyo.owner@careup.com", "010-5500-5959", Gender.MALE,
                    gradeIds.get("점장"),
                    AuthorityType.FRANCHISE_OWNER, EmploymentStatus.ACTIVE, EmploymentType.FULL_TIME,
                    "경기도 수원시 영통구 광교중앙로 248", "3층", "16514",
                    "010-8500-5959", "이다은", Relationship.SPOUSE,
                    LocalDate.of(1986, 5, 5), LocalDate.of(2024, 5, 1),
                    DEFAULT_PROFILE_URL, "광교점 가맹점주",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(gwanggyoId).assignedFrom(LocalDate.of(2024,5,1))
                            .assignedTo(LocalDate.of(2034,5,1)).placementYn("N").build())
            );
            createEmployee(
                    "S2025060", "정연우", "gwanggyo.staff1@careup.com", "010-5501-6060", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "경기도 수원시 영통구 대학3로 25", "501호", "16491",
                    "010-8501-6060", "정가윤", Relationship.SIBLING,
                    LocalDate.of(1998, 3, 3), LocalDate.of(2024, 5, 3),
                    DEFAULT_PROFILE_URL, "광교점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(gwanggyoId).assignedFrom(LocalDate.of(2024,5,3))
                            .assignedTo(LocalDate.of(2028,5,3)).placementYn("N").build())
            );
            createEmployee(
                    "S2025061", "김보라", "gwanggyo.staff2@careup.com", "010-5502-6161", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "경기도 수원시 영통구 센트럴타운로 31", "901호", "16506",
                    "010-8502-6161", "김보민", Relationship.PARENT,
                    LocalDate.of(1997, 7, 7), LocalDate.of(2024, 5, 5),
                    DEFAULT_PROFILE_URL, "광교점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(gwanggyoId).assignedFrom(LocalDate.of(2024,5,5))
                            .assignedTo(LocalDate.of(2028,5,5)).placementYn("N").build())
            );
            createEmployee(
                    "S2025062", "하승진", "gwanggyo.staff3@careup.com", "010-5503-6262", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "경기도 수원시 영통구 법조로 25", "701호", "16514",
                    "010-8503-6262", "하승우", Relationship.FRIEND,
                    LocalDate.of(1996, 6, 6), LocalDate.of(2024, 5, 7),
                    DEFAULT_PROFILE_URL, "광교점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(gwanggyoId).assignedFrom(LocalDate.of(2024,5,7))
                            .assignedTo(LocalDate.of(2028,5,7)).placementYn("N").build())
            );
            createEmployee(
                    "S2025063", "노정의", "gwanggyo.staff4@careup.com", "010-5504-6363", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "경기도 수원시 영통구 광교로 124", "1102호", "16514",
                    "010-8504-6363", "노지우", Relationship.NEIGHBOR,
                    LocalDate.of(1995, 1, 1), LocalDate.of(2024, 5, 9),
                    DEFAULT_PROFILE_URL, "광교점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(gwanggyoId).assignedFrom(LocalDate.of(2024,5,9))
                            .assignedTo(LocalDate.of(2028,5,9)).placementYn("N").build())
            );
            createEmployee(
                    "S2025064", "김도연", "gwanggyo.staff5@careup.com", "010-5505-6464", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "경기도 수원시 영통구 광교호수로 30", "601호", "16514",
                    "010-8505-6464", "김도현", Relationship.SIBLING,
                    LocalDate.of(1994, 9, 9), LocalDate.of(2024, 5, 11),
                    DEFAULT_PROFILE_URL, "광교점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(gwanggyoId).assignedFrom(LocalDate.of(2024,5,11))
                            .assignedTo(LocalDate.of(2028,5,11)).placementYn("N").build())
            );

            createEmployee(
                    "B2025065", "마동석", "dongseongno.mgr@careup.com", "010-5600-6565", Gender.MALE,
                    gradeIds.get("점장"),
                    AuthorityType.BRANCH_ADMIN, EmploymentStatus.ACTIVE, EmploymentType.FULL_TIME,
                    "대구광역시 중구 동성로 25", "1층", "41917",
                    "010-8600-6565", "마지우", Relationship.SPOUSE,
                    LocalDate.of(1984, 4, 4), LocalDate.of(2023, 10, 1),
                    DEFAULT_PROFILE_URL, "대구동성로점 직영 지점장",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(dongseongnoId).assignedFrom(LocalDate.of(2023,10,1))
                            .assignedTo(LocalDate.of(2033,10,1)).placementYn("N").build())
            );
            createEmployee(
                    "S2025066", "이준기", "dongseongno.staff1@careup.com", "010-5601-6666", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "대구광역시 중구 국채보상로 585", "901호", "41919",
                    "010-8601-6666", "이주하", Relationship.SIBLING,
                    LocalDate.of(1998, 8, 8), LocalDate.of(2023, 10, 3),
                    DEFAULT_PROFILE_URL, "대구동성로점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(dongseongnoId).assignedFrom(LocalDate.of(2023,10,3))
                            .assignedTo(LocalDate.of(2027,10,3)).placementYn("N").build())
            );
            createEmployee(
                    "S2025067", "문채원", "dongseongno.staff2@careup.com", "010-5602-6767", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "대구광역시 중구 달구벌대로 2074", "701호", "41919",
                    "010-8602-6767", "문지안", Relationship.PARENT,
                    LocalDate.of(1997, 7, 7), LocalDate.of(2023, 10, 5),
                    DEFAULT_PROFILE_URL, "대구동성로점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(dongseongnoId).assignedFrom(LocalDate.of(2023,10,5))
                            .assignedTo(LocalDate.of(2027,10,5)).placementYn("N").build())
            );
            createEmployee(
                    "S2025068", "강하늘", "dongseongno.staff3@careup.com", "010-5603-6868", Gender.MALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "대구광역시 중구 중앙대로 366", "601호", "41937",
                    "010-8603-6868", "강민지", Relationship.FRIEND,
                    LocalDate.of(1996, 6, 6), LocalDate.of(2023, 10, 7),
                    DEFAULT_PROFILE_URL, "대구동성로점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(dongseongnoId).assignedFrom(LocalDate.of(2023,10,7))
                            .assignedTo(LocalDate.of(2027,10,7)).placementYn("N").build())
            );
            createEmployee(
                    "S2025069", "김유빈", "dongseongno.staff4@careup.com", "010-5604-6969", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "대구광역시 중구 동성로2길 81", "401호", "41918",
                    "010-8604-6969", "김유나", Relationship.NEIGHBOR,
                    LocalDate.of(1995, 5, 5), LocalDate.of(2023, 10, 9),
                    DEFAULT_PROFILE_URL, "대구동성로점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(dongseongnoId).assignedFrom(LocalDate.of(2023,10,9))
                            .assignedTo(LocalDate.of(2027,10,9)).placementYn("N").build())
            );
            createEmployee(
                    "S2025070", "한지민", "dongseongno.staff5@careup.com", "010-5605-7070", Gender.FEMALE,
                    gradeIds.get("바리스타"),
                    AuthorityType.STAFF, EmploymentStatus.ACTIVE, EmploymentType.PART_TIME,
                    "대구광역시 중구 동성로 12", "301호", "41918",
                    "010-8605-7070", "한지원", Relationship.SIBLING,
                    LocalDate.of(1994, 4, 4), LocalDate.of(2023, 10, 11),
                    DEFAULT_PROFILE_URL, "대구동성로점 직원",
                    List.of(DispatchAssignmentDto.builder()
                            .branchId(dongseongnoId).assignedFrom(LocalDate.of(2023,10,11))
                            .assignedTo(LocalDate.of(2027,10,11)).placementYn("N").build())
            );

            // ===== 모든 지점에 일반 직원 5명씩 자동 생성 (비고 한글화 적용) =====
            Long baristaGradeId = gradeIds.get("바리스타");

            addFiveStaff(hqId,         "hq",          "본점",
                    "서울특별시 중구 을지로 100", "본관 15층", "04550", LocalDate.of(2024,10,1), baristaGradeId);
            addFiveStaff(dongjakId,    "dongjak",     "동작점",
                    "서울특별시 동작구 상도로 22", "302호", "06970", LocalDate.of(2024,10,1), baristaGradeId);
            addFiveStaff(boramaeId,    "boramae",     "보라매점",
                    "서울특별시 동작구 보라매로 30", "상가동 1층", "07060", LocalDate.of(2024,10,1), baristaGradeId);
            addFiveStaff(samsongId,    "samsong",     "고양삼송점",
                    "경기도 고양시 덕양구 삼송로 21", "101호", "10500", LocalDate.of(2024,10,1), baristaGradeId);
            addFiveStaff(euljiroId,    "euljiro",     "을지로점",
                    "서울특별시 중구 을지로 160", "301호", "04549", LocalDate.of(2024,10,1), baristaGradeId);
            addFiveStaff(sindaebangId, "sindaebang",  "신대방삼거리점",
                    "서울특별시 동작구 보라매로 122", "2층", "07024", LocalDate.of(2024,10,1), baristaGradeId);
            addFiveStaff(gupabalId,    "gupabal",     "구파발점",
                    "서울특별시 은평구 진관2로 31", "2층", "03381", LocalDate.of(2024,10,1), baristaGradeId);
            addFiveStaff(magokId,      "magok",       "마곡나루점",
                    "서울특별시 강서구 공항대로 200", "1201호", "07551", LocalDate.of(2024,10,1), baristaGradeId);
            addFiveStaff(seongsuId,    "seongsu",     "성수점",
                    "서울특별시 성동구 성수이로 20", "301호", "04795", LocalDate.of(2024,10,1), baristaGradeId);
            addFiveStaff(jamsilId,     "jamsil",      "잠실점",
                    "서울특별시 송파구 올림픽로 250", "1201호", "05555", LocalDate.of(2024,10,1), baristaGradeId);
            addFiveStaff(pangyoId,     "pangyo",      "판교점",
                    "경기도 성남시 분당구 대왕판교로 660", "501호", "13487", LocalDate.of(2024,10,1), baristaGradeId);
            addFiveStaff(seomyeonId,   "seomyeon",    "부산서면점",
                    "부산광역시 부산진구 중앙대로 700", "1101호", "47261", LocalDate.of(2024,10,1), baristaGradeId);
            addFiveStaff(gwanggyoId,   "gwanggyo",    "광교점",
                    "경기도 수원시 영통구 센트럴타운로 31", "901호", "16506", LocalDate.of(2024,10,1), baristaGradeId);
            addFiveStaff(dongseongnoId,"dongseongno", "대구동성로점",
                    "대구광역시 중구 국채보상로 585", "901호", "41919", LocalDate.of(2024,10,1), baristaGradeId);
            // ===== 끝 =====

            // 사전코드(근무종류/휴가종류)만 보강
            ensureWorkTypes(List.of("일반근무", "야간근무", "재택근무", "외근"));
            ensureLeaveTypes(List.of("연차", "무급휴가", "특별휴가"));

            // 근태 템플릿 보강
            ensureAttendanceTemplates();
        });
    }

    private void runAsSystem(Runnable task) {
        Claims claims = Jwts.claims().setSubject("0");
        claims.put("role", "HQ_ADMIN");
        claims.put("employeeId", 0L);

        var auth = new UsernamePasswordAuthenticationToken(
                "0", null, List.of(new SimpleGrantedAuthority("ROLE_HQ_ADMIN"))
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
        Map<String, AuthorityType> map = new LinkedHashMap<>();
        map.put("바리스타", AuthorityType.STAFF);
        map.put("시프트 슈퍼바이저", AuthorityType.STAFF);
        map.put("부점장", AuthorityType.BRANCH_ADMIN);
        map.put("점장", AuthorityType.BRANCH_ADMIN);
        map.put("지역매니저", AuthorityType.BRANCH_ADMIN);
        map.put("본사매니저", AuthorityType.HQ_ADMIN);

        Map<String, Long> result = new LinkedHashMap<>();
        for (String n : names) {
            AuthorityType at = map.getOrDefault(n, AuthorityType.STAFF);
            jobGradeRepository.findByName(n).ifPresentOrElse(
                    j -> {
                        if (j.getAuthorityType() != at) {
                            JobGradeListDto updated = jobGradeService.update(
                                    j.getId(),
                                    JobGradeUpdateDto.builder().name(n).authorityType(at).build()
                            );
                            result.put(n, updated.getId());
                        } else {
                            result.put(n, j.getId());
                        }
                    },
                    () -> {
                        JobGradeListDto created = jobGradeService.create(
                                JobGradeCreateDto.builder().name(n).authorityType(at).build()
                        );
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
                                List<DispatchAssignmentDto> dispatches
    ) {

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

    private void ensureAttendanceTemplates() {
        var all = attendanceTemplateRepository.findAll();

        boolean hasNormal = all.stream().anyMatch(t -> "일반 근무".equals(t.getName()));
        if (!hasNormal) {
            attendanceTemplateRepository.save(
                    AttendanceTemplate.builder()
                            .name("일반 근무")
                            .defaultClockIn(LocalTime.of(9, 0))
                            .defaultBreakStart(LocalTime.of(12, 0))
                            .defaultBreakEnd(LocalTime.of(13, 0))
                            .defaultClockOut(LocalTime.of(18, 0))
                            .build()
            );
        }

        boolean hasNight = all.stream().anyMatch(t -> "야간 근무".equals(t.getName()));
        if (!hasNight) {
            attendanceTemplateRepository.save(
                    AttendanceTemplate.builder()
                            .name("야간 근무")
                            .defaultClockIn(LocalTime.of(21, 0))
                            .defaultBreakStart(LocalTime.of(0, 0))  // 익일 00:00
                            .defaultBreakEnd(LocalTime.of(1, 0))    // 익일 01:00
                            .defaultClockOut(LocalTime.of(6, 0))    // 익일 06:00
                            .build()
            );
        }
    }
}
