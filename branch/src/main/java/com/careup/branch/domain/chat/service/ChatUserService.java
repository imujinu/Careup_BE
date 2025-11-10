package com.careup.branch.domain.chat.service;

import com.careup.branch.common.client.ChatFeignClient;
import com.careup.branch.common.client.OrderingInventoryClient;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.chat.dto.DocumentSearchResultDto;
import com.careup.branch.domain.chat.dto.SalesStatisticsDto;
import com.careup.branch.domain.chat.dto.attendance.*;
import com.careup.branch.domain.chat.dto.purchase.PurchaseResDto;
import com.careup.branch.domain.chat.dto.sales.*;
import com.careup.branch.domain.chat.dto.stock.StockResponseDto;
import com.careup.branch.domain.employee.controller.ScheduleController;
import com.careup.branch.domain.employee.dto.request.ScheduleUpdateDto;
import com.careup.branch.domain.employee.dto.response.*;
import com.careup.branch.domain.employee.entity.AuthorityType;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.*;
import com.careup.branch.domain.employee.service.ScheduleService;
import com.careup.branch.domain.purchaseOrder.controller.PurchaseOrderController;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderListResponseDto;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderRequestDto;
import com.careup.branch.domain.purchaseOrder.service.PurchaseOrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final RagService ragService;

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

    public ResponseEntity<?> handleAttendanceAction(String intent, String action, JSONObject params, Long branchId) {
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
                if("WEEK".equals(periodType)){
                    ZoneId zoneId = ZoneId.of("Asia/Seoul");
                    LocalDate today = LocalDate.now(zoneId);

                    // 저번주 월요일 ~ 일요일 계산
                    LocalDate startOfLastWeek = today.minusWeeks(1).with(DayOfWeek.MONDAY);
                    LocalDate endOfLastWeek = startOfLastWeek.with(DayOfWeek.SUNDAY);

                    List<ScheduleListDto> dto = scheduleService.listAll(startOfLastWeek,endOfLastWeek);
                    TodayAttendanceResDto resultDto = TodayAttendanceResDto.makeDto(intent,"get", periodType, startOfLastWeek, endOfLastWeek, dto, branch.getName());
                    System.out.println(resultDto);
                    return ResponseEntity.ok(resultDto);
                }
                else if("MONTH".equals(periodType)){
                    ZoneId zoneId = ZoneId.of("Asia/Seoul");
                    LocalDate today = LocalDate.now(zoneId);

                    // 저번달 1일 ~ 말일 계산
                    LocalDate startOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
                    LocalDate endOfLastMonth = startOfLastMonth.withDayOfMonth(startOfLastMonth.lengthOfMonth());

                    List<ScheduleListDto> dto = scheduleService.listAll(startOfLastMonth,endOfLastMonth);
                    System.out.println("=========응답 결과 ======" + dto);
                    List<ScheduleListDto> dtos = scheduleService.listAll(LocalDate.parse("2025-10-01"), LocalDate.parse("2025-10-30"));
                    System.out.println("=========응답 결과2 ======" + dtos);
                    TodayAttendanceResDto resultDto = TodayAttendanceResDto.makeDto(intent,"get",periodType, startOfLastMonth, endOfLastMonth, dto, branch.getName());
                    System.out.println(resultDto);
                    return ResponseEntity.ok(resultDto);
                }
                else if("DAY".equals(periodType)){
                    LocalDate day = LocalDate.parse(date);

                    List<ScheduleListDto> dto = scheduleService.listAll(day,day);
                    TodayAttendanceResDto resultDto = TodayAttendanceResDto.makeDto(intent,"get",periodType, day, day ,dto, branch.getName());
                    System.out.println(resultDto);
                    return ResponseEntity.ok(resultDto);
                }
                //전체 근태 조회
                else if (params.has("allAttendance")) {
                    List<ScheduleListDto> list = scheduleService.listAll(startDate, endDate);
                    AttendanceCompareResDto resultDto = AttendanceCompareResDto.makeDto(intent, action, list, branch.getName(), startDate,endDate);
                    List<AttendanceTemplateListDto> template = attendanceTemplateRepository.findAll().stream()
                            .map(AttendanceTemplateListDto::fromEntity)
                            .collect(Collectors.toList());
                    List<LeaveTypeDetailDto> leaveDtos = leaveTypeRepository.findAll().stream().map(LeaveTypeDetailDto::fromEntity).collect(Collectors.toList());
                    List<WorkTypeDetailDto> workDtos = workTypeRepository.findAll().stream().map(WorkTypeDetailDto::fromEntity).collect(Collectors.toList());
                    AttendanceAllResDto response = AttendanceAllResDto.builder()
                            .intent(intent)
                            .startDate(startDate)
                            .endDate(endDate)
                            .action("all")
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
                    AttendanceCompareResDto resultDto = AttendanceCompareResDto.makeDto(intent,"detail", list, branch.getName(), startDate,endDate);
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
            default -> throw new IllegalArgumentException("관련 정보가 존재하지 않습니다.: ");
        }
    }

    // [ 재고 서비스]
    public ResponseEntity<?> handleStockAction(String intent, String action, JSONObject params, Long branchId) {
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
                StockResponseDto dto = new StockResponseDto().makeDto(intent,action,stocks);
                return ResponseEntity.ok(dto);

            }

            case "PATCH" -> {
                JSONArray itemsArr = params.getJSONArray("items");

                for (int i = 0; i < itemsArr.length(); i++) {
                    JSONObject item = itemsArr.getJSONObject(i);
                    Long productId = item.getLong("productId");
                    String product = item.getString("product");
                    Long quantity = item.getLong("quantity");
                    String type = quantity>=0 ? "INCREASE" : "DECREASE";
                    quantity = Math.abs(quantity);
                    String reason = item.getString("reason");
                    System.out.println("재고 변경 type " + type);
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

                ObjectNode responseNode = (ObjectNode) parsed;

                responseNode.put("intent", intent);
                responseNode.put("action", action);

                return ResponseEntity.ok(responseNode);

            }
            default -> throw new IllegalArgumentException("지원하지 않는 action: " + action);
        }
    }


    // [발주 서비스 ]
    public ResponseEntity<?> handleOrderAction(String intent, String action, JSONObject params, Long branchId) {
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
                Page<PurchaseOrderListResponseDto> page = purchaseOrderService.getPurchaseOrders(
                        branch.getId(),
                        Pageable.unpaged()
                );
                PurchaseResDto dto = new PurchaseResDto().makeDto(intent, action, page.getContent());
                return ResponseEntity.ok(dto);
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

    public ResponseEntity<?> handleSalesAction(String intent, String action, JSONObject params, Long branchId) {
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
                if("WEEK".equals(periodType)){
                    // 저번주 월요일 ~ 일요일 계산
                    // 저번달 1일 ~ 말일 계산
                    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
                    LocalDate startOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
                    LocalDate endOfLastMonth = startOfLastMonth.withDayOfMonth(startOfLastMonth.lengthOfMonth());


                    response = client.getSalesStatistics(branch.getId(), startOfLastMonth, endOfLastMonth, "HOUR");

                    SalesStatisticsResponseDto dto = objectMapper.convertValue(
                            response.getResult(),
                            SalesStatisticsResponseDto.class);
                    return ResponseEntity.ok(dto);
                }
                else if("MONTH".equals(periodType)){


                    // 저번달 1일 ~ 말일 계산
                    LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
                    LocalDate startOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
                    LocalDate endOfLastMonth = startOfLastMonth.withDayOfMonth(startOfLastMonth.lengthOfMonth());


                    response = client.getSalesStatistics(branch.getId(), startOfLastMonth, endOfLastMonth, "HOUR");

                    SalesStatisticsResponseDto dto = objectMapper.convertValue(
                            response.getResult(),
                            SalesStatisticsResponseDto.class);
                    return ResponseEntity.ok(dto);
                }
                else if(periodType!=null &&periodType.equals("DAY")){
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

    // [문서 서비스 ]
    public ResponseEntity<?> handleDocumentAction(String intent, String action, JSONObject params, Long branchId) {

        if(branchId==null){

            branchId = getBranchIdFromToken();
        }
        Branch branch = branchRepository.findById(branchId).orElseThrow(()->new EntityNotFoundException("존재하지 않는 지점입니다."));

        switch (action) {
            case "QUERY" -> {
                Long documentId = params.getLong("documentId");
                String question = params.getString("question");
                String result = ragService.retrieve(documentId, question, 10);
                return ResponseEntity.ok(result);
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
