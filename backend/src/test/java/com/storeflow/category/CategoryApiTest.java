package com.storeflow.category;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.storeflow.common.code.Role;
import com.storeflow.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class CategoryApiTest extends IntegrationTestSupport {

    @Test
    @DisplayName("카테고리 목록은 전 역할이 조회하고 이름순으로 정렬된다. 등록은 ADMIN만 한다")
    void listForAllRolesAndCreateForAdmin() throws Exception {
        insertCategory("하의");
        insertCategory("상의");
        String userToken = tokenFor(Role.USER, insertStore("GN01", "강남점"));

        mockMvc.perform(get("/api/v1/categories").header(HttpHeaders.AUTHORIZATION, bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].categoryName").value("상의"))
                .andExpect(jsonPath("$.data[1].categoryName").value("하의"));

        mockMvc.perform(post("/api/v1/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("categoryName", "잡화")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("카테고리명 중복은 409, 자기 이름 그대로 수정은 허용, 없는 카테고리 수정은 404")
    void duplicateAndUpdate() throws Exception {
        String adminToken = adminToken();
        Long top = insertCategory("상의");
        insertCategory("하의");

        mockMvc.perform(post("/api/v1/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("categoryName", " 상의 ")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_CATEGORY_NAME"));

        mockMvc.perform(put("/api/v1/categories/{id}", top)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("categoryName", "상의", "description", "티셔츠, 셔츠")))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/categories/{id}", top)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("categoryName", "하의")))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/categories/{id}", 999_999)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("categoryName", "없음")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NOT_FOUND"));
    }

}
