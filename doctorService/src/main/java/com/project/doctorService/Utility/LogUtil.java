package com.project.doctorService.Utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class LogUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final int MAX_PAYLOAD_LENGTH = 500;
    private static final String[] SENSITIVE_FIELDS = {"userPassword", "password", "secretKey", "secret"};

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        try {
            String json = objectMapper.writeValueAsString(obj);
            json = maskSensitiveFields(json);
            if (json.length() > MAX_PAYLOAD_LENGTH) {
                return json.substring(0, MAX_PAYLOAD_LENGTH) + "...[truncated]";
            }
            return json;
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private static String maskSensitiveFields(String json) {
        for (String field : SENSITIVE_FIELDS) {
            json = json.replaceAll(
                    "\"" + field + "\"\\s*:\\s*\"[^\"]*\"",
                    "\"" + field + "\":\"***\""
            );
        }
        return json;
    }
}
