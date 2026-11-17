package com.example.floodmonitor.config;

import com.example.floodmonitor.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-API-Key";
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final byte[] viewerKey;
    private final byte[] adminKey;

    public ApiKeyInterceptor(@Value("${api.viewer-key}") String viewer, @Value("${api.admin-key}") String admin,
                             @Value("${station.key}") String station) {
        if (viewer.isBlank() || admin.isBlank() || station.isBlank() || viewer.equals(admin)
                || station.equals(viewer) || station.equals(admin)) {
            throw new IllegalArgumentException("Drei unterschiedliche, nicht leere Schlüssel erforderlich");
        }
        viewerKey = viewer.getBytes(StandardCharsets.UTF_8);
        adminKey = admin.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        response.setHeader("Cache-Control", "no-store");
        String key = request.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            throw ApiException.unauthorized("Header " + HEADER + " fehlt");
        }
        byte[] supplied = key.getBytes(StandardCharsets.UTF_8);
        if (MessageDigest.isEqual(adminKey, supplied)) {
            return true;
        }
        if (!MessageDigest.isEqual(viewerKey, supplied)) {
            throw ApiException.unauthorized("API-Key ist ungültig");
        }
        if (!READ_METHODS.contains(request.getMethod())) {
            throw ApiException.forbidden("Admin-Rechte erforderlich");
        }
        return true;
    }
}
