package com.project.patientService.Filters;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class FeignCorrelationInterceptor implements RequestInterceptor {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public void apply(RequestTemplate template) {
        String corrId = MDC.get("correlationId");
        if (corrId != null) {
            template.header(CORRELATION_ID_HEADER, corrId);
        }
    }
}
