package com.weather.alert.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private static final String UNKNOWN = "unknown";
    private static final int MAX_CLIENT_IP_LENGTH = 64;

    private final boolean trustForwardedFor;

    public ClientIpResolver(@Value("${app.rate-limit.trust-forwarded-for:true}") boolean trustForwardedFor) {
        this.trustForwardedFor = trustForwardedFor;
    }

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        if (trustForwardedFor) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            String forwardedCandidate = firstValidForwardedAddress(forwardedFor);
            if (forwardedCandidate != null) {
                return forwardedCandidate;
            }

            String realIp = sanitize(request.getHeader("X-Real-IP"));
            if (realIp != null) {
                return realIp;
            }
        }

        String remoteAddr = sanitize(request.getRemoteAddr());
        return remoteAddr != null ? remoteAddr : UNKNOWN;
    }

    private String firstValidForwardedAddress(String forwardedFor) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return null;
        }
        String[] candidates = forwardedFor.split(",");
        for (String candidate : candidates) {
            String sanitized = sanitize(candidate);
            if (sanitized != null) {
                return sanitized;
            }
        }
        return null;
    }

    private String sanitize(String candidate) {
        if (candidate == null) {
            return null;
        }

        String value = candidate.trim();
        if (value.isBlank()) {
            return null;
        }

        if (value.regionMatches(true, 0, "for=", 0, 4)) {
            value = value.substring(4).trim();
        }

        if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
            value = value.substring(1, value.length() - 1);
        }

        if (value.startsWith("[") && value.contains("]")) {
            value = value.substring(1, value.indexOf(']'));
        } else if (hasSinglePortSeparator(value)) {
            value = value.substring(0, value.indexOf(':'));
        }

        if (value.isBlank() || value.length() > MAX_CLIENT_IP_LENGTH) {
            return null;
        }

        return value;
    }

    private boolean hasSinglePortSeparator(String value) {
        int firstColon = value.indexOf(':');
        return firstColon > 0 && firstColon == value.lastIndexOf(':');
    }
}
