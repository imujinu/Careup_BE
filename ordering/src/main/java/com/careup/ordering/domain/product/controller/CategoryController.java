package com.careup.ordering.domain.product.controller;

import com.careup.ordering.common.dto.ResponseDto;
import com.careup.ordering.domain.product.dto.CategoryRequestDto;
import com.careup.ordering.domain.product.dto.CategoryResponseDto;
import com.careup.ordering.domain.product.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 카테고리 등록
     */
    @PostMapping
    public ResponseEntity<ResponseDto<CategoryResponseDto>> createCategory(
            @Valid @RequestBody CategoryRequestDto requestDto) {
        log.info("POST /api/categories - 카테고리 등록 요청");

        CategoryResponseDto response = categoryService.createCategory(requestDto);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.CREATED), HttpStatus.CREATED);
    }

    /**
     * 전체 카테고리 조회
     */
    @GetMapping
    public ResponseEntity<ResponseDto<List<CategoryResponseDto>>> getAllCategories() {
        log.info("GET /api/categories - 전체 카테고리 조회");

        List<CategoryResponseDto> response = categoryService.getAllCategories();

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 카테고리 단건 조회
     */
    @GetMapping("/{categoryId}")
    public ResponseEntity<ResponseDto<CategoryResponseDto>> getCategory(
            @PathVariable Long categoryId) {
        log.info("GET /api/categories/{} - 카테고리 조회", categoryId);

        CategoryResponseDto response = categoryService.getCategoryById(categoryId);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 카테고리 삭제
     */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ResponseDto<Void>> deleteCategory(
            @PathVariable Long categoryId) {
        log.info("DELETE /api/categories/{} - 카테고리 삭제", categoryId);

        categoryService.deleteCategory(categoryId);

        return new ResponseEntity<>(ResponseDto.ok(null, HttpStatus.OK), HttpStatus.OK);
    }
}
