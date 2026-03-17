package com.weather.alert.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpResolverTest {

    @Test
    void shouldPreferForwardedForWhenTrusted() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.2");

        assertEquals("203.0.113.7", resolver.resolve(request));
    }

    @Test
    void shouldNormalizeForwardedAddressWithPort() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.7:443");

        assertEquals("203.0.113.7", resolver.resolve(request));
    }

    @Test
    void shouldFallbackToRemoteAddressWhenForwardedHeadersDisabled() {
        ClientIpResolver resolver = new ClientIpResolver(false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.7");

        assertEquals("10.0.0.1", resolver.resolve(request));
    }
}
