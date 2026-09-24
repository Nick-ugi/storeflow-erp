package com.storeflow;

import com.storeflow.common.config.ClockConfig;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StoreFlowApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(ClockConfig.BUSINESS_ZONE));
        SpringApplication.run(StoreFlowApplication.class, args);
    }

}
