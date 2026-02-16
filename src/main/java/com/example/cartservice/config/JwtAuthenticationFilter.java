package com.example.cartservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Filter that relies on identity headers injected by the API Gateway.
 * This avoids redundant JWT validation in this microservice.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Extract identity headers injected by the Gateway
        String userIdStr = request.getHeader("X-User-Id");
        String roleName = request.getHeader("X-Role");

        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdStr);

                // If role is missing, default to USER
                String finalRole = (roleName != null && !roleName.isEmpty()) ? roleName : "USER";

                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + finalRole));

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userId, null,
                        authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authenticated user {} with role {} via headers", userId, finalRole);

            } catch (NumberFormatException e) {
                log.warn("Invalid X-User-Id header format: {}", userIdStr);
            }
        }

        filterChain.doFilter(request, response);
    }
}
