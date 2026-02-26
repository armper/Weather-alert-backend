package com.weather.alert.infrastructure.error;

import com.weather.alert.application.exception.ApiException;
import com.weather.alert.domain.service.notification.InvalidNotificationPreferenceConfigurationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException ex, HttpServletRequest request) {
        ProblemDetail problem = buildProblem(
                ex.getStatus(),
                ex.getErrorCode(),
                ex.getMessage(),
                request,
                null
        );
        return ResponseEntity.status(ex.getStatus()).body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        List<Map<String, String>> errors = ex.getConstraintViolations().stream()
                .map(violation -> Map.of(
                        "field", violation.getPropertyPath().toString(),
                        "message", violation.getMessage()))
                .toList();

        ProblemDetail problem = buildProblem(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed",
                request,
                errors
        );
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String detail = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());
        ProblemDetail problem = buildProblem(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                detail,
                request,
                null
        );
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(InvalidNotificationPreferenceConfigurationException.class)
    public ResponseEntity<ProblemDetail> handleInvalidNotificationPreferenceConfiguration(
            InvalidNotificationPreferenceConfigurationException ex,
            HttpServletRequest request) {

        ProblemDetail problem = buildProblem(
                HttpStatus.BAD_REQUEST,
                "INVALID_NOTIFICATION_PREFERENCE_CONFIGURATION",
                ex.getMessage(),
                request,
                null
        );
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<ProblemDetail> handleEmptyResultDataAccessException(
            EmptyResultDataAccessException ex,
            HttpServletRequest request) {

        ProblemDetail problem = buildProblem(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "Requested resource was not found",
                request,
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = buildProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                request,
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    String field = "AssertTrue".equals(error.getCode()) ? "request" : error.getField();
                    item.put("field", field);
                    item.put("message", error.getDefaultMessage());
                    return item;
                })
                .toList();

        List<Map<String, String>> globalErrors = ex.getBindingResult().getGlobalErrors().stream()
                .map(error -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("field", "request");
                    item.put("message", error.getDefaultMessage());
                    return item;
                })
                .toList();

        List<Map<String, String>> combinedErrors = Stream.concat(errors.stream(), globalErrors.stream()).toList();

        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
        ProblemDetail problem = buildProblem(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed",
                servletRequest,
                combinedErrors
        );

        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request) {

        if (body instanceof ProblemDetail problemDetail) {
            return super.handleExceptionInternal(ex, problemDetail, headers, statusCode, request);
        }

        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
        ProblemDetail problem = buildProblem(
                HttpStatus.valueOf(statusCode.value()),
                "HTTP_ERROR",
                ex.getMessage() != null ? ex.getMessage() : "Request failed",
                servletRequest,
                null
        );

        return super.handleExceptionInternal(ex, problem, headers, statusCode, request);
    }

    private ProblemDetail buildProblem(
            HttpStatus status,
            String errorCode,
            String detail,
            HttpServletRequest request,
            List<Map<String, String>> errors) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("https://weather-alert-backend/errors/" + errorCode.toLowerCase()));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("errorCode", errorCode);
        problem.setProperty("path", request.getRequestURI());
        problem.setProperty("traceId", resolveTraceId());
        problem.setProperty("correlationId", resolveCorrelationId());

        if (errors != null && !errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }

        return problem;
    }

    private String resolveTraceId() {
        return valueOrDefault(MDC.get("traceId"), "n/a");
    }

    private String resolveCorrelationId() {
        return valueOrDefault(MDC.get(CorrelationIdFilter.MDC_KEY), "n/a");
    }

    private String valueOrDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
