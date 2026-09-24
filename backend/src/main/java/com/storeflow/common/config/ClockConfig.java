package com.storeflow.common.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 업무 날짜 계산(오늘, 기본 조회 기간 등)은 서버 시간대와 관계없이 한국 시간을 기준으로 한다. (BR-006)
 */
@Configuration
public class ClockConfig {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(BUSINESS_ZONE);
    }

}
