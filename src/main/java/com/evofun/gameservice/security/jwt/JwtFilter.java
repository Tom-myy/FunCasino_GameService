package com.evofun.gameservice.security.jwt;

import com.evofun.gameservice.common.error.ErrorCode;
import com.evofun.gameservice.common.error.ErrorDto;
import com.evofun.gameservice.common.error.ExceptionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {
    //TODO WHITE_LIST (like in JwtFiler in user-service), but meaningfully
    private static final AntPathMatcher matcher = new AntPathMatcher();
    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();

/*        // 👇 если это WebSocket-запрос — пропускаем без JWT-проверки
        if (path.startsWith("/ws") || path.startsWith("/websocket")) {
            filterChain.doFilter(request, response);
            return;
        }*/
        if (matcher.match("/ws/**", path) || matcher.match("/websocket", path)) {
            filterChain.doFilter(request, response);
            return;
        }


/*        // ❗ Пропускаем открытые пути (например, регистрация и логин)
        if (path.startsWith("/api/game-service/gameAccess") || path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }*/
        if (matcher.match("/api/game-service/**", path) || matcher.match("/actuator/**", path)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars")
                || path.equals("/swagger-ui.html")) {
            logger.debug(">>> Swagger path — skipping JwtFilter");
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");

        // Проверка: есть ли заголовок и начинается ли он с "Bearer "
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            JwtPayload payload;

            try {
                payload = jwtUtil.extractPayload(token);
            } catch (ExpiredJwtException expired) {
                logger.warn("Expired JWT token: {}", expired);
                SecurityContextHolder.clearContext();
                handleErrorResponse(response, "Expired JWT token.");
                return;
            } catch (JwtException e) {
                logger.warn("Invalid JWT token: {}", e);
                SecurityContextHolder.clearContext();
                handleErrorResponse(response, "Invalid JWT token.");
                return;
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(payload.userId(), null, null);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

        } else {
            handleErrorResponse(response, "Missing or malformed Authorization header.");
            return;

        }

        filterChain.doFilter(request, response);
    }

    public void handleErrorResponse(HttpServletResponse response, String reasonPrefix) throws IOException {
        String code = ExceptionUtils.generateErrorId("VAL");
        String message = reasonPrefix + " ERROR-CODE: " + code;

        ErrorDto errorDto = new ErrorDto(ErrorCode.AUTHORIZATION, code, message, null);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");

        String json = new ObjectMapper().writeValueAsString(errorDto);
        response.getWriter().write(json);

        response.flushBuffer();
    }
}
