package com.careup.branch.domain.employee.service;

import com.careup.branch.common.file.AwsS3Uploader;
import com.careup.branch.domain.employee.dto.request.DocumentsCreateReqDto;
import com.careup.branch.domain.employee.dto.response.DocumentsDto;
import com.careup.branch.domain.employee.dto.response.DocumentsListResDto;
import com.careup.branch.domain.employee.entity.Documents;
import com.careup.branch.domain.employee.entity.Employee;
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
    private final AwsS3Uploader awsS3Uploader;

    private static final String TABLE_NAME = "branch_documents";

    // 서류 생성 - 특정 지점 하위로 생성
    public DocumentsDto createDocuments(Long employeeId, DocumentsCreateReqDto requestDto) {
        Employee findEmployee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 직원이 존재하지 않습니다."));

        MultipartFile file = requestDto.getDocumentUrl();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 필요합니다.");
        }
        String uploadedUrl = awsS3Uploader.uploadFile(TABLE_NAME, findEmployee.getId(), file);

        Documents document = Documents.builder()
                .employee(findEmployee)
                .documentType(requestDto.getDocumentType())
                .title(requestDto.getTitle())
                .documentUrl(uploadedUrl)
                .build();

        Documents savedDocument = documentsRepository.save(document);

        return DocumentsDto.fromEntity(savedDocument);
    }

    // 서류 목록 조회 (페이지네이션) - 특정 지점
    @Transactional(readOnly = true)
    public DocumentsListResDto getDocumentsList(Long employeeId, Pageable pageable) {
        if (employeeId == null) {
            throw new IllegalArgumentException("employeeId는 필수입니다.");
        }
        Page<Documents> page = documentsRepository.findByEmployee_Id(employeeId, pageable);
        Page<DocumentsDto> dtoPage = page.map(this::toDto);
        return DocumentsListResDto.fromPage(dtoPage);
    }

    // 서류 단건 조회 - 특정 지점에 속한 문서만
    @Transactional(readOnly = true)
    public DocumentsDto getDocument(Long employeeId, Long id) {
        Documents findDocument = documentsRepository.findByIdAndEmployee_Id(id, employeeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 지점의 문서가 존재하지 않습니다."));
        return DocumentsDto.fromEntity(findDocument);
    }

    // 서류 수정 - 특정 지점에 속한 문서만
    public DocumentsDto updateDocuments(Long employeeId, Long id, DocumentsCreateReqDto request) {
        Documents findDocuments = documentsRepository.findByIdAndEmployee_Id(id, employeeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 지점에 해당 문서가 존재하지 않습니다."));

        Employee findEmployee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 직원이 존재하지 않습니다."));

        String currentUrl = findDocuments.getDocumentUrl();
        String newUrl = currentUrl;
        MultipartFile newFile = request.getDocumentUrl();
        if (newFile != null && !newFile.isEmpty()) {
            newUrl = awsS3Uploader.uploadFile(TABLE_NAME, findDocuments.getId(), newFile);
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
                findEmployee, // 지점 변경은 허용하지 않고 path의 branchId를 유지
                request.getDocumentType(),
                request.getTitle(),
                newUrl
        );

        Documents updatedDocument = documentsRepository.save(findDocuments);
        return DocumentsDto.fromEntity(updatedDocument);
    }

    // 서류 삭제 - 특정 지점에 속한 문서만
    public void deleteDocuments(Long emploueeId, Long id) {
        Documents deletedDocument = documentsRepository.findByIdAndEmployee_Id(id, emploueeId)
                .orElseThrow(() -> new EntityNotFoundException("해당 문서가 존재하지 않습니다."));

        String url = deletedDocument.getDocumentUrl();
        if (url != null && !url.isBlank()) {
            awsS3Uploader.deleteByUrl(url);
        }

        documentsRepository.delete(deletedDocument);
    }

    // Entity -> DTO
    private DocumentsDto toDto(Documents entity) {
        return DocumentsDto.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployee() != null ? entity.getEmployee().getId() : null)
                .documentType(entity.getDocumentType())
                .title(entity.getTitle())
                .documentUrl(entity.getDocumentUrl())
                .build();
    }
}
