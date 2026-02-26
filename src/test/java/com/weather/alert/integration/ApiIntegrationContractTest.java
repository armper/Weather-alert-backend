package com.weather.alert.integration;

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import com.weather.alert.domain.model.PagedResult;
import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.port.NotificationPort;
import com.weather.alert.domain.port.WeatherDataPort;
import com.weather.alert.domain.port.WeatherDataSearchPort;
import com.weather.alert.domain.service.AlertProcessingService;
import com.weather.alert.infrastructure.adapter.elasticsearch.ElasticsearchWeatherRepository;
import com.weather.alert.infrastructure.adapter.kafka.AlertKafkaConsumer;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.restassured.http.ContentType.JSON;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.user.username=test-user",
        "app.security.user.password=test-user-password",
        "app.security.admin.username=test-admin",
        "app.security.admin.password=test-admin-password",
        "app.security.jwt.secret=test-jwt-signing-secret-with-minimum-length-123",
        "spring.task.scheduling.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "spring.data.elasticsearch.repositories.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration,org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration",
        "management.tracing.enabled=false"
})
class ApiIntegrationContractTest {

    @LocalServerPort
    private int port;

    @MockBean
    private WeatherDataPort weatherDataPort;

    @MockBean
    private WeatherDataSearchPort weatherDataSearchPort;

    @MockBean
    private AlertProcessingService alertProcessingService;

    @MockBean
    private NotificationPort notificationPort;

    @MockBean
    private ElasticsearchWeatherRepository elasticsearchWeatherRepository;

    @MockBean
    private AlertKafkaConsumer alertKafkaConsumer;

    private OpenApiValidationFilter openApiValidationFilter;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        openApiValidationFilter = new OpenApiValidationFilter("http://localhost:" + port + "/v3/api-docs");

        when(weatherDataSearchPort.getActiveWeatherData(anyInt(), anyInt()))
                .thenReturn(PagedResult.<WeatherData>builder()
                        .items(List.of(sampleWeatherData("weather-active-1")))
                        .page(0)
                        .size(5)
                        .totalElements(1)
                        .totalPages(1)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build());

