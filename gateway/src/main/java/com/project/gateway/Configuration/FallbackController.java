package com.project.gateway.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Map;

@RestController
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);

    @RequestMapping("/fallback")
    public ResponseEntity<Map<String, String>> fallback(ServerWebExchange exchange) {
        String corrId = exchange.getRequest().getHeaders().getFirst("X-Correlation-ID");
        LinkedHashSet<URI> originalUrls = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
        String originalPath = originalUrls != null && !originalUrls.isEmpty()
                ? originalUrls.iterator().next().getPath()
                : exchange.getRequest().getURI().getPath();
        log.warn("action=CIRCUIT_BREAKER status=OPEN corrId={} originalPath={} detail=Fallback triggered service unavailable", corrId, originalPath);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "Service is currently unavailable. Please try again later."));
    }
}
