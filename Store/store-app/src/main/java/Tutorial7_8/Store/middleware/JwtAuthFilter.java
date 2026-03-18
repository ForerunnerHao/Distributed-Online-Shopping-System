package Tutorial7_8.Store.middleware;

import Tutorial7_8.Store.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements Filter {
    private final JwtService jwtService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    //
    private static final List<String> PUBLIC_PATTERNS = List.of(
            "/api/tests/delivery",
            "/api/users/login",
            "/api/users/signup",
            "/api/items/**",
            "/api/payments/callback",
            "/api/payment/refund/callback",
            "/docs",
            "/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var req = (HttpServletRequest) request;
        var res = (HttpServletResponse) response;

        // Allow CORS preflight without authentication
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // step1: check the request's URI
        String path = req.getRequestURI();
        if (isPublic(path) || !path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        // step2: get the jwt from cookie or 'Bearer' header
        String token = resolveToken(req);
        if (token == null || token.isBlank()) {
            unauthorized(res, "missing_token");
            return;
        }

        // step3: verified the jwt
        try {
            var jws = jwtService.parseAndValidate(token);
            Claims c = jws.getPayload();
            // append the jwt payload on the request to make controller to handle the user info
            req.setAttribute("auth.userId", c.getSubject());
            req.setAttribute("auth.email", String.valueOf(c.get("email")));
            req.setAttribute("auth.username", String.valueOf(c.get("username")));
            req.setAttribute("auth.type",  c.get("type"));
            chain.doFilter(request, response);

        } catch (Exception e) {
            log.warn("JWT invalid: {}", e.getMessage());
            unauthorized(res, "invalid_token");
        }
    }

    private String resolveToken(HttpServletRequest req) {
        // 1) Authorization: Bearer xxx
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) return auth.substring(7);
        // 2) HttpOnly Cookie
        if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if (JwtService.COOKIE_NAME.equals(c.getName())) return c.getValue();
            }
        }
        return null;
    }

    private boolean isPublic(String path) {
        for (String p : PUBLIC_PATTERNS) {
            if (pathMatcher.match(p, path)) return true;
        }
        return false;
    }

    private void unauthorized(HttpServletResponse res, String code) throws IOException {
        res.setStatus(HttpStatus.UNAUTHORIZED.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(res.getWriter(), Map.of("code", code, "message", "Unauthorized"));
    }
}