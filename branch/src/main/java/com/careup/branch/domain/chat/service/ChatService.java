package com.careup.branch.domain.chat.service;

import com.careup.branch.common.client.ChatFeignClient;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.entity.BusinessHour;
import com.careup.branch.domain.branch.entity.BusinessHourType;
import com.careup.branch.domain.branch.repository.BranchRepository;
//import com.careup.branch.domain.chat.controller.ChatFeignClient;
import com.careup.branch.domain.branch.repository.BusinessHourRepository;
import com.careup.branch.domain.chat.dto.req.ChatBotReqDto;
import com.careup.branch.domain.chat.dto.req.ChatDailyPaymentsDto;
import com.careup.branch.domain.chat.dto.req.ChatOrderDto;
import com.careup.branch.domain.chat.dto.req.LaborCostRequestDto;
import com.careup.branch.domain.chat.dto.res.*;
import com.careup.branch.domain.chat.dto.res.sales.SalesPredictionRequestDto;
import com.careup.branch.domain.chat.dto.res.sales.SalesPredictionResponseDto;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.employee.repository.ScheduleEventRepository;
import com.careup.branch.domain.employee.repository.ScheduleRepository;
import com.careup.branch.domain.purchaseOrder.service.PurchaseOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
//import org.springframework.ai.openai.OpenAiChatModel;
//import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final BranchRepository branchRepository;
    private final ChatClient chatClient;
