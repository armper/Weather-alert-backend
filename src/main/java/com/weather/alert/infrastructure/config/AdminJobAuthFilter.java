package com.weather.alert.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class AdminJobAuthFilter extends OncePerRequestFilter {

    static final String ADMIN_JOB_TOKEN_HEADER = "X-Admin-Job-Token";
    private final String configuredToken;

    public AdminJobAuthFilter(@Value("${app.admin.jobs.token:}") String configuredToken) {
        this.configuredToken = configuredToken == null ? "" : configuredToken.trim();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/admin/jobs/")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!configuredToken.isEmpty() && SecurityContextHolder.getContext().getAuthentication() == null) {
            String requestToken = request.getHeader(ADMIN_JOB_TOKEN_HEADER);
            if (configuredToken.equals(requestToken)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        "cloud-scheduler",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                authentication.setDetails("admin-job-token");
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            if ("cloud-scheduler".equals(
                    SecurityContextHolder.getContext().getAuthentication() == null
                            ? null
                            : SecurityContextHolder.getContext().getAuthentication().getName())) {
                SecurityContextHolder.clearContext();
            }
        }
    }
}
