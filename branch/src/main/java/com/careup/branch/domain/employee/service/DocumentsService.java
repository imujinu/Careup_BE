package com.careup.branch.domain.employee.service;

import com.careup.branch.common.file.AwsS3Uploader;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.chat.dto.UploadFileDto;
import com.careup.branch.domain.chat.service.DocumentProcessingService;
import com.careup.branch.domain.chat.service.RagService;
import com.careup.branch.domain.employee.dto.request.DocumentsCreateReqDto;
import com.careup.branch.domain.employee.dto.response.DocumentsDto;
import com.careup.branch.domain.employee.dto.response.DocumentsListResDto;
import com.careup.branch.domain.employee.entity.Documents;
import com.careup.branch.domain.employee.repository.DocumentsRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class DocumentsService {

    private final DocumentsRepository documentsRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final AwsS3Uploader awsS3Uploader;
    private final DocumentProcessingService documentProcessingService;
    private final RagService ragService;

    private static final String TABLE_NAME = "documents";

    // 서류 생성 - 특정 지점 하위로 생성
    public DocumentsDto createDocuments(Long branchId, DocumentsCreateReqDto requestDto) {
        MultipartFile file = requestDto.getDocumentUrl();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 필요합니다.");
        }

        // Branch 조회
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new EntityNotFoundException("해당 지점이 존재하지 않습니다. branchId: " + branchId));

        String uploadedUrl = awsS3Uploader.uploadFile(TABLE_NAME, branchId, file);

        Documents document = Documents.builder()
                .employee(null)
                .branch(branch)
                .documentType(requestDto.getDocumentType())
                .title(requestDto.getTitle())
                .documentUrl(uploadedUrl)
                .fileSize(file.getSize())
                .expiryDate(requestDto.getExpiryDate())
                .description(requestDto.getDescription())
                .build();

        document.updateStatus();

        Documents savedDocument = documentsRepository.save(document);
        UploadFileDto uploadFileDto = documentProcessingService.uploadPdfFile(document.getId(), requestDto.getDocumentUrl());
        ragService.uploadPdfFile(uploadFileDto.getDocumentId(), uploadFileDto.getFile(), uploadFileDto.getFileName());
        return DocumentsDto.fromEntity(savedDocument);
    }

    // 서류 목록 조회 (페이지네이션) - 특정 지점
    @Transactional(readOnly = true)
    public DocumentsListResDto getDocumentsListByBranch(Long branchId, Pageable pageable) {
        if (branchId == null) {
            throw new IllegalArgumentException("branchId는 필수입니다.");
        }
        // Branch에 직접 연결된 서류 조회
        Page<Documents> page = documentsRepository.findByBranch_Id(branchId, pageable);

        page.getContent().forEach(Documents::updateStatus);

        Page<DocumentsDto> dtoPage = page.map(DocumentsDto::fromEntity);
        return DocumentsListResDto.fromPage(dtoPage);
    }

    // 서류 단건 조회
    @Transactional(readOnly = true)
    public DocumentsDto getDocument(Long id) {
        Documents findDocument = documentsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 문서가 존재하지 않습니다."));

        findDocument.updateStatus();

        return DocumentsDto.fromEntity(findDocument);
    }

    // 서류 수정
    public DocumentsDto updateDocuments(Long id, DocumentsCreateReqDto request) {
        Documents findDocuments = documentsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 문서가 존재하지 않습니다."));

        String currentUrl = findDocuments.getDocumentUrl();
        String newUrl = currentUrl;
        Long newFileSize = findDocuments.getFileSize();
        MultipartFile newFile = request.getDocumentUrl();
        if (newFile != null && !newFile.isEmpty()) {
            newUrl = awsS3Uploader.uploadFile(TABLE_NAME, findDocuments.getId(), newFile);
            newFileSize = newFile.getSize();
            try {
                if (currentUrl != null && !currentUrl.isBlank()) {
                    awsS3Uploader.deleteByUrl(currentUrl);
                }
            } catch (Exception ex) {
                log.warn("기존 S3 파일 삭제 실패. url={}", currentUrl, ex);
            }
        }

        findDocuments.update(
                id,
                findDocuments.getEmployee(),
                findDocuments.getBranch(),
                request.getDocumentType(),
                request.getTitle(),
                newUrl,
                newFileSize,
                request.getExpiryDate(),
                request.getDescription()
        );

        Documents updatedDocument = documentsRepository.save(findDocuments);
        return DocumentsDto.fromEntity(updatedDocument);
    }

    // 서류 삭제
    public void deleteDocuments(Long id) {
        Documents deletedDocument = documentsRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 문서가 존재하지 않습니다."));

        String url = deletedDocument.getDocumentUrl();
        if (url != null && !url.isBlank()) {
            awsS3Uploader.deleteByUrl(url);
        }

        documentsRepository.delete(deletedDocument);
    }

    // 서류 다운로드 URL 조회
    @Transactional(readOnly = true)
    public String getDocumentDownloadUrl(Long id) {
        Documents findDocument = documentsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 문서가 존재하지 않습니다."));

        return findDocument.getDocumentUrl();
    }
}

