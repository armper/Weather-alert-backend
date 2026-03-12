package com.weather.alert.integration;

import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import com.weather.alert.application.dto.BillingCheckoutSessionResponse;
import com.weather.alert.application.dto.BillingStatusResponse;
import com.weather.alert.domain.model.BillingPlan;
import com.weather.alert.application.dto.JobRunResponse;
import com.weather.alert.application.usecase.CreateBillingCheckoutSessionUseCase;
import com.weather.alert.application.usecase.GetBillingStatusUseCase;
import com.weather.alert.application.usecase.PublishDueAlertDeliveryTasksUseCase;
import com.weather.alert.application.usecase.RunDataRetentionCleanupUseCase;
import com.weather.alert.application.usecase.RunWeatherAlertProcessingUseCase;
import com.weather.alert.domain.model.HydrologyQuery;
import com.weather.alert.domain.model.PagedResult;
import com.weather.alert.domain.model.WeatherData;
import com.weather.alert.domain.port.AlertDeliveryDlqPublisherPort;
import com.weather.alert.domain.port.AlertDeliveryTaskPublisherPort;
import com.weather.alert.domain.port.AlertCriteriaRepositoryPort;
import com.weather.alert.domain.port.NotificationPort;
import com.weather.alert.domain.port.SmsSenderPort;
import com.weather.alert.domain.port.WeatherDataPort;
import com.weather.alert.domain.port.WeatherDataSearchPort;
import com.weather.alert.domain.service.AlertProcessingService;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
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
import static org.mockito.ArgumentMatchers.any;
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
        "app.admin.jobs.token=test-admin-jobs-token",
        "spring.task.scheduling.enabled=false",
        "management.tracing.enabled=false"
})
class ApiIntegrationContractTest {

    @LocalServerPort
    private int port;

    @Autowired
    private AlertCriteriaRepositoryPort alertCriteriaRepository;

    @MockBean
    private WeatherDataPort weatherDataPort;

    @MockBean
    private WeatherDataSearchPort weatherDataSearchPort;

    @MockBean
    private AlertProcessingService alertProcessingService;

    @MockBean
    private NotificationPort notificationPort;

    @MockBean
    private SmsSenderPort smsSenderPort;

    @MockBean
    private AlertDeliveryTaskPublisherPort alertDeliveryTaskPublisherPort;

    @MockBean
    private AlertDeliveryDlqPublisherPort alertDeliveryDlqPublisherPort;

    @MockBean
    private RunWeatherAlertProcessingUseCase runWeatherAlertProcessingUseCase;

    @MockBean
    private PublishDueAlertDeliveryTasksUseCase publishDueAlertDeliveryTasksUseCase;

    @MockBean
    private RunDataRetentionCleanupUseCase runDataRetentionCleanupUseCase;

    @MockBean
    private GetBillingStatusUseCase getBillingStatusUseCase;

    @MockBean
    private CreateBillingCheckoutSessionUseCase createBillingCheckoutSessionUseCase;

    private OpenApiValidationFilter openApiValidationFilter;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        openApiValidationFilter = new OpenApiValidationFilter("http://localhost:" + port + "/v3/api-docs");
        deleteCriteriaForUser("test-admin");
        deleteCriteriaForUser("dev-admin");

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

        when(weatherDataPort.fetchForecastConditions(anyDouble(), anyDouble(), anyInt()))
                .thenReturn(List.of(WeatherData.builder()
                        .id("weather-forecast-1")
                        .location("Orlando")
                        .eventType("FORECAST_CONDITIONS")
                        .headline("Cloudy and humid")
                        .description("Forecast grid data enriched period")
                        .temperature(16.0)
                        .humidity(86.0)
                        .dewPoint(19.0)
                        .windSpeed(24.0)
                        .windGust(51.0)
                        .skyCover(94.0)
                        .precipitationProbability(65.0)
                        .precipitationAmount(2.5)
                        .onset(Instant.parse("2026-03-06T15:00:00Z"))
                        .expires(Instant.parse("2026-03-06T16:00:00Z"))
                        .timestamp(Instant.parse("2026-03-06T14:00:00Z"))
                        .build()));

        when(weatherDataPort.fetchHydrologyCurrentConditions(any(HydrologyQuery.class)))
                .thenReturn(Optional.of(WeatherData.builder()
                        .id("river-current-1")
                        .location("Flint River (GA) at Albany, GA")
                        .eventType("RIVER_CURRENT_CONDITIONS")
                        .riverGaugeId("ABNG1")
                        .riverObservedStage(26.8)
                        .riverForecastStage(31.4)
                        .riverFloodStage(26.0)
                        .riverActionStage(16.0)
                        .riverObservedCategory("minor")
                        .riverForecastCategory("moderate")
                        .riverStageUnit("ft")
                        .timestamp(Instant.parse("2026-03-06T21:15:00Z"))
                        .build()));

