package com.zkp.my12306.ntc.config;

import com.zkp.my12306.ntc.dto.ErrorResponseDto;
import com.zkp.my12306.ntc.service.AuthSessionTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SessionIdValidationFilter extends OncePerRequestFilter {
    public static final String SESSION_ID_HEADER = "X-Session-Id";

    private final AuthSessionTokenService authSessionTokenService;

    public SessionIdValidationFilter(AuthSessionTokenService authSessionTokenService) {
        this.authSessionTokenService = authSessionTokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.startsWith("/api/")
                || "/api/auth/login".equals(path)
                || "/api/auth/register".equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String sessionId = request.getHeader(SESSION_ID_HEADER);
        if (sessionId == null || sessionId.isBlank()) {
            writeUnauthorized(response, "sessionId不能为空");
            return;
        }
        if (!authSessionTokenService.isValid(sessionId)) {
            writeUnauthorized(response, "sessionId无效或已过期");
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null
                && !"anonymousUser".equals(authentication.getName())) {
            String tokenUsername = authSessionTokenService.getUsername(sessionId);
            if (tokenUsername == null || !authentication.getName().equals(tokenUsername)) {
                writeUnauthorized(response, "sessionId与当前用户不匹配");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + new ErrorResponseDto(message).message() + "\"}");
    }
}
