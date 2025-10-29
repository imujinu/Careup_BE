package com.careup.branch.domain.chat.service;

import com.careup.branch.common.client.ChatFeignClient;
import com.careup.branch.common.client.OrderingInventoryClient;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.chat.dto.SalesStatisticsDto;
import com.careup.branch.domain.chat.dto.attendance.*;
import com.careup.branch.domain.chat.dto.sales.*;
import com.careup.branch.domain.employee.controller.ScheduleController;
import com.careup.branch.domain.employee.dto.request.ScheduleUpdateDto;
import com.careup.branch.domain.employee.dto.response.*;
import com.careup.branch.domain.employee.entity.AuthorityType;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.*;
import com.careup.branch.domain.employee.service.ScheduleService;
import com.careup.branch.domain.purchaseOrder.controller.PurchaseOrderController;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderRequestDto;
import com.careup.branch.domain.purchaseOrder.service.PurchaseOrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ChatUserService {
    private final ObjectMapper objectMapper;
    private final BranchRepository branchRepository;
    private final ChatFeignClient client;
    private final PurchaseOrderService purchaseOrderService;
    private final EmployeeRepository employeeRepository;
    private final ScheduleController scheduleController;
    private final ScheduleService scheduleService;
    private final PurchaseOrderController purchaseOrderController;
    private final OrderingInventoryClient orderingInventoryClient;
    private final AttendanceTemplateRepository attendanceTemplateRepository;
    private final DispatchStatusRepository dispatchStatusRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final WorkTypeRepository workTypeRepository;
    private final ChatFeignClient chatFeignClient;
    @Autowired
    @Qualifier("attendanceSuggestionClient")
    private ChatClient attendanceSuggestionClient;

    @Autowired
    @Qualifier("inventoryAdvisorClient")
    private ChatClient inventoryAdvisorClient;

    // [ 인건비 분석 모델 ]
    @Autowired
    @Qualifier("salesLaborAnalysisClient")
    private ChatClient salesLaborAnalysisClient;

    // [ 매출 보고 모델 ]
    @Autowired
    @Qualifier("salesReportClient")
    private ChatClient salesReportClient;
    // [근태 서비스 ]

    public ResponseEntity<?> handleAttendanceAction(String action, JSONObject params, Long branchId) {
        LocalDate startDate = null;
        LocalDate endDate = null;
        if(branchId==null){

        branchId = getBranchIdFromToken();
        }

        System.out.println("branchId ===" + branchId);
        if (!params.has("range")) {
            LocalDate now = LocalDate.now(ZoneId.of("Asia/Seoul"));
            startDate = now.withDayOfMonth(1);
            endDate = now.withDayOfMonth(now.lengthOfMonth());
        }else{
            JSONObject range = params.optJSONObject("range");
            startDate = LocalDate.parse(range.optString("start", null));
            endDate = LocalDate.parse(range.optString("end", null));
        }

        String date = params.optString("date", null);
        Branch branch = branchRepository.findById(branchId).orElseThrow(()->new EntityNotFoundException("존재하지 않는 지점입니다."));
        String periodType = params.optString("periodType", null);
        System.out.println(periodType);

        switch (action) {
            case "GET" -> {
                //금일 근태 조회
                if("DAY".equals(periodType)){
                    LocalDate today = LocalDate.parse(date);
                    List<ScheduleListDto> dto = scheduleService.listAll(today,today);
                    TodayAttendanceResDto resultDto = TodayAttendanceResDto.makeDto(dto, branch.getName());
                    System.out.println(resultDto);
                    return ResponseEntity.ok(resultDto);
                }
                //전체 근태 조회
                else if (params.has("allAttendance")) {
                    List<ScheduleListDto> list = scheduleService.listAll(startDate, endDate);
                    AttendanceCompareResDto resultDto = AttendanceCompareResDto.makeDto(list, branch.getName(), startDate,endDate);
                    List<AttendanceTemplateListDto> template = attendanceTemplateRepository.findAll().stream()
                            .map(AttendanceTemplateListDto::fromEntity)
                            .collect(Collectors.toList());
                    List<LeaveTypeDetailDto> leaveDtos = leaveTypeRepository.findAll().stream().map(LeaveTypeDetailDto::fromEntity).collect(Collectors.toList());
                    List<WorkTypeDetailDto> workDtos = workTypeRepository.findAll().stream().map(WorkTypeDetailDto::fromEntity).collect(Collectors.toList());
                    AttendanceAllResDto response = AttendanceAllResDto.builder()
                            .attendance(resultDto)
                            .templates(template)
                            .leaveTypes(leaveDtos)
                            .workTypes(workDtos)
                            .build();

                    return ResponseEntity.ok(response);
                }
                //특정 직원 근태 조회
                else if (params.has("employees")){

                    List<String> employees = new ArrayList<>();

                    JSONArray arr = params.getJSONArray("employees");
                    for (int i = 0; i < arr.length(); i++) {
                        employees.add(arr.getString(i));
                    }
                    List<ScheduleListDto> list  = scheduleService.listAll(startDate,endDate)
                            .stream()
                            .filter(s -> employees.stream().anyMatch(name -> s.getEmployeeName().contains(name)))
                            .collect(Collectors.toList());
                    AttendanceCompareResDto resultDto = AttendanceCompareResDto.makeDto(list, branch.getName(), startDate,endDate);
                    return ResponseEntity.ok(resultDto);
                }
                else{
                    throw new IllegalArgumentException("지원하지 않는 action: " + action);
                }

            }
            //[ 근태 수정 ]
            case "PATCH" ->{
                ScheduleUpdateReq req = null;
                try {
                    req = objectMapper.readValue(params.toString(), ScheduleUpdateReq.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                if(req.getBranchId()==null){
                    req.setBranchId(branchId);
                }
                System.out.println("req===" + req.toString());
                scheduleService.updateSchedule(
                        req.getScheduleId(),
                        ScheduleUpdateDto.makeDto(req)
                );
                return ResponseEntity.ok("근태 수정 완료");

            }
            case "CALCULATE" ->{
                ZoneId zone = ZoneId.of("Asia/Seoul");
                LocalDate today = LocalDate.now(zone);
                LocalDate targetDate = today.plusDays(1);
                LocalDate preWeek = today.minusWeeks(1).with(DayOfWeek.MONDAY);
                LocalDate preWeek2 = today.minusWeeks(1).with(DayOfWeek.SUNDAY);

                System.out.println("전주" +preWeek + preWeek2);
                // ✅ 1️⃣ 전주 시간대별 매출 조회
                CommonSuccessDto salesResponse = client.getSalesStatistics(branchId, today.minusWeeks(1).with(DayOfWeek.MONDAY), today.minusWeeks(1).with(DayOfWeek.SUNDAY), "HOUR");
                SalesStatisticsResponseDto sales = objectMapper.convertValue(salesResponse.getResult(),SalesStatisticsResponseDto.class);
                List<SalesStatisticsDto> stats = Optional.ofNullable(sales.getStatistics())
                                        .orElse(Collections.emptyList());

                CommonSuccessDto response = client.getSalesStatistics(branchId, today, today, "HOUR");
                SalesStatisticsResponseDto salesDto = objectMapper.convertValue(salesResponse.getResult(),SalesStatisticsResponseDto.class);
                List<SalesStatisticsDto> todaySales = Optional.ofNullable(sales.getStatistics())
                        .orElse(Collections.emptyList());

                 String hourlySummary = stats.stream()
                 .sorted(Comparator.comparing(SalesStatisticsDto::getHour, Comparator.nullsLast(Integer::compareTo)))
                 .map(stat -> String.format(
                 "%02d시~%02d시 | 매출: %,d원 (%d건, 평균 주문가 %,d원)",
                  stat.getHour(),
                  stat.getHour() + 1,
                  stat.getTotalSales(),
                  stat.getTotalOrders(), stat.getAverageOrderAmount()
                                        ))
                                        .collect(Collectors.joining("\n"));

                                System.out.println("전주 시간대별 매출 ======" + hourlySummary);
                String todaySummary = todaySales.stream()
                        .sorted(Comparator.comparing(SalesStatisticsDto::getHour, Comparator.nullsLast(Integer::compareTo)))
                        .map(stat -> String.format(
                                "%02d시~%02d시 | 매출: %,d원 (%d건, 평균 주문가 %,d원)",
                                stat.getHour(),
                                stat.getHour() + 1,
                                stat.getTotalSales(),
                                stat.getTotalOrders(), stat.getAverageOrderAmount()
                        ))
                        .collect(Collectors.joining("\n"));


                // ✅ 2️⃣ 전주 근무 스케줄 조회
                                List<ScheduleListDto> lastWeekSchedules =
                                        scheduleService.listAll(today.minusWeeks(1).with(DayOfWeek.MONDAY),
                                                today.minusWeeks(1).with(DayOfWeek.SUNDAY));
                                for(ScheduleListDto sc : lastWeekSchedules){
                                    System.out.println(sc);
                                }

                // ✅ 3️⃣ 근무 템플릿 조회
                                List<AttendanceTemplateListDto> template =
                                        attendanceTemplateRepository.findAll().stream()
                                                .map(AttendanceTemplateListDto::fromEntity)
                                                .collect(Collectors.toList());
                                for(AttendanceTemplateListDto at : template){
                                    System.out.println("at===" + at);
                                }
                                System.out.println("근무 템플릿 ======" + template);
                // ✅ 근무 템플릿 조회

                List<LeaveTypeDetailDto> leaveDtos = leaveTypeRepository.findAll().stream().map(LeaveTypeDetailDto::fromEntity).collect(Collectors.toList());
                List<WorkTypeDetailDto> workDtos = workTypeRepository.findAll().stream().map(WorkTypeDetailDto::fromEntity).collect(Collectors.toList());

                List<DispatchStatus> dispatchStatuses = dispatchStatusRepository.findAllByBranch(branch);
                List<EmployeeDetailDto> employees = new ArrayList<>();

                for(DispatchStatus ds : dispatchStatuses){
                    employees.add(new EmployeeDetailDto().fromEntity(ds.getEmployee()));
                }
                // ✅ 직원 리스트 포맷
                String employeeSummary = employees.stream()
                        .map(e -> String.format("{id:%d, name:'%s', position:'%s'}",
                                e.getId(), e.getName(), e.getJobGradeName()))
                        .collect(Collectors.joining(",\n"));

                // ✅ 스케줄 포맷
                String scheduleSummary = lastWeekSchedules.stream()
                        .map(s -> String.format(
                                "{scheduleId:%d, employeeId:%d, employeeName:'%s', date:'%s', clockIn:'%s', clockOut:'%s'}",
                                s.getId(), s.getEmployeeId(), s.getEmployeeName(),
                                s.getRegisteredDate(), s.getRegisteredClockIn(), s.getRegisteredClockOut()))
                        .collect(Collectors.joining(",\n"));

                // ✅ 템플릿 포맷
                String templateSummary = template.stream()
                        .map(t -> String.format("{id:%d, name:'%s', clockIn:'%s', clockOut:'%s'}",
                                t.getId(), t.getName(), t.getDefaultClockIn(), t.getDefaultClockOut()))
                        .collect(Collectors.joining(",\n"));

                // ✅ 근무/휴가 타입 포맷
                String workTypeSummary = workDtos.stream()
                        .map(w -> String.format("{id:%d, name:'%s'}", w.getId(), w.getName()))
                        .collect(Collectors.joining(", "));
                String leaveTypeSummary = leaveDtos.stream()
                        .map(l -> String.format("{id:%d, name:'%s'}", l.getId(), l.getName()))
                        .collect(Collectors.joining(", "));

                        // ✅ 4️⃣ 프롬프트 생성
                                        String prompt = """
                                아래 정보를 기반으로 인건비 효율을 분석하고, 다음주 동일 요일의 근무 스케줄 변경을 JSON으로 제안하세요.
                                ⚠️ JSON 이외의 문자는 절대 포함하지 마세요.
                                ⚠️ 백틱(```)이나 설명 없이 **순수 JSON 본문만 반환하세요.**
                        
                                현재 날짜: %s (Asia/Seoul 기준)
                                다음주 동일 요일 범위: %s ~ %s
                        
                                [전주 시간대별 매출 요약]
                                %s
                                
                                [금일 시간대별 매출 요약]
                                %s
                                
                                [전주 근무 스케줄]
                                %s
                        
                                [지점 근무 템플릿]
                                %s
                        
                                [근무유형(WorkType)]
                                %s
                        
                                [휴가유형(LeaveType)]
                                %s
                        
                                [직원(Employee) 목록]
                                %s
                        
                                요구사항:
                                1. 전주의 시간대별 매출과 근무 인원 정보를 비교해 시간당 인건비 평균을 계산하세요.
                                2. 오늘 매출 대비 인건비 수준을 추정하세요 (예: 효율적 / 과잉 / 부족).
                                3. 다음주 같은 요일의 스케줄을 조회하고, 필요한 경우 수정 제안을 만드세요.
                                4. 수정은 기존 템플릿 중 하나 또는 "ABSENT"로 변경 제안 가능합니다.
                                5. 반드시 아래 형식의 JSON만 반환하세요.
                                6. "reason" 필드에는 왜 이 변경이 필요한지 수치를 제시하며 논리적으로 설명하세요.
                        
                                JSON 형식:
                                {
                                  "branchId": number,
                                  "employeeId": number,               // 위 직원 목록 중 하나여야 함
                                  "scheduleId": number,               // 위 스케줄 목록 중 하나여야 함
                                  "workTypeId": number,               // 위 근무유형 목록 중 하나여야 함
                                  "leaveTypeId": number | null,       // 위 휴가유형 목록 중 하나 또는 null
                                  "attendanceTemplateId": number,     // 위 템플릿 목록 중 하나여야 함
                                  "registeredDate": "yyyy-MM-dd",     // 반드시 다음주 동일 요일 중 하루여야 함
                                  "reason": string                    // 수정 이유를 간단히 기술
                                }
                                """.formatted(
                        today,
                        today.plusWeeks(1).with(DayOfWeek.MONDAY),
                        today.plusWeeks(1).with(DayOfWeek.SUNDAY),
                        hourlySummary,
                        todaySummary,
                        scheduleSummary,
                        templateSummary,
                        workTypeSummary,
                        leaveTypeSummary,
                        employeeSummary
                );



                // ✅ 5️⃣ OpenAI 호출
                 String result = attendanceSuggestionClient.prompt()
                                        .user(prompt)
                                        .call()
                                        .content();

                                try {
                                    AttendanceModifyRequestDto suggestion =
                                            objectMapper.readValue(result, AttendanceModifyRequestDto.class);
                                    System.out.println("근태 수정 제안 DTO === " + suggestion);
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException("JSON 파싱 실패: " + result, e);
                                }

                System.out.println("챗봇 결과 === " + result);
                return ResponseEntity.ok(result);
            }

            default -> throw new IllegalArgumentException("지원하지 않는 action: " + action);
        }
    }

    // [ 재고 서비스]
    public ResponseEntity<?> handleStockAction(String action, JSONObject params, Long branchId) {
        String date = null;
        JSONObject range = null;
        if(params!=null){

        date = params.optString("date", null);
        range = params.optJSONObject("range", null);
        }
        LocalDate startDate = null;
        LocalDate endDate = null;

        if (range != null && range.has("start") && range.has("end")) {
            startDate = LocalDate.parse(range.optString("start"));
            endDate = LocalDate.parse(range.optString("end"));
        } else {
            YearMonth currentMonth = YearMonth.now(ZoneId.of("Asia/Seoul"));
            startDate = currentMonth.atDay(1);
            endDate = currentMonth.atEndOfMonth();
        }
        if(branchId==null){

            branchId = getBranchIdFromToken();
        }
        Branch branch = branchRepository.findById(branchId).orElseThrow(()->new EntityNotFoundException("존재하지 않는 지점입니다."));

        switch (action) {
            case "GET" -> {
                CommonSuccessDto response = client.getBranchProducts(branchId);

                List<OrderingInventoryClient.BranchProductResponseDto> stocks =
                        objectMapper.convertValue(
                                response.getResult(),
                                new TypeReference<List<OrderingInventoryClient.BranchProductResponseDto>>() {}
                        );

//                CommonSuccessDto response = orderingInventoryClient.getBranchProducts(branchId);
                System.out.println("branchID ======" + branchId);

                System.out.println("response =====" + response);

                return ResponseEntity.ok(stocks);

            }

            case "PATCH" -> {
                JSONArray itemsArr = params.getJSONArray("items");

                for (int i = 0; i < itemsArr.length(); i++) {
                    JSONObject item = itemsArr.getJSONObject(i);
                    Long productId = item.getLong("productId");
                    String product = item.getString("product");
                    Long quantity = item.getLong("quantity");
                    String type = quantity>=0 ? "INCREASE" : "DECREASE";
                    String reason = item.getString("reason");
                    OrderingInventoryClient.StockAdjustRequest requestDto = OrderingInventoryClient.StockAdjustRequest.makeDto(productId,quantity, type,reason);
                    orderingInventoryClient.adjustStock(requestDto);
                }
                return ResponseEntity.ok("재고 수정 완료");
            }
            case "ANALYZE" -> {
                CommonSuccessDto response = client.getBranchProducts(branchId);

                List<OrderingInventoryClient.BranchProductResponseDto> currentStocks =
                        objectMapper.convertValue(
                                response.getResult(),
                                new TypeReference<List<OrderingInventoryClient.BranchProductResponseDto>>() {}
                        );

                    ZoneId zone = ZoneId.of("Asia/Seoul");
                    LocalDate today = LocalDate.now(zone);
                    LocalDate targetDate = today.plusDays(1);

                // ✅ 지난 4주간 주별 판매 데이터 수집
                List<Map<String, Object>> last4Weeks = new ArrayList<>();
                for (int i = 4; i >= 1; i--) {
                    LocalDate start = today.minusWeeks(i).with(DayOfWeek.MONDAY);
                    LocalDate end = today.minusWeeks(i).with(DayOfWeek.SUNDAY);

                    CommonSuccessDto weekSales = chatFeignClient.getProductSales(branchId, start, end, "HIGH_MARGIN");
                    ProductSalesResponseDto weekData =
                            objectMapper.convertValue(weekSales.getResult(), ProductSalesResponseDto.class);

                    Map<String, Object> weekMap = new HashMap<>();
                    weekMap.put("weekStart", start);
                    weekMap.put("weekEnd", end);
                    weekMap.put("products", weekData.getProducts());
                    last4Weeks.add(weekMap);
                }



                Map<String, Object> input = new HashMap<>();
                input.put("branchId", branchId);
                input.put("stocks", last4Weeks);       // ✅ 지난 4주간 판매량
                input.put("products", currentStocks);
                    String jsonInput = null;
                    try {
                        jsonInput = objectMapper.writeValueAsString(input);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                System.out.println("input data" + jsonInput);


                String result = inventoryAdvisorClient
                            .prompt()
                            .user(jsonInput)
                            .call()
                            .content();
                JsonNode parsed = null;
                try {
                    parsed = objectMapper.readTree(result);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                return ResponseEntity.ok(parsed);

            }
            default -> throw new IllegalArgumentException("지원하지 않는 action: " + action);
        }
    }


    // [발주 서비스 ]
    public ResponseEntity<?> handleOrderAction(String action, JSONObject params, Long branchId) {
        String date = null;
        JSONObject range = null;
        if(params!=null){

            date = params.optString("date", null);
            range = params.optJSONObject("range", null);
        }

        LocalDate startDate = null;
        LocalDate endDate = null;

        if (range != null && range.has("start") && range.has("end")) {
            startDate = LocalDate.parse(range.optString("start"));
            endDate = LocalDate.parse(range.optString("end"));
        } else {
            YearMonth currentMonth = YearMonth.now(ZoneId.of("Asia/Seoul"));
            startDate = currentMonth.atDay(1);
            endDate = currentMonth.atEndOfMonth();
        }

        if(branchId==null){

            branchId = getBranchIdFromToken();
        }
        Branch branch = branchRepository.findById(branchId).orElseThrow(()->new EntityNotFoundException("존재하지 않는 지점입니다."));

        switch (action) {
            case "GET" -> {
                return  purchaseOrderController.getPurchaseOrders(branch.getId());
            }
            case "CREATE" -> {
                JSONArray itemsArr = params.getJSONArray("items");

                List<PurchaseOrderRequestDto.PurchaseOrderDetailRequestDto> items = new ArrayList<>();
                for (int i = 0; i < itemsArr.length(); i++) {
                    JSONObject item = itemsArr.getJSONObject(i);
                    String product = item.getString("product");
                    int quantity = item.getInt("quantity");
                    Long productId = item.getLong("productId");
                    PurchaseOrderRequestDto.PurchaseOrderDetailRequestDto dto = new PurchaseOrderRequestDto().makeOrderDetail(productId,quantity);
                    items.add(dto);
                }
                PurchaseOrderRequestDto requestDto = new PurchaseOrderRequestDto().makeOrder(branchId, items);
                ResponseEntity<?> result = purchaseOrderController.createPurchaseOrder(requestDto);
                return result;
            }
            default -> throw new IllegalArgumentException("지원하지 않는 action: " + action);
        }
    }




    // [ 매출 서비스 ]

    public ResponseEntity<?> handleSalesAction(String action, JSONObject params, Long branchId) {
        String date = null;
        JSONObject range = null;
        LocalDate startDate = null;
        LocalDate endDate = null;
        if(params!=null){
            date = params.optString("date", null);
            range = params.optJSONObject("range", null);
        }
        if (range != null && range.has("start") && range.has("end")) {
            startDate = LocalDate.parse(range.optString("start"));
            endDate = LocalDate.parse(range.optString("end"));
        } else {
            YearMonth currentMonth = YearMonth.now(ZoneId.of("Asia/Seoul"));
            startDate = currentMonth.atDay(1);
            endDate = currentMonth.atEndOfMonth();
        }
        AuthorityType authorityType = getEmployee().getAuthorityType();
        JSONObject product = null;

        if (params != null && params.has("product")) {
            product = params.optJSONObject("product");
        }
        if(branchId==null){

            branchId = getBranchIdFromToken();
        }
        Branch branch = branchRepository.findById(branchId).orElseThrow(()->new EntityNotFoundException("존재하지 않는 지점입니다."));
        String periodType =null;
        String sortType =null;
        if(params.has("periodType")){
            periodType = params.getString("periodType");
        }
        if(params.has("sortType")){
            sortType = params.getString("periodType");
        }
        switch (action) {
            case "GET" -> {
                    CommonSuccessDto response = null;
                    // 당일 매출

                    if(periodType!=null &&periodType.equals("DAY")){
                        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
                        startDate = today;
                        endDate = today;
                         response = client.getSalesStatistics(branch.getId(), startDate, endDate, "HOUR");

                        SalesStatisticsResponseDto dto = objectMapper.convertValue(
                                response.getResult(),
                                SalesStatisticsResponseDto.class);
                        return ResponseEntity.ok(dto);
                    }
                    // 상품 매출
                    else if(params.has("productSales")){
                        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
                        startDate = today;
                        endDate = today;

                        response = client.getProductSales(branch.getId(),startDate,endDate,"HIGH_MARGIN");
                        ProductSalesResponseDto dto = objectMapper.convertValue(response.getResult(), ProductSalesResponseDto.class);
                        log.info("[Sales] : " + dto);
                        return ResponseEntity.ok(dto);
                    }
                    // 인근 지역 매출 비교
                    else {
                        response = client.compareBranchSales(branch.getId(), startDate,endDate, null);
                        ProductSalesResponseDto dto = objectMapper.convertValue(response.getResult(), ProductSalesResponseDto.class);
                        log.info("[Sales] : " + dto);
                        return ResponseEntity.ok(dto);
                    }



            }
            case "CALCULATE" -> {
                ZoneId zone = ZoneId.of("Asia/Seoul");
                LocalDate today = LocalDate.now(zone);

                // 당일 매출
                CommonSuccessDto response = client.getSalesStatistics(branchId, today, today, "HOUR");
                SalesStatisticsResponseDto sales = objectMapper.convertValue(response.getResult(), SalesStatisticsResponseDto.class);
                List<SalesStatisticsDto> stats = Optional.ofNullable(sales.getStatistics()).orElse(Collections.emptyList());

                // 당일 스케줄
                List<ScheduleListDto> schedules = scheduleService.listAll(today, today);

                //전주 매출
                LocalDate prevWeekStart = today.minusWeeks(1).with(DayOfWeek.MONDAY);
                LocalDate prevWeekEnd = today.minusWeeks(1).with(DayOfWeek.SUNDAY);
                CommonSuccessDto prevSale = client.getSalesStatistics(branchId, prevWeekStart, prevWeekEnd, "HOUR");
                SalesStatisticsResponseDto prevSales = objectMapper.convertValue(prevSale.getResult(), SalesStatisticsResponseDto.class);
                List<SalesStatisticsDto> prevStats = Optional.ofNullable(prevSales.getStatistics()).orElse(Collections.emptyList());

                // 전주 스케줄

                List<ScheduleListDto> prevSchedules = scheduleService.listAll(prevWeekStart, prevWeekEnd);
                // 직원 별 임금 정보
                List<DispatchStatus> dispatchStatuses = dispatchStatusRepository.findAllByBranch(branch);
                List<EmployeeDetailDto> employees = new ArrayList<>();

                for(DispatchStatus ds : dispatchStatuses){
                    employees.add(new EmployeeDetailDto().fromEntity(ds.getEmployee()));
                }

                String employeeSummary = employees.stream()
                        .map(e -> String.format("{id:%d, name:'%s', hourlyPay:'%s'}",
                                e.getId(), e.getName(), e.getHourlyPay()))
                        .collect(Collectors.joining(",\n"));

                Map<String, Object> input = new HashMap<>();
                input.put("branchId", branchId);
                input.put("todaySales", stats);
                input.put("prevSales", prevStats);
                input.put("todaySchedules", schedules);
                input.put("prevSchedules", prevSchedules);
                input.put("employees", employees);

                String aiInput = null;
                try {
                    aiInput = objectMapper.writeValueAsString(input);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("ai 호출 시작");
                // ✅ AI 분석 호출
                String aiInsight = salesLaborAnalysisClient
                        .prompt()
                        .user(aiInput)
                        .call()
                        .content()
                        .trim();

                if (!aiInsight.startsWith("{")) {
                    int start = aiInsight.indexOf("{");
                    int end = aiInsight.lastIndexOf("}") + 1;

                    if (start >= 0 && end > start) {
                        aiInsight = aiInsight.substring(start, end);
                    } else {
                        throw new RuntimeException("유효한 JSON 응답 아님: " + aiInsight);
                    }
                }
                SalesLaborInsightDto insight = null;
                try {
                    insight = objectMapper.readValue(aiInsight, SalesLaborInsightDto.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                log.info("📊 AI 인건비 인사이트 === " + insight);
                return ResponseEntity.ok(insight);
            }

            case "ANALYZE" ->{
                ZoneId zone = ZoneId.of("Asia/Seoul");
                LocalDate today = LocalDate.now(zone);
                // 당일 매출
                CommonSuccessDto response = client.getSalesStatistics(branchId, today, today, "HOUR");
                SalesStatisticsResponseDto sales = objectMapper.convertValue(response.getResult(), SalesStatisticsResponseDto.class);
                List<SalesStatisticsDto> stats = Optional.ofNullable(sales.getStatistics()).orElse(Collections.emptyList());

                // 당일 스케줄
                List<ScheduleListDto> schedules = scheduleService.listAll(today, today);

                //전주 매출
                LocalDate prevWeekStart = today.minusWeeks(1).with(DayOfWeek.MONDAY);
                LocalDate prevWeekEnd = today.minusWeeks(1).with(DayOfWeek.SUNDAY);
                CommonSuccessDto prevSale = client.getSalesStatistics(branchId, prevWeekStart, prevWeekEnd, "HOUR");
                SalesStatisticsResponseDto prevSales = objectMapper.convertValue(prevSale.getResult(), SalesStatisticsResponseDto.class);
                List<SalesStatisticsDto> prevStats = Optional.ofNullable(prevSales.getStatistics()).orElse(Collections.emptyList());

                //전월 매출
                YearMonth lastMonth = YearMonth.now().minusMonths(1);
                LocalDate startOfLastMonth = lastMonth.atDay(1);          // 전월 1일
                LocalDate endOfLastMonth = lastMonth.atEndOfMonth();
                CommonSuccessDto prevMonthSale = client.getSalesStatistics(branchId, startOfLastMonth, endOfLastMonth, "HOUR");
                SalesStatisticsResponseDto prevMonthSales = objectMapper.convertValue(prevSale.getResult(), SalesStatisticsResponseDto.class);
                List<SalesStatisticsDto> prevMonthStats = Optional.ofNullable(prevSales.getStatistics()).orElse(Collections.emptyList());
                // 전주 스케줄

                List<ScheduleListDto> prevSchedules = scheduleService.listAll(prevWeekStart, prevWeekEnd);
                // 직원 별 임금 정보
                List<DispatchStatus> dispatchStatuses = dispatchStatusRepository.findAllByBranch(branch);
                List<EmployeeDetailDto> employees = new ArrayList<>();

                for(DispatchStatus ds : dispatchStatuses){
                    employees.add(new EmployeeDetailDto().fromEntity(ds.getEmployee()));
                }

                CommonSuccessDto productSales = client.getProductSales(branch.getId(),startOfLastMonth,endOfLastMonth,"HIGH_MARGIN");
                ProductSalesResponseDto salesProducts = objectMapper.convertValue(productSales.getResult(), ProductSalesResponseDto.class);

                String employeeSummary = employees.stream()
                        .map(e -> String.format("{id:%d, name:'%s', hourlyPay:'%s'}",
                                e.getId(), e.getName(), e.getHourlyPay()))
                        .collect(Collectors.joining(",\n"));

                Map<String, Object> input = new HashMap<>();
                input.put("branchId", branchId);
                input.put("todaySales", stats);
                input.put("prevSales", prevStats);
                input.put("prevMonthSales", prevMonthStats);
                input.put("salesProducts", salesProducts);
                input.put("todaySchedules", schedules);
                input.put("prevSchedules", prevSchedules);
                input.put("employees", employees);

                String aiInput = null;
                try {
                    aiInput = objectMapper.writeValueAsString(input);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("ai 호출 시작");
                // ✅ AI 분석 호출
                String aiInsight = salesReportClient
                        .prompt()
                        .user(aiInput)
                        .call()
                        .content()
                        .trim();

                if (!aiInsight.startsWith("{")) {
                    int start = aiInsight.indexOf("{");
                    int end = aiInsight.lastIndexOf("}") + 1;

                    if (start >= 0 && end > start) {
                        aiInsight = aiInsight.substring(start, end);
                    } else {
                        throw new RuntimeException("유효한 JSON 응답 아님: " + aiInsight);
                    }
                }
                SalesReportDto reportDto = null;
                try {
                    reportDto = objectMapper.readValue(aiInsight, SalesReportDto.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                log.info("📊 AI 매출 분석 인사이트 === " + reportDto);
                return ResponseEntity.ok(reportDto);

              }
            default -> throw new IllegalArgumentException("지원하지 않는 action: " + action);
        }
    }

    private Employee getEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();

        String email = (String) details.get("email");
        System.out.println("auth===" + details+ email);
        Employee employee = employeeRepository.findByEmail(email).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 유저입니다."));
        return employee;
    }

    public static Long getBranchIdFromToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("email" + auth.getName());
        System.out.println("auth======" + auth);

        if (auth == null || auth.getDetails() == null)
            throw new IllegalStateException("인증 정보가 없습니다.");

        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        Long branchId = Long.valueOf(details.get("branchId").toString());
        if (branchId == null)
            throw new IllegalStateException("branchId가 토큰에 없습니다.");

        return branchId;
    }




}
