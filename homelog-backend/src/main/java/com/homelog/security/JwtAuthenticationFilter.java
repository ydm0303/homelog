package com.homelog.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT 认证过滤器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            if (!jwtTokenProvider.isRefreshToken(token)) {
                Authentication auth = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("JWT authenticated: username={}, uri={}", auth.getName(), request.getRequestURI());
            } else {
                log.debug("Refresh token presented, skipping authentication: uri={}", request.getRequestURI());
            }
        } else if (StringUtils.hasText(token)) {
            log.debug("JWT token invalid or expired: uri={}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头解析 Token
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtTokenProvider.getTokenHeader());
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(jwtTokenProvider.getTokenPrefix())) {
            return bearerToken.substring(jwtTokenProvider.getTokenPrefix().length());
        }
        return null;
    }
}