        when(weatherDataPort.fetchCurrentConditions(anyDouble(), anyDouble()))
                .thenReturn(Optional.of(sampleWeatherData("weather-current-1")));
    }

    @Test
    void shouldIssueJwtTokenAndMatchOpenApiContract() {
        given()
                .contentType(JSON)
                .filter(openApiValidationFilter)
                .body(Map.of(
                        "username", "test-admin",
                        "password", "test-admin-password"))
                .when()
                .post("/api/auth/token")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("accessToken", notNullValue())
                .body("tokenType", equalTo("Bearer"))
                .body("expiresIn", greaterThan(0));
    }

    @Test
    void shouldCreateAndReadCriteriaWithOpenApiValidation() {
        String token = issueAdminToken();

        String criteriaId = given()
                .contentType(JSON)
                .header("Authorization", "Bearer " + token)
                .filter(openApiValidationFilter)
                .body(validCriteriaRequest())
                .when()
                .post("/api/criteria")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", notNullValue())
                .body("userId", equalTo("dev-admin"))
                .extract()
                .path("id");

        given()
                .header("Authorization", "Bearer " + token)
                .filter(openApiValidationFilter)
                .when()
                .get("/api/criteria/{criteriaId}", criteriaId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(criteriaId))
                .body("userId", equalTo("dev-admin"))
                .body("temperatureUnit", equalTo("F"));

        given()
                .header("Authorization", "Bearer " + token)
                .filter(openApiValidationFilter)
                .when()
                .delete("/api/criteria/{criteriaId}", criteriaId)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void shouldReturnPaginatedWeatherReadModelWithOpenApiValidation() {
        String token = issueAdminToken();

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("page", 0)
                .queryParam("size", 5)
                .filter(openApiValidationFilter)
                .when()
                .get("/api/weather/active")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("items", hasSize(1))
                .body("page", equalTo(0))
                .body("size", equalTo(5))
                .body("totalElements", equalTo(1));
    }

    @Test
    void shouldReturnCurrentConditionsWithOpenApiValidation() {
        String token = issueAdminToken();

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("latitude", 28.5383)
                .queryParam("longitude", -81.3792)
                .filter(openApiValidationFilter)
                .when()
                .get("/api/weather/conditions/current")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo("weather-current-1"))
                .body("location", equalTo("Orlando"));
    }

    @Test
    void shouldStartAndConfirmEmailVerificationWithOpenApiValidation() {
        String token = issueAdminToken();

        io.restassured.response.ExtractableResponse<io.restassured.response.Response> startResponse = given()
                .contentType(JSON)
                .header("Authorization", "Bearer " + token)
                .filter(openApiValidationFilter)
                .body(Map.of(
                        "channel", "EMAIL",
                        "destination", "test-admin@example.com"))
                .when()
                .post("/api/notifications/verifications/start")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", notNullValue())
                .body("channel", equalTo("EMAIL"))
                .body("status", equalTo("PENDING_VERIFICATION"))
                .body("verificationToken", notNullValue())
                .extract()
                ;

        String verificationId = startResponse.path("id");
        String verificationToken = startResponse.path("verificationToken");

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer " + token)
                .filter(openApiValidationFilter)
                .body(Map.of("token", verificationToken))
                .when()
                .post("/api/notifications/verifications/{verificationId}/confirm", verificationId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("VERIFIED"))
                .body("verifiedAt", notNullValue())
                .body("verificationToken", nullValue());
    }

    @Test
    void shouldManageNotificationPreferencesWithOpenApiValidation() {
        String token = issueAdminToken();

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer " + token)
                .filter(openApiValidationFilter)
                .body(Map.of(
                        "channel", "EMAIL",
                        "destination", "test-admin@example.com"))
                .when()
                .post("/api/notifications/verifications/start")
                .then()
                .statusCode(HttpStatus.OK.value());

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer " + token)
                .filter(openApiValidationFilter)
                .body(Map.of(
                        "enabledChannels", List.of("EMAIL"),
                        "preferredChannel", "EMAIL",
                        "fallbackStrategy", "FIRST_SUCCESS"))
                .when()
                .put("/api/users/me/notification-preferences")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("userId", equalTo("test-admin"))
                .body("preferredChannel", equalTo("EMAIL"));

        String criteriaId = given()
                .contentType(JSON)
                .header("Authorization", "Bearer " + token)
                .filter(openApiValidationFilter)
                .body(validCriteriaRequest())
                .when()
                .post("/api/criteria")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .path("id");

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer " + token)
                .filter(openApiValidationFilter)
                .body(Map.of(
                        "useUserDefaults", false,
                        "enabledChannels", List.of("EMAIL"),
                        "preferredChannel", "EMAIL",
                        "fallbackStrategy", "FIRST_SUCCESS"))
                .when()
                .put("/api/criteria/{criteriaId}/notification-preferences", criteriaId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("criteriaId", equalTo(criteriaId))
                .body("useUserDefaults", equalTo(false))
                .body("preferredChannel", equalTo("EMAIL"));

        given()
                .header("Authorization", "Bearer " + token)
                .filter(openApiValidationFilter)
                .when()
                .get("/api/criteria/{criteriaId}/notification-preferences", criteriaId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("criteriaId", equalTo(criteriaId))
                .body("useUserDefaults", equalTo(false));
    }

    @Test
    void shouldRegisterVerifyApproveAndCreateCriteriaHappyPath() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String username = "user" + unique;
        String email = username + "@example.com";
        String password = "StrongPass123!";

        io.restassured.response.ExtractableResponse<io.restassured.response.Response> registerResponse = given()
                .contentType(JSON)
                .filter(openApiValidationFilter)
                .body(Map.of(
                        "username", username,
                        "password", password,
                        "email", email,
                        "name", "Test User"))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("account.id", equalTo(username))
                .body("account.approvalStatus", equalTo("PENDING_APPROVAL"))
                .body("account.emailVerified", equalTo(false))
                .body("emailVerification.id", notNullValue())
                .body("emailVerification.verificationToken", notNullValue())
                .extract();

        String originalVerificationId = registerResponse.path("emailVerification.id");
        String originalVerificationToken = registerResponse.path("emailVerification.verificationToken");
        org.junit.jupiter.api.Assertions.assertNotNull(originalVerificationId);
        org.junit.jupiter.api.Assertions.assertNotNull(originalVerificationToken);

        io.restassured.response.ExtractableResponse<io.restassured.response.Response> resendResponse = given()
                .contentType(JSON)
                .filter(openApiValidationFilter)
                .body(Map.of("username", username))
                .when()
                .post("/api/auth/register/resend-verification")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", notNullValue())
                .body("verificationToken", notNullValue())
                .extract();

        String verificationId = resendResponse.path("id");
        String verificationToken = resendResponse.path("verificationToken");

        given()
                .contentType(JSON)
                .filter(openApiValidationFilter)
                .body(Map.of(
                        "userId", username,
                        "verificationId", verificationId,
                        "token", verificationToken))
                .when()
                .post("/api/auth/register/verify-email")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(username))
                .body("emailVerified", equalTo(true));

        String adminToken = issueAdminToken();
        given()
                .header("Authorization", "Bearer " + adminToken)
                .filter(openApiValidationFilter)
                .when()
                .post("/api/admin/users/{userId}/approve", username)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo(username))
                .body("approvalStatus", equalTo("ACTIVE"));

        String userToken = given()
                .contentType(JSON)
                .filter(openApiValidationFilter)
                .body(Map.of(
                        "username", username,
                        "password", password))
                .when()
                .post("/api/auth/token")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .path("accessToken");

        String criteriaId = given()
                .contentType(JSON)
                .header("Authorization", "Bearer " + userToken)
                .filter(openApiValidationFilter)
                .body(Map.ofEntries(
                        Map.entry("userId", username),
                        Map.entry("location", "Orlando"),
                        Map.entry("latitude", 28.5383),
                        Map.entry("longitude", -81.3792),
                        Map.entry("temperatureThreshold", 60),
                        Map.entry("temperatureDirection", "BELOW"),
                        Map.entry("temperatureUnit", "F"),
                        Map.entry("monitorCurrent", true),
                        Map.entry("monitorForecast", true),
                        Map.entry("forecastWindowHours", 48),
                        Map.entry("oncePerEvent", true),
                        Map.entry("rearmWindowMinutes", 120)))
                .when()
                .post("/api/criteria")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("userId", equalTo(username))
                .extract()
                .path("id");

        given()
                .header("Authorization", "Bearer " + userToken)
                .filter(openApiValidationFilter)
                .when()
                .delete("/api/criteria/{criteriaId}", criteriaId)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    private String issueAdminToken() {
        return given()
                .contentType(JSON)
                .body(Map.of(
                        "username", "test-admin",
                        "password", "test-admin-password"))
                .when()
                .post("/api/auth/token")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .path("accessToken");
    }

    private Map<String, Object> validCriteriaRequest() {
        return Map.ofEntries(
                Map.entry("userId", "dev-admin"),
                Map.entry("location", "Orlando"),
                Map.entry("latitude", 28.5383),
                Map.entry("longitude", -81.3792),
                Map.entry("temperatureThreshold", 60),
                Map.entry("temperatureDirection", "BELOW"),
                Map.entry("temperatureUnit", "F"),
                Map.entry("rainThreshold", 40),
                Map.entry("rainThresholdType", "PROBABILITY"),
                Map.entry("monitorCurrent", true),
                Map.entry("monitorForecast", true),
                Map.entry("forecastWindowHours", 48),
                Map.entry("oncePerEvent", true),
                Map.entry("rearmWindowMinutes", 120)
        );
    }

    private WeatherData sampleWeatherData(String id) {
        return WeatherData.builder()
                .id(id)
                .location("Orlando")
                .latitude(28.5383)
                .longitude(-81.3792)
                .eventType("Rain")
                .severity("MODERATE")
                .headline("Light rain expected")
                .description("Sample weather payload used for API integration tests")
                .onset(Instant.now().plusSeconds(300))
                .expires(Instant.now().plusSeconds(7200))
                .temperature(18.0)
                .windSpeed(12.5)
                .precipitationProbability(50.0)
                .precipitationAmount(1.2)
                .humidity(80.0)
                .timestamp(Instant.now())
                .build();
    }
}
