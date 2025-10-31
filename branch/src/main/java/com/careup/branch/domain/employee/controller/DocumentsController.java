package com.careup.branch.domain.employee.controller;

import com.careup.branch.common.dto.CommonErrorDto;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.employee.dto.request.DocumentsCreateReqDto;
import com.careup.branch.domain.employee.dto.response.DocumentsDto;
import com.careup.branch.domain.employee.dto.response.DocumentsListResDto;
import com.careup.branch.domain.employee.service.DocumentsService;
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
@RequestMapping("/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentsController {

    private final DocumentsService documentsService;

    // 생성 - 특정 지점 하위
    @PreAuthorize("hasAnyRole('HQ_ADMIN', 'BRANCH_ADIMN', 'FRANCHISE_ADMIN')")
    @PostMapping(value = "/branch/{branchId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createDocument(
            @PathVariable Long branchId,
            @ModelAttribute DocumentsCreateReqDto requestDto) {
        try {
            DocumentsDto newDocument = documentsService.createDocuments(branchId, requestDto);
            log.info("DocumentsDto created: {}", newDocument.toString());
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(newDocument)
                            .status_code(HttpStatus.CREATED.value())
                            .status_message("서류 생성 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("서류 생성 중 오류가 발생했습니다. error: " + e.getMessage())
                            .build()
            );
        }
    }

    // 서류 조회 (페이지네이션) - 지점별 & ALL 권한 허용
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<?> getDocumentsListByBranch(
            @PathVariable Long branchId,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        try {
            DocumentsListResDto documentsList = documentsService.getDocumentsListByBranch(branchId, pageable);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(documentsList)
                            .status_code(HttpStatus.OK.value())
                            .status_message("지점별 서류 목록 조회 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_FOUND.value())
                            .status_message("지점별 서류 목록 조회 중 오류가 발생했습니다. error: " + e.getMessage())
                            .build()
            );
        }
    }

    // 서류 단건 조회 - 특정 지점 하위 - ALL 권한 허용
    @GetMapping("/{documentId}")
    public ResponseEntity<?> getDocument(@PathVariable Long documentId) {
        try {
            DocumentsDto findDocument = documentsService.getDocument(documentId);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(findDocument)
                            .status_code(HttpStatus.OK.value())
                            .status_message("서류 단건 조회 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_FOUND.value())
                            .status_message("서류 단건 조회 중 오류가 발생했습니다. error: " + e.getMessage())
                            .build()
            );
        }
    }

    // 서류 수정 - 특정 지점 하위
    @PreAuthorize("hasAnyRole('HQ_ADMIN', 'BRANCH_ADIMN', 'FRANCHISE_ADMIN')")
    @PatchMapping(value = "/{documentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateDocument(
            @PathVariable Long documentId,
            @ModelAttribute DocumentsCreateReqDto requestDto) {
        try {
            DocumentsDto result = documentsService.updateDocuments(documentId, requestDto);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(result)
                            .status_code(HttpStatus.OK.value())
                            .status_message("서류 수정 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("서류 수정 중 오류가 발생했습니다. error: " + e.getMessage())
                            .build()
            );
        }
    }

    // 서류 삭제 - 특정 지점 하위
    @PreAuthorize("hasAnyRole('HQ_ADMIN', 'BRANCH_ADIMN', 'FRANCHISE_ADMIN')")
    @DeleteMapping("/{documentId}")
    public ResponseEntity<?> deleteBranchDocument(@PathVariable Long documentId) {
        try {
            documentsService.deleteDocuments(documentId);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(documentId)
                            .status_code(HttpStatus.OK.value())
                            .status_message("서류 삭제 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("서류 삭제 중 오류가 발생했습니다. error: " + e.getMessage())
                            .build()
            );
        }
    }

    // 서류 다운로드 - 특정 지점 하위 - ALL 권한 허용
    @GetMapping("/{documentId}/download")
    public ResponseEntity<?> downloadDocument(@PathVariable Long documentId) {
        try {
            String downloadUrl = documentsService.getDocumentDownloadUrl(documentId);
            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(downloadUrl)
                            .status_code(HttpStatus.OK.value())
                            .status_message("서류 다운로드 URL 조회 완료")
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    CommonErrorDto.builder()
                            .status_code(HttpStatus.NOT_FOUND.value())
                            .status_message("서류 다운로드 URL 조회 중 오류가 발생했습니다. error: " + e.getMessage())
                            .build()
            );
        }
    }
}
