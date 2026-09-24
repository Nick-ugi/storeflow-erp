package com.storeflow.supplier.dto;

import com.storeflow.common.util.Texts;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 공급처 등록 · 수정 (API-SUPP-004, 005). 사업자등록번호는 하이픈을 제거해 숫자만 저장한다.
 */
public record SupplierRequest(
        @NotBlank(message = "공급처명을 입력하세요.")
        @Size(max = 100, message = "공급처명은 100자 이하로 입력하세요.") String supplierName,
        @NotBlank(message = "사업자등록번호를 입력하세요.")
        @Pattern(regexp = "^[0-9]{10}$", message = "사업자등록번호는 숫자 10자리로 입력하세요.") String businessNumber,
        @Size(max = 50, message = "담당자명은 50자 이하로 입력하세요.") String contactName,
        @Pattern(regexp = "^[0-9-]{0,20}$", message = "연락처는 숫자와 하이픈(-)으로 20자 이하로 입력하세요.") String phone) {

    public SupplierRequest {
        supplierName = Texts.trim(supplierName);
        businessNumber = businessNumber == null ? null : Texts.trim(businessNumber).replace("-", "");
        contactName = Texts.trimToNull(contactName);
        phone = Texts.trimToNull(phone);
    }

}
