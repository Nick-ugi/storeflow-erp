package com.storeflow.product.service;

import com.storeflow.category.service.CategoryService;
import com.storeflow.common.code.ActiveStatus;
import com.storeflow.common.exception.BusinessException;
import com.storeflow.common.exception.ErrorCode;
import com.storeflow.common.response.PageResponse;
import com.storeflow.common.response.StatusResponse;
import com.storeflow.common.security.LoginUser;
import com.storeflow.product.domain.ProductSnapshot;
import com.storeflow.product.dto.ProductCreateRequest;
import com.storeflow.product.dto.ProductDetailResponse;
import com.storeflow.product.dto.ProductResponse;
import com.storeflow.product.dto.ProductSearchRequest;
import com.storeflow.product.dto.ProductUpdateRequest;
import com.storeflow.product.mapper.ProductMapper;
import com.storeflow.stock.service.StockRowInitializer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 (기준정보 명세 3장). 상품은 전 매장 공통이며 등록 · 수정은 ADMIN만 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductMapper productMapper;
    private final CategoryService categoryService;
    private final StockRowInitializer stockRowInitializer;

    public PageResponse<ProductResponse> search(LoginUser loginUser, ProductSearchRequest cond) {
        Long stockStoreId = loginUser.scopeStoreId(cond.storeId());
        List<ProductResponse> content = productMapper.findProducts(cond, stockStoreId, cond.offset(), cond.pageSize());
        return PageResponse.of(content, cond, productMapper.countProducts(cond));
    }

    public ProductDetailResponse get(Long id) {
        ProductDetailResponse product = productMapper.findById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    /**
     * 판매 · 발주에 쓸 상품을 한 번에 읽는다. 없는 상품은 404, 사용 중지 상품은 409 (BR-023)
     *
     * @param action 오류 메시지의 동작 이름 ("판매", "발주")
     */
    public Map<Long, ProductSnapshot> requireAvailable(Collection<Long> productIds, String action) {
        Map<Long, ProductSnapshot> products = productMapper.findSnapshots(productIds).stream()
                .collect(Collectors.toMap(ProductSnapshot::getId, Function.identity()));
        for (Long productId : productIds) {
            ProductSnapshot product = products.get(productId);
            if (product == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            if (product.getStatus() != ActiveStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE,
                        "%s할 수 없는 상품이 포함되어 있습니다: %s".formatted(action, product.getProductName()));
            }
        }
        return products;
    }

    /**
     * 상품을 등록하고 모든 매장의 재고 행(수량 0)을 함께 만든다.
     */
    @Transactional
    public Long create(ProductCreateRequest request) {
        stockRowInitializer.lockRegistration();
        if (productMapper.existsByProductCode(request.productCode())) {
            throw new BusinessException(ErrorCode.DUPLICATE_PRODUCT_CODE);
        }
        categoryService.requireExists(request.categoryId());
        Long productId = productMapper.insert(request);
        stockRowInitializer.createForProduct(productId);
        return productId;
    }

    /** 판매가를 바꿔도 이미 완료된 판매 금액은 판매 상세의 단가로 유지된다. (BR-041) */
    @Transactional
    public void update(Long id, ProductUpdateRequest request) {
        get(id);
        categoryService.requireExists(request.categoryId());
        productMapper.update(id, request);
    }

    @Transactional
    public StatusResponse<ActiveStatus> changeStatus(Long id, ActiveStatus status) {
        get(id);
        productMapper.updateStatus(id, status);
        return new StatusResponse<>(id, status);
    }

}