        when(weatherDataPort.fetchHydrologyForecastConditions(any(HydrologyQuery.class)))
                .thenReturn(Optional.of(WeatherData.builder()
                        .id("river-forecast-1")
                        .location("Flint River (GA) at Albany, GA")
                        .eventType("RIVER_FORECAST_CONDITIONS")
                        .riverGaugeId("ABNG1")
                        .riverObservedStage(26.8)
                        .riverForecastStage(31.4)
                        .riverFloodStage(26.0)
                        .riverActionStage(16.0)
                        .riverObservedCategory("minor")
                        .riverForecastCategory("moderate")
                        .riverStageUnit("ft")
                        .timestamp(Instant.parse("2026-03-07T00:00:00Z"))
                        .build()));

        when(runWeatherAlertProcessingUseCase.run())
                .thenReturn(jobResponse("weather-processing"));
        when(publishDueAlertDeliveryTasksUseCase.run())
                .thenReturn(jobResponse("alert-delivery-retries"));
        when(runDataRetentionCleanupUseCase.run())
                .thenReturn(jobResponse("data-retention"));

        when(getBillingStatusUseCase.getForUser("test-admin"))
                .thenReturn(BillingStatusResponse.builder()
                        .userId("test-admin")
                        .plan(BillingPlan.PLUS)
                        .paidPlan(true)
                        .maxActiveAlerts(10)
                        .adSponsoredEmails(false)
                        .stripeCustomerId("cus_test_admin")
                        .stripeSubscriptionId("sub_test_admin")
                        .stripePriceId("price_test_weather_alerts")
                        .stripeSubscriptionStatus("active")
                        .stripeCurrentPeriodEnd(Instant.parse("2026-04-01T00:00:00Z"))
                        .activeSubscription(true)
                        .build());

        when(createBillingCheckoutSessionUseCase.createForUser("test-admin", null))
                .thenReturn(BillingCheckoutSessionResponse.builder()
                        .sessionId("cs_test_weather_alerts")
                        .url("https://checkout.stripe.com/c/pay/cs_test_weather_alerts")
                        .build());
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
    void shouldReturnBillingStatusWithOpenApiValidation() {
        String token = issueAdminToken();

        given()
                .header("Authorization", "Bearer " + token)
                .filter(openApiValidationFilter)
                .when()
                .get("/api/billing/me")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("userId", equalTo("test-admin"))
                .body("stripeCustomerId", equalTo("cus_test_admin"))
                .body("activeSubscription", equalTo(true));
    }

