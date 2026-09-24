package com.storeflow.common.numbering;

import com.storeflow.common.config.ClockConfig;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 판매번호 · 발주번호 채번 — 코드 정의서 3장
 * {@code S-{매장코드}-{YYYYMMDD}-{순번 6자리}}, 순번은 DB 시퀀스(전 매장 공통, 날짜별 초기화 없음).
 * 채번 때문에 다른 판매가 기다리는 잠금이 없으며, 롤백된 번호는 건너뛴다.
 */
@Component
@RequiredArgsConstructor
public class DocumentNumberGenerator {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DocumentSequenceMapper documentSequenceMapper;

    public String saleNumber(String storeCode, OffsetDateTime registeredAt) {
        return format("S", storeCode, registeredAt, documentSequenceMapper.nextSaleSequence());
    }

    public String purchaseOrderNumber(String storeCode, OffsetDateTime registeredAt) {
        return format("PO", storeCode, registeredAt, documentSequenceMapper.nextPurchaseOrderSequence());
    }

    private static String format(String prefix, String storeCode, OffsetDateTime registeredAt, long sequence) {
        String date = registeredAt.atZoneSameInstant(ClockConfig.BUSINESS_ZONE).format(DATE);
        return "%s-%s-%s-%06d".formatted(prefix, storeCode, date, sequence);
    }

}
