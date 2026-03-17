package com.weather.alert.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather.alert.infrastructure.web.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiRateLimitingFilterTest {

    @Test
    void shouldReturnTooManyRequestsWhenLimitExceeded() throws Exception {
        ApiRateLimitingFilter filter = new ApiRateLimitingFilter(1, 60, new ClientIpResolver(false), new ObjectMapper());

        MockHttpServletRequest firstRequest = new MockHttpServletRequest("GET", "/api/weather/active");
        firstRequest.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, new MockFilterChain());

        MockHttpServletRequest secondRequest = new MockHttpServletRequest("GET", "/api/weather/active");
        secondRequest.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());

        assertNotEquals(429, firstResponse.getStatus());
        assertEquals(429, secondResponse.getStatus());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON_VALUE, secondResponse.getContentType());
        assertEquals("60", secondResponse.getHeader("Retry-After"));
        assertTrue(secondResponse.getContentAsString().contains("\"errorCode\":\"RATE_LIMIT_EXCEEDED\""));
    }

    @Test
    void shouldBypassRateLimitForNonApiPath() throws Exception {
        ApiRateLimitingFilter filter = new ApiRateLimitingFilter(1, 60, new ClientIpResolver(false), new ObjectMapper());

        MockHttpServletRequest firstRequest = new MockHttpServletRequest("GET", "/actuator/health");
        firstRequest.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, new MockFilterChain());

        MockHttpServletRequest secondRequest = new MockHttpServletRequest("GET", "/actuator/health");
        secondRequest.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());

        assertNotEquals(429, firstResponse.getStatus());
        assertNotEquals(429, secondResponse.getStatus());
    }

    @Test
    void shouldUseForwardedForWhenPresent() throws Exception {
        ApiRateLimitingFilter filter = new ApiRateLimitingFilter(1, 60, new ClientIpResolver(true), new ObjectMapper());

        MockHttpServletRequest firstRequest = new MockHttpServletRequest("GET", "/api/weather/active");
        firstRequest.setRemoteAddr("10.0.0.1");
        firstRequest.addHeader("X-Forwarded-For", "203.0.113.7");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, new MockFilterChain());

        MockHttpServletRequest secondRequest = new MockHttpServletRequest("GET", "/api/weather/active");
        secondRequest.setRemoteAddr("10.0.0.2");
        secondRequest.addHeader("X-Forwarded-For", "203.0.113.7");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());

        assertNotEquals(429, firstResponse.getStatus());
        assertEquals(429, secondResponse.getStatus());
    }
}
