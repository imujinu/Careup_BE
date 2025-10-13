package com.careup.branch.domain.branch.controller;

import com.careup.branch.common.dto.CommonErrorDto;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.branch.dto.document.BranchDocumentDto;
import com.careup.branch.domain.branch.dto.document.BranchDocumentListResDto;
import com.careup.branch.domain.branch.dto.document.BranchDocumentCreateReqDto;
import com.careup.branch.domain.branch.service.BranchDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/branch-documents")
@RequiredArgsConstructor
@Slf4j
public class BranchDocumentController {

    private final BranchDocumentService branchDocumentService;

    // 지점 서류 생성 - 특정 지점 하위
    @PreAuthorize("hasRole('HQ_ADMIN')")
    @PostMapping(value = "/branch/{branchId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createBranchDocument(
            @PathVariable Long branchId,
            @ModelAttribute BranchDocumentCreateReqDto branchDocumentCreateReqDto) {
        try {
            BranchDocumentDto branchDocument = branchDocumentService.createBranchDocument(branchId, branchDocumentCreateReqDto);
            log.info("BranchDocumentDto created: {}", branchDocument.toString());
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(branchDocument)
                            .status_code(HttpStatus.CREATED.value())
                            .status_message("지점 서류 생성 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("지정 서류 생성 중 오류가 발생했습니다. error: " + e.getMessage())
                            .build()
            );
        }
    }

    // 지점 서류 조회 (페이지네이션) - 지점별
    @PreAuthorize("hasRole('HQ_ADMIN')")
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<?> getBranchDocuments(
            @PathVariable Long branchId,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable
            ) {
        try {
            BranchDocumentListResDto branchDocumentList = branchDocumentService.getBranchDocumentList(branchId, pageable);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(branchDocumentList)
                            .status_code(HttpStatus.OK.value())
                            .status_message("지점 서류 목록 조회 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_FOUND.value())
                            .status_message("지점 서류 목록 조회 중 오류가 발생했습니다. error: " + e.getMessage())
                            .build()
            );
        }
    }

    // 지점 서류 단건 조회 - 특정 지점 하위
    @PreAuthorize("hasRole('HQ_ADMIN')")
    @GetMapping("/branch/{branchId}/{id}")
    public ResponseEntity<?> getBranchDocument(@PathVariable Long branchId, @PathVariable Long id) {
        try {
            BranchDocumentDto findDocument = branchDocumentService.getBranchDocument(branchId, id);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(findDocument)
                            .status_code(HttpStatus.OK.value())
                            .status_message("지점 서류 단건 조회 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_FOUND.value())
                            .status_message("지점 서류 단건 조회 중 오류가 발생했습니다. error: " + e.getMessage())
                            .build()
            );
        }
    }

    // 지점 서류 수정 - 특정 지점 하위
    @PreAuthorize("hasRole('HQ_ADMIN')")
    @PatchMapping(value = "/branch/{branchId}/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateBranchDocument(
            @PathVariable Long branchId,
            @PathVariable Long id,
            @ModelAttribute BranchDocumentCreateReqDto request) {
        try {
            BranchDocumentDto result = branchDocumentService.updateBranchDocument(branchId, id, request);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(result)
                            .status_code(HttpStatus.OK.value())
                            .status_message("지점 서류 수정 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("지점 서류 수정 중 오류가 발생했습니다. error: " + e.getMessage())
                            .build()
            );
        }
    }

    // 지점 서류 삭제 - 특정 지점 하위
    @PreAuthorize("hasRole('HQ_ADMIN')")
    @DeleteMapping("/branch/{branchId}/{id}")
    public ResponseEntity<?> deleteBranchDocument(@PathVariable Long branchId, @PathVariable Long id) {
        try {
            branchDocumentService.deleteBranchDocument(branchId, id);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(id)
                            .status_code(HttpStatus.OK.value())
                            .status_message("지점 서류 삭제 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("지점 서류 삭제 중 오류가 발생했습니다. error: " + e.getMessage())
                            .build()
            );
        }
    }
}
