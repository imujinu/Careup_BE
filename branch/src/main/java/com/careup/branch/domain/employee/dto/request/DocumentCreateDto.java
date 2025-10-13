package com.careup.branch.domain.employee.dto.request;

import com.careup.branch.domain.employee.entity.DocumentType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentCreateDto {
    @NotNull
    private DocumentType documentType;
}
