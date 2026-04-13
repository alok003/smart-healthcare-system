package com.project.gateway.Filters;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class JwtFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Value("${app.secret-key}")
    private String SECRET_KEY;

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String corrId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (corrId == null || corrId.isBlank()) {
            corrId = UUID.randomUUID().toString();
        }

        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();
        String clientIp = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";

        final String finalCorrId = corrId;

        log.info("action=REQUEST_RECEIVED method={} path={} corrId={} clientIp={}", method, path, finalCorrId, clientIp);

        ServerHttpRequest requestWithCorrId = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, finalCorrId)
                .build();
        ServerWebExchange exchangeWithCorrId = exchange.mutate().request(requestWithCorrId).build();

        if (path.matches(".*/open/.*") || path.matches(".*/v3/api-docs.*") || path.matches(".*/swagger-ui.*")) {
            log.debug("action=JWT_VALIDATION status=SKIPPED path={} corrId={} reason=PUBLIC_OR_DOCS_ENDPOINT", path, finalCorrId);
            return chain.filter(exchangeWithCorrId)
                    .doFinally(sig -> logResponse(exchangeWithCorrId, finalCorrId));
        }

        if (path.matches(".*/secure/.*")) {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("action=JWT_VALIDATION status=FAILED path={} corrId={} reason=MISSING_OR_INVALID_AUTH_HEADER", path, finalCorrId);
                return onError(exchangeWithCorrId, HttpStatus.UNAUTHORIZED);
            }

            try {
                String token = authHeader.substring(7);
                Claims claims = Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String email = claims.getSubject();
                String role = claims.get("role", String.class);

                log.debug("action=JWT_VALIDATION status=SUCCESS path={} corrId={} requestedBy={} role={}", path, finalCorrId, email, role);

                ServerHttpRequest mutatedRequest = exchangeWithCorrId.getRequest().mutate()
                        .header("X-User-Email", email)
                        .header("X-User-Role", role)
                        .build();

                ServerWebExchange mutatedExchange = exchangeWithCorrId.mutate().request(mutatedRequest).build();

                log.info("action=REQUEST_FORWARDED method={} path={} corrId={} requestedBy={} role={}", method, path, finalCorrId, email, role);

                return chain.filter(mutatedExchange)
                        .doFinally(sig -> logResponse(mutatedExchange, finalCorrId));

            } catch (JwtException | IllegalArgumentException e) {
                log.warn("action=JWT_VALIDATION status=FAILED path={} corrId={} reason=INVALID_TOKEN detail={}", path, finalCorrId, e.getMessage());
                return onError(exchangeWithCorrId, HttpStatus.UNAUTHORIZED);
            }
        }

        return chain.filter(exchangeWithCorrId)
                .doFinally(sig -> logResponse(exchangeWithCorrId, finalCorrId));
    }

    private void logResponse(ServerWebExchange exchange, String corrId) {
        ServerHttpResponse response = exchange.getResponse();
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();
        int status = response.getStatusCode() != null ? response.getStatusCode().value() : 0;
        if (status >= 500) {
            log.error("action=REQUEST_COMPLETED method={} path={} corrId={} status={}", method, path, corrId, status);
        } else if (status >= 400) {
            log.warn("action=REQUEST_COMPLETED method={} path={} corrId={} status={}", method, path, corrId, status);
        } else {
            log.info("action=REQUEST_COMPLETED method={} path={} corrId={} status={}", method, path, corrId, status);
        }
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}
