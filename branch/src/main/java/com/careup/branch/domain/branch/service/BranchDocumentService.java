package com.careup.branch.domain.branch.service;

import com.careup.branch.domain.branch.dto.document.BranchDocumentDto;
import com.careup.branch.domain.branch.dto.document.BranchDocumentListResDto;
import com.careup.branch.domain.branch.dto.document.BranchDocumentCreateReqDto;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.entity.BranchDocuments;
import com.careup.branch.domain.branch.repository.BranchDocumentRepository;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.common.file.AwsS3Uploader;
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
public class BranchDocumentService {

    private final BranchDocumentRepository branchDocumentRepository;
    private final BranchRepository branchRepository;
    private final AwsS3Uploader awsS3Uploader;

    private static final String TABLE_NAME = "branch_documents";

    // 지점 서류 생성 - 특정 지점 하위로 생성
    public BranchDocumentDto createBranchDocument(Long branchId, BranchDocumentCreateReqDto requestDto) {
        Branch findBranch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("해당 지점이 존재하지 않습니다."));

        MultipartFile file = requestDto.getBranchDocumentUrl();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 필요합니다.");
        }
        String uploadedUrl = awsS3Uploader.uploadFile(TABLE_NAME, findBranch.getId(), file);

        BranchDocuments branchDocument = BranchDocuments.builder()
                .branch(findBranch)
                .branchDocumentType(requestDto.getBranchDocumentType())
                .title(requestDto.getTitle())
                .branchDocumentUrl(uploadedUrl)
                .build();

        BranchDocuments saved = branchDocumentRepository.save(branchDocument);

        return BranchDocumentDto.fromEntity(saved);
    }

    // 지점 서류 목록 조회 (페이지네이션) - 특정 지점
    @Transactional(readOnly = true)
    public BranchDocumentListResDto getBranchDocumentList(Long branchId, Pageable pageable) {
        if (branchId == null) {
            throw new IllegalArgumentException("branchId는 필수입니다.");
        }
        Page<BranchDocuments> page = branchDocumentRepository.findByBranch_Id(branchId, pageable);
        Page<BranchDocumentDto> dtoPage = page.map(this::toDto);
        return BranchDocumentListResDto.fromPage(dtoPage);
    }

    // 지점 서류 단건 조회 - 특정 지점에 속한 문서만
    @Transactional(readOnly = true)
    public BranchDocumentDto getBranchDocument(Long branchId, Long id) {
        BranchDocuments findDocument = branchDocumentRepository.findByIdAndBranch_Id(id, branchId)
                .orElseThrow(() -> new IllegalArgumentException("해당 지점에 해당 문서가 존재하지 않습니다."));
        return BranchDocumentDto.fromEntity(findDocument);
    }

    // 지점 서류 수정 - 특정 지점에 속한 문서만
    public BranchDocumentDto updateBranchDocument(Long branchId, Long id, BranchDocumentCreateReqDto request) {
        BranchDocuments findDocument = branchDocumentRepository.findByIdAndBranch_Id(id, branchId)
                .orElseThrow(() -> new IllegalArgumentException("해당 지점에 해당 문서가 존재하지 않습니다."));

        Branch findBranch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("해당 지점이 존재하지 않습니다."));

        String currentUrl = findDocument.getBranchDocumentUrl();
        String newUrl = currentUrl;
        MultipartFile newFile = request.getBranchDocumentUrl();
        if (newFile != null && !newFile.isEmpty()) {
            newUrl = awsS3Uploader.uploadFile(TABLE_NAME, findDocument.getId(), newFile);
            try {
                if (currentUrl != null && !currentUrl.isBlank()) {
                    awsS3Uploader.deleteByUrl(currentUrl);
                }
            } catch (Exception ex) {
                log.warn("기존 S3 파일 삭제 실패. url={}", currentUrl, ex);
            }
        }

        findDocument.update(
                id,
                findBranch, // 지점 변경은 허용하지 않고 path의 branchId를 유지
                request.getBranchDocumentType(),
                request.getTitle(),
                newUrl
        );

        BranchDocuments updatedDocument = branchDocumentRepository.save(findDocument);
        return BranchDocumentDto.fromEntity(updatedDocument);
    }

    // 지점 서류 삭제 - 특정 지점에 속한 문서만
    public void deleteBranchDocument(Long branchId, Long id) {
        BranchDocuments deletedDocument = branchDocumentRepository.findByIdAndBranch_Id(id, branchId)
                .orElseThrow(() -> new EntityNotFoundException("해당 지점에 해당 문서가 존재하지 않습니다."));

        String url = deletedDocument.getBranchDocumentUrl();
        if (url != null && !url.isBlank()) {
            awsS3Uploader.deleteByUrl(url);
        }

        branchDocumentRepository.delete(deletedDocument);
    }

    // Entity -> DTO
    private BranchDocumentDto toDto(BranchDocuments entity) {
        return BranchDocumentDto.builder()
                .id(entity.getId())
                .branchId(entity.getBranch() != null ? entity.getBranch().getId() : null)
                .branchDocumentType(entity.getBranchDocumentType())
                .title(entity.getTitle())
                .branchDocumentUrl(entity.getBranchDocumentUrl())
                .build();
    }
}
