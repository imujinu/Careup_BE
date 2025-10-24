package com.careup.branch.domain.chat.service;

import com.careup.branch.common.client.ChatFeignClient;
import com.careup.branch.common.client.OrderingInventoryClient;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.chat.dto.res.attendance.AttendanceCompareResDto;
import com.careup.branch.domain.chat.dto.res.attendance.TodayAttendanceResDto;
import com.careup.branch.domain.chat.dto.res.sales.AllBranchesSalesResponseDto;
import com.careup.branch.domain.chat.dto.res.sales.BranchSalesDetailResponseDto;
import com.careup.branch.domain.chat.dto.res.sales.ProductSalesResponseDto;
import com.careup.branch.domain.chat.dto.res.sales.SalesStatisticsResponseDto;
import com.careup.branch.domain.employee.controller.ScheduleController;
import com.careup.branch.domain.employee.dto.response.ScheduleListDto;
import com.careup.branch.domain.employee.entity.AuthorityType;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import com.careup.branch.domain.employee.service.ScheduleService;
import com.careup.branch.domain.purchaseOrder.controller.PurchaseOrderController;
import com.careup.branch.domain.purchaseOrder.dto.PurchaseOrderRequestDto;
import com.careup.branch.domain.purchaseOrder.service.PurchaseOrderService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    // [근태 서비스 ]

    public ResponseEntity<?> handleAttendanceAction(String action, JSONObject params, Long branchId) {
        LocalDate startDate = null;
        LocalDate endDate = null;
        if(branchId==null){

        branchId = getBranchIdFromToken();
        }


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
                    return ResponseEntity.ok(resultDto);
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

                //근태 수정 제안
                else if (params.has("suggestion")){


                    return null;
                }

                else{
                    throw new IllegalArgumentException("지원하지 않는 action: " + action);
                }

            }
            case "COMPARE" ->{
                if (params.has("employees")){

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
                }else{
                    throw new IllegalArgumentException("지원하지 않는 action: " + action);
                }
            }
            case "PATCH" ->{

                return null;
            }


            default -> throw new IllegalArgumentException("지원하지 않는 action: " + action);
        }
    }

    // [ 재고 서비스]
    public ResponseEntity<?> handleStockAction(String action, JSONObject params, Long branchId) {
        String date = params.optString("date", null);
        JSONObject range = params.optJSONObject("range");
        LocalDate startDate = LocalDate.parse(range.optString("start", null));
        LocalDate endDate = LocalDate.parse(range.optString("end", null));
        if(branchId==null){

            branchId = getBranchIdFromToken();
        }
        Branch branch = branchRepository.findById(branchId).orElseThrow(()->new EntityNotFoundException("존재하지 않는 지점입니다."));
        switch (action) {
            case "GET" -> {
                CommonSuccessDto response = client.getBranchProducts(branchId);
                List<OrderingInventoryClient.BranchProductResponseDto> dtos =
                        objectMapper.convertValue(
                                response.getResult(),
                                new TypeReference<List<OrderingInventoryClient.BranchProductResponseDto>>() {}
                        );
                return ResponseEntity.ok(dtos);
            }
//            case "CREATE" -> {
//                int amount = json.path("amount").asInt(0);
//                return salesService.createSalesRecord(amount);
//            }
//            case "PATCH" -> {
//                String id = json.path("id").asText();
//                int newAmount = json.path("amount").asInt();
//                return salesService.updateSales(id, newAmount);
//            }
//            case "DELETE" -> {
//                String id = json.path("id").asText();
//                return salesService.deleteSales(id);
//            }
            default -> throw new IllegalArgumentException("지원하지 않는 action: " + action);
        }
    }


    // [발주 서비스 ]
    public ResponseEntity<?> handleOrderAction(String action, JSONObject params, Long branchId) {
        String date = params.optString("date", null);
        JSONObject range = params.optJSONObject("range");
        LocalDate startDate = LocalDate.parse(range.optString("start", null));
        LocalDate endDate = LocalDate.parse(range.optString("end", null));

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
//            case "PATCH" -> {
//                String id = json.path("id").asText();
//                int newAmount = json.path("amount").asInt();
//                return salesService.updateSales(id, newAmount);
//            }
//            case "DELETE" -> {
//                String id = json.path("id").asText();
//                return salesService.deleteSales(id);
//            }
            default -> throw new IllegalArgumentException("지원하지 않는 action: " + action);
        }
    }




    // [ 매출 서비스 ]

    public ResponseEntity<?> handleSalesAction(String action, JSONObject params, Long branchId) {
        AuthorityType authorityType = getEmployee().getAuthorityType();
        LocalDate date = LocalDate.parse(params.optString("date", null));
        JSONObject range = params.optJSONObject("range");
        JSONObject product = params.optJSONObject("product");
        LocalDate startDate = LocalDate.parse(range.optString("start", null));
        LocalDate endDate = LocalDate.parse(range.optString("end", null));
        if(branchId==null){

            branchId = getBranchIdFromToken();
        }
        Branch branch = branchRepository.findById(branchId).orElseThrow(()->new EntityNotFoundException("존재하지 않는 지점입니다."));
        String periodType = params.getString("periodType");
        String sortType = params.getString("sortType");
        switch (action) {
            case "GET" -> {
                if(authorityType == AuthorityType.HQ_ADMIN){
                    CommonSuccessDto response = null;
                    // 특정 지점 매출 조회
                    if(params.has("brandId")){
                        Long id = params.getLong("branchId");
                        response = client.getBranchSalesDetail(id,startDate,endDate, periodType);
                        BranchSalesDetailResponseDto dto = objectMapper.convertValue(response.getResult(), BranchSalesDetailResponseDto.class);
                        return ResponseEntity.ok(dto);
                    }
                    // 전체 지점 매출 조회
                    else{
                        response = client.getAllBranchesSales(startDate,endDate,periodType);
                        AllBranchesSalesResponseDto dto = objectMapper.convertValue(response.getResult(), AllBranchesSalesResponseDto.class);
                        return ResponseEntity.ok(dto);
                    }
                }else{
                    CommonSuccessDto response = null;
                    // 예상 매출
                    if(params.has("predictedSales")){
                        response = client.getSalesForecast(branch.getId(), date);
                        ProductSalesResponseDto dto = objectMapper.convertValue(response.getResult(), ProductSalesResponseDto.class);
                        log.info("[Sales] : " + dto);
                        return ResponseEntity.ok(dto);
                    }
                    // 인근 지역 매출 비교
                    else if(params.has("compareWith")){
                        response = client.compareBranchSales(branch.getId(), startDate,endDate, null);
                        ProductSalesResponseDto dto = objectMapper.convertValue(response.getResult(), ProductSalesResponseDto.class);
                        log.info("[Sales] : " + dto);
                        return ResponseEntity.ok(dto);
                    }
                    // 상품 매출
                    else if(params.has("productSales")){
                        response = client.getProductSales(branch.getId(),startDate,endDate,sortType);
                        ProductSalesResponseDto dto = objectMapper.convertValue(response.getResult(), ProductSalesResponseDto.class);
                        log.info("[Sales] : " + dto);
                        return ResponseEntity.ok(dto);
                    }
                    // 기간별 매출
                    else{

                       response = client.getSalesStatistics(branch.getId(), startDate, endDate, periodType);
                        SalesStatisticsResponseDto dto = objectMapper.convertValue(response.getResult(), SalesStatisticsResponseDto.class);
                        log.info("[Sales] : " + dto);
                        return ResponseEntity.ok(dto);
                    }
                }
            }
              case "COMPARE" -> {
                if(params.has("branchIds")){
                      Object branchIdsObj = params.get("branchIds");
                      List<Long> branchIds = new ArrayList<>();
                      JSONArray array = (JSONArray) branchIdsObj;
                      for (int i = 0; i < array.length(); i++) {
                          branchIds.add(array.getLong(i));
                      }
                      CommonSuccessDto response = client.compareBranchesSales(branchIds, startDate, endDate, periodType);

                }
                throw new IllegalArgumentException("지점 아이디 값이 입력되지 않았습니다.");
              }
            default -> throw new IllegalArgumentException("지원하지 않는 action: " + action);
        }
    }

    private Employee getEmployee() {
        Employee employee = employeeRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow(()-> new EntityNotFoundException("존재하지 않는 유저입니다."));
        return employee;
    }

    public Long getBranchIdFromToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getDetails() == null)
            throw new IllegalStateException("인증 정보가 없습니다.");

        Map<String, Object> details = (Map<String, Object>) auth.getDetails();

        Object branchIdObj = details.get("branchId");
        if (branchIdObj == null)
            throw new IllegalStateException("branchId가 토큰에 없습니다.");

        return ((Number) branchIdObj).longValue();
    }




}
