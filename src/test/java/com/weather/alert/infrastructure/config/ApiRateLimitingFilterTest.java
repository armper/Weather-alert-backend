package com.weather.alert.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ApiRateLimitingFilterTest {

    @Test
    void shouldReturnTooManyRequestsWhenLimitExceeded() throws Exception {
        ApiRateLimitingFilter filter = new ApiRateLimitingFilter(1, 60);

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
    }

    @Test
    void shouldBypassRateLimitForNonApiPath() throws Exception {
        ApiRateLimitingFilter filter = new ApiRateLimitingFilter(1, 60);

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
}