    @Test
    void shouldCreateBillingCheckoutSessionWithOpenApiValidation() {
        String token = issueAdminToken();

        given()
                .contentType(JSON)
                .header("Authorization", "Bearer " + token)
                .filter(openApiValidationFilter)
                .body(Map.of())
                .when()
                .post("/api/billing/checkout-session")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("sessionId", equalTo("cs_test_weather_alerts"))
                .body("url", equalTo("https://checkout.stripe.com/c/pay/cs_test_weather_alerts"));
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
                .body("name", equalTo("Orlando Temp + Rain"))
                .body("userId", equalTo("test-admin"))
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
                .body("name", equalTo("Orlando Temp + Rain"))
                .body("userId", equalTo("test-admin"))
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
    void shouldReturnForecastConditionsWithAdvancedFieldsAndOpenApiValidation() {
        String token = issueAdminToken();

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("latitude", 28.5383)
                .queryParam("longitude", -81.3792)
                .queryParam("hours", 48)
                .filter(openApiValidationFilter)
                .when()
                .get("/api/weather/conditions/forecast")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("[0].id", equalTo("weather-forecast-1"))
                .body("[0].dewPoint", equalTo(19.0f))
                .body("[0].windGust", equalTo(51.0f))
                .body("[0].skyCover", equalTo(94.0f))
                .body("[0].humidity", equalTo(86.0f));
    }

    @Test
    void shouldReturnHydrologyCurrentConditionsWithOpenApiValidation() {
        String token = issueAdminToken();

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("gaugeId", "ABNG1")
                .filter(openApiValidationFilter)
                .when()
                .get("/api/weather/hydrology/current")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("id", equalTo("river-current-1"))
                .body("riverGaugeId", equalTo("ABNG1"))
                .body("riverObservedStage", equalTo(26.8f))
                .body("riverFloodStage", equalTo(26.0f))
                .body("riverObservedCategory", equalTo("minor"));
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
    void shouldRunAdminJobsWithOpenApiValidation() {
        String token = issueAdminToken();

        given()
                .header("Authorization", "Bearer " + token)
                .filter(openApiValidationFilter)
                .when()
                .post("/api/admin/jobs/weather-processing")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("jobName", equalTo("weather-processing"))
                .body("status", equalTo("COMPLETED"));

        given()
                .header("Authorization", "Bearer " + token)
                .filter(openApiValidationFilter)
                .when()
                .post("/api/admin/jobs/alert-delivery-retries")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("jobName", equalTo("alert-delivery-retries"))
                .body("status", equalTo("COMPLETED"));

        given()
                .header("Authorization", "Bearer " + token)
                .filter(openApiValidationFilter)
                .when()
                .post("/api/admin/jobs/data-retention")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("jobName", equalTo("data-retention"))
                .body("status", equalTo("COMPLETED"));
    }

    @Test
    void shouldRunAdminJobsWithSharedToken() {
        given()
                .header("X-Admin-Job-Token", "test-admin-jobs-token")
                .filter(openApiValidationFilter)
                .when()
                .post("/api/admin/jobs/weather-processing")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("jobName", equalTo("weather-processing"))
                .body("status", equalTo("COMPLETED"));
    }

    @Test
    void shouldRegisterVerifyAndCreateCriteriaHappyPath() {
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
                .body("account.approvalStatus", equalTo("ACTIVE"))
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
                        Map.entry("name", "New User First Alert"),
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
                .body("name", equalTo("New User First Alert"))
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

    @Test
    void shouldRecoverUsernameAndResetPasswordWithOpenApiValidation() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String username = "recover" + unique;
        String email = username + "@example.com";
        String originalPassword = "StrongPass123!";
        String newPassword = "EvenStrongerPass123!";

        io.restassured.response.ExtractableResponse<io.restassured.response.Response> registerResponse = given()
                .contentType(JSON)
                .filter(openApiValidationFilter)
                .body(Map.of(
                        "username", username,
                        "password", originalPassword,
                        "email", email,
                        "name", "Recover User"))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract();

        String verificationId = registerResponse.path("emailVerification.id");
        String verificationToken = registerResponse.path("emailVerification.verificationToken");

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
                .body("emailVerified", equalTo(true));

        io.restassured.response.ExtractableResponse<io.restassured.response.Response> usernameRecoveryRequest = given()
                .contentType(JSON)
                .filter(openApiValidationFilter)
                .body(Map.of("email", email))
                .when()
                .post("/api/auth/recovery/username/request")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("message", notNullValue())
                .body("recoveryId", notNullValue())
                .body("recoveryCode", notNullValue())
                .extract();

        given()
                .contentType(JSON)
                .filter(openApiValidationFilter)
                .body(Map.of(
                        "recoveryId", usernameRecoveryRequest.path("recoveryId"),
                        "code", usernameRecoveryRequest.path("recoveryCode")))
                .when()
                .post("/api/auth/recovery/username/confirm")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("username", equalTo(username));

        io.restassured.response.ExtractableResponse<io.restassured.response.Response> passwordRecoveryRequest = given()
                .contentType(JSON)
                .filter(openApiValidationFilter)
                .body(Map.of("usernameOrEmail", username))
                .when()
                .post("/api/auth/recovery/password/request")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("message", notNullValue())
                .body("recoveryId", notNullValue())
                .body("recoveryCode", notNullValue())
                .extract();

        given()
                .contentType(JSON)
                .filter(openApiValidationFilter)
                .body(Map.of(
                        "recoveryId", passwordRecoveryRequest.path("recoveryId"),
                        "code", passwordRecoveryRequest.path("recoveryCode"),
                        "newPassword", newPassword))
                .when()
                .post("/api/auth/recovery/password/confirm")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("message", equalTo("Password updated successfully."));

        given()
                .contentType(JSON)
                .filter(openApiValidationFilter)
                .body(Map.of(
                        "username", username,
                        "password", newPassword))
                .when()
                .post("/api/auth/token")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("accessToken", notNullValue());
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

    private void deleteCriteriaForUser(String userId) {
        alertCriteriaRepository.findByUserId(userId).stream()
                .map(criteria -> criteria.getId())
                .forEach(alertCriteriaRepository::delete);
    }

    private Map<String, Object> validCriteriaRequest() {
        return Map.ofEntries(
                Map.entry("userId", "test-admin"),
                Map.entry("name", "Orlando Temp + Rain"),
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

    private JobRunResponse jobResponse(String jobName) {
        Instant now = Instant.parse("2026-03-09T12:00:00Z");
        return JobRunResponse.builder()
                .jobName(jobName)
                .status("COMPLETED")
                .startedAt(now)
                .finishedAt(now.plusSeconds(1))
                .durationMillis(1000)
                .message("test trigger")
                .metrics(Map.of("runs", 1L))
                .build();
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
