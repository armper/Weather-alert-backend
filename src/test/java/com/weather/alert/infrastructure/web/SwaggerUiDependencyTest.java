package com.weather.alert.infrastructure.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SwaggerUiDependencyTest {

    @Test
    void shouldHaveSwaggerUiClassesAvailable() {
        assertDoesNotThrow(() -> Class.forName("org.springdoc.webmvc.ui.SwaggerWelcomeWebMvc"));
    }
}