//    private final ChatFeignClient chatFeignClient;
    private final ObjectMapper objectMapper;
    private final Executor executor = Executors.newFixedThreadPool(4);
    private final ChatFeignClient client;
    private final DispatchStatusRepository dispatchStatusRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleEventRepository scheduleEventRepository;
    private final BusinessHourRepository businessHourRepository;
    private final EmployeeRepository employeeRepository;
    private final PurchaseOrderService purchaseOrderService;
    private final ChatUserService chatUserService;

    public String getResponseAsync(String message, String systemPrompt) {
        String promptText = systemPrompt == null ? "너는 CareUp의 전담 비서야. 친절히 답해." : systemPrompt;
        try {
            return CompletableFuture.supplyAsync(() -> {
                Prompt prompt = new Prompt(
                        List.of(
                                new SystemMessage(promptText),
                                new UserMessage(message)
                        )
                );

                return chatClient.prompt(prompt).call().chatResponse().getResult().toString();
            }, executor).get(10, TimeUnit.SECONDS); // ✅ 타임아웃 추가
        } catch (TimeoutException e) {
            log.error("⏰ OpenAI 응답 지연 (10초 초과)", e);
            return "요청이 너무 오래 걸립니다. 잠시 후 다시 시도해주세요.";
        } catch (Exception e) {
            log.error("💥 ChatModel 호출 중 오류 발생", e);
            return "AI 응답 중 오류가 발생했습니다.";
        }
    }

    public ResponseEntity<?> handleUserQuery(ChatBotReqDto dto) {
        String response = chatClient.prompt()
                .user(dto.getMessage())
                .call()
                .content();
        System.out.println("[Chat][Response] + :" + response );
        // 2️⃣ LLM 응답 JSON 파싱
        JSONObject json = new JSONObject(response);

// intent, action, parameters 파싱
        String intent = json.optString("intent", "UNKNOWN");
        String action = json.optString("action", "UNKNOWN");
        JSONObject parameters = json.optJSONObject("parameters");
        if (parameters != null && parameters.has("date")) {
            String dateStr = parameters.optString("date");
            LocalDate now = LocalDate.now();

            switch (dateStr.toLowerCase()) {
                case "today" -> parameters.put("date", now.toString());
                case "yesterday" -> parameters.put("date", now.minusDays(1).toString());
                case "tomorrow" -> parameters.put("date", now.plusDays(1).toString());
            }
        }
// intent 분기 처리
        System.out.println("[Chat][Intent] : " + intent);
        System.out.println("[Chat][Action] : " + action);
        switch (intent) {
                case "SALES" -> {
                return chatUserService.handleSalesAction(action, parameters, dto.getBranchId() );
                }
              case "ATTENDANCE" -> {
                return chatUserService.handleAttendanceAction(action, parameters,  dto.getBranchId());
              }
              case "STOCK" -> {
                return chatUserService.handleStockAction(action, parameters,  dto.getBranchId());
              }
              case "ORDER" -> {
                return chatUserService.handleOrderAction(action, parameters, dto.getBranchId());
              }
            default -> {
                return ResponseEntity.ok("죄송해요, 아직 그 요청은 처리할 수 없어요.");
            }
        }
    }






    private Branch getBranch(Long branchId) {
        return branchRepository.findById(branchId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 지점입니다."));
    }


    public String getSchedule(Long branchId) {
        return null;
    }

    // [일일 매출 조회]
    public ChatDailySalesDto getDailySales(Long branchId) {
        BusinessHourType type = null;

        BusinessHour businessHour = getBusinessHour(branchId);


        CommonSuccessDto dto = client.getDailyPayments(branchId);
        ChatDailyPaymentsDto result = objectMapper.convertValue(dto.getResult(), ChatDailyPaymentsDto.class);

        log.info("[ChatService]/getDailySales : 일일 매출 조회 성공");
        return new ChatDailySalesDto().makeDto(businessHour.getOpenTime(), businessHour.getCloseTime(), result);
    }

    private BusinessHour getBusinessHour(Long branchId) {
        BusinessHourType type;
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();


        if(dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
            type = BusinessHourType.WEEKDAYS;
        } else {
            type = BusinessHourType.WEEKENDS;
        }

        BusinessHour businessHour = businessHourRepository.findByBranchAndBusinessHourType(getBranch(branchId), type);
        return businessHour;
    }

    // [매출 예측]
    public SalesPredictionResponseDto getPredictDailySales(Long branchId) {
        BusinessHour businessHour = getBusinessHour(branchId);
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        int currentHour = LocalTime.now(ZoneId.of("Asia/Seoul")).getHour();
        CommonSuccessDto result = client.getPredictDailySales(branchId);
        SalesPredictionRequestDto dto = objectMapper.convertValue(result.getResult(), SalesPredictionRequestDto.class);
        dto.updateDto(today,dayOfWeek,currentHour,businessHour.getCloseTime().getHour());

        String message = String.format(
                "너는 외식업 매출 예측 AI야. " +
                        "날짜: %s, 요일: %s. " +
                        "현재 시간: %d시, 지금까지 매출: %,d원. " +
                        "최근 7일 시간대별 매출 데이터: %s. " +
                        "영업 종료 시간 : %d시" +
                        "남은 영업 시간 동안의 예상 매출을 예측하고, " +
                        "전일 대비, 전주 대비 변화율도 계산해. " +
                        "반환은 반드시 아래 JSON 형태로 만들어줘:\n" +
                        "{\n" +
                        "  \"date\": \"YYYY-MM-DD\",\n" +
                        "  \"totalSales\": 0,\n" +
                        "  \"salesSoFar\": 0,\n" +
                        "  \"remainingHoursSales\": 0,\n" +
                        "  \"percentChangePrevDay\": 0.0,\n" +
                        "  \"percentChangePrevWeek\": 0.0,\n" +
                        "  \"analysis\": \"\"\n" +
                        "}",
                dto.getToday(),
                dto.getDayOfWeek(),
                dto.getCurrentHour(),
                dto.getTodaySales(),
                dto.getRecentHourlySales().stream()
                        .map(h -> String.format("%s %02d시: %,d원", h.getDate(), h.getHour(), h.getSales()))
                        .collect(Collectors.joining(", ")),
                dto.getClosingHour()
        );
        log.info("[ChatService]/getPredictDailySales : 일일 매출 예측 성공");
        String response = getResponseAsync(message, null);
        return objectMapper.convertValue(response, SalesPredictionResponseDto.class);
    }

    // [인건비 조회]
    public ChatLaborCostDto getLaborCost(Long branchId) {
        LocalDate today = LocalDate.now();
        int currentHour = LocalTime.now(ZoneId.of("Asia/Seoul")).getHour();

        CommonSuccessDto result = client.getLaborCost(branchId);
        LaborCostRequestDto dto = objectMapper.convertValue(result.getResult(), LaborCostRequestDto.class);

        String prompt = String.format(
                "너는 외식업 지점의 근무 최적화 AI야. " +
                        "날짜: %s, 현재 시각: %d시. " +
                        "현재 매출: %,d원, 현재 근무 인원: %d명, 현재 인건비: %,d원. " +
                        "최근 7일 동일 시간대 인건비율 데이터: %s. " +
                        "다음 정보를 JSON 형태로 리턴해줘:\n" +
                        "- currentLaborCostRate: 현재 시간 인건비율 (인건비/매출*100)\n" +
                        "- avgLaborCostRatePrevWeek: 전주 동일 시간 평균 인건비율\n" +
                        "- recommendation: 인원 조정 권장 사항 (증원/감소/유지)\n" +
                        "- analysis: 현재 인건비율과 전주 대비 변화, 근무 인원 관련 인사이트",
                dto.getDate(),
                dto.getCurrentHour(),
                dto.getSalesSoFar(),
                dto.getCurrentStaffCount(),
                dto.getTotalLaborCostSoFar(),
                dto.getRecentWeekData().stream()
                        .map(d -> String.format("{date: %s, hour: %d, sales: %d, laborCost: %d, staffCount: %d}",
                                d.getDate(), d.getHour(), d.getSales(), d.getLaborCost(), d.getStaffCount()))
                        .collect(Collectors.joining(", "))
        );
        return null;
    }


    // [재고 조회]
    public ChatStockDto getStock(Long branchId) {
        CommonSuccessDto result = client.getDailyStock(branchId);
        ChatStockDto dto = objectMapper.convertValue(result.getResult(), ChatStockDto.class);
        log.info("[ChatService]/getStock : 재고 조회 성공");
        return dto;
    }

    // [발주 요청]
    public Long createOrder(ChatOrderDto dto) {
        log.info("[ChatService]/createOrder : 발주 요청 성공");
        return Long.parseLong(client.createOrder(dto).getResult().toString());
    }

    // [고객 조회]
    public ChatTodayVisitsDto getTodayVisits(Long branchId) {
        CommonSuccessDto result = client.getTodayVisits(branchId);
        ChatTodayVisitsDto dto = objectMapper.convertValue(result.getResult(), ChatTodayVisitsDto.class);

        

        log.info("[ChatService]/createOrder : 발주 요청 성공");
        return null;
    }


    public Long getLaborSum(Long branchId) {
        List<Employee> employees = dispatchStatusRepository.findAllByBranch(getBranch(branchId));
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Seoul"));

        long totalLaborCost = 0L;

        for(Employee employee : employees){
            Long hourPerSalary = employee.getHourPerSalary();
            if(hourPerSalary == null || hourPerSalary <= 0) continue;

            Optional<Schedule> scheduleOpt = scheduleRepository.findByEmployeeAndRegisteredDate(employee, today);
            if (scheduleOpt.isEmpty()) continue;
            Schedule schedule = scheduleOpt.get();

            LocalDateTime clockInTime = schedule.getRegisteredClockIn();
            LocalDateTime clockOutTime = schedule.getRegisteredClockOut();
            LocalDateTime breakStartTime = schedule.getRegisteredBreakStart();
            LocalDateTime breakEndTime = schedule.getRegisteredBreakEnd();

            if(clockInTime == null || clockOutTime == null) continue; // 필수 체크

            LocalTime clockIn = clockInTime.toLocalTime();
            LocalTime clockOut = clockOutTime.toLocalTime();
            LocalTime breakStart = breakStartTime.toLocalTime();
            LocalTime breakEnd = breakEndTime.toLocalTime();


            Duration workedDuration;
            if(now.isBefore(clockIn)) {
                continue;
            } else if(now.isAfter(clockOut)) {
                workedDuration = Duration.between(clockIn, clockOut);
            } else {
                workedDuration = Duration.between(clockIn, now);
            }

            // 휴게시간 제외
                if(!now.isBefore(breakEnd)) {
                    // 이미 휴게시간 지남
                    workedDuration = workedDuration.minus(Duration.between(breakStart, breakEnd));
                } else if(!now.isBefore(breakStart) && now.isBefore(breakEnd)) {
                    // 현재 휴게시간 중
                    workedDuration = workedDuration.minus(Duration.between(breakStart, now));
                }


            // 인건비 계산 (시급 * 근무 시간)
            double hoursWorked = workedDuration.toMinutes() / 60.0;
            long laborCost = Math.round(hoursWorked * hourPerSalary);

            totalLaborCost += laborCost;
        }

        return totalLaborCost;
    }



}
