package com.weather.alert;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class WeatherAlertApplicationTests {

    @Test
    void applicationMainMethodExists() {
        // Verify that the main application class exists and can be instantiated
        WeatherAlertApplication app = new WeatherAlertApplication();
        assertNotNull(app);
    }
}
