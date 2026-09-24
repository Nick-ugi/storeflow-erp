package com.storeflow.user.dto;

import com.storeflow.common.code.ActiveStatus;
import com.storeflow.common.code.Role;
import com.storeflow.common.request.PageParams;
import com.storeflow.common.util.Texts;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UserSearchRequest(
        String keyword,
        Role role,
        Long storeId,
        ActiveStatus status,
        @Min(value = 0, message = PageParams.PAGE_MESSAGE) Integer page,
        @Min(value = 1, message = PageParams.SIZE_MESSAGE) @Max(value = 100, message = PageParams.SIZE_MESSAGE) Integer size)
        implements PageParams {

    public UserSearchRequest {
        keyword = Texts.trimToNull(keyword);
    }

}
