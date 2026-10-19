package com.football.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.football.vo.ResultVO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_ROLES = new HashSet<>(Arrays.asList("admin", "referee", "user"));

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.trim().isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith("Bearer ") || authorization.length() <= 7) {
            writeUnauthorized(response);
            return;
        }

        try {
            DecodedJWT token = jwtService.verify(authorization.substring(7).trim());
            Long userId = token.getClaim("userId").asLong();
            String username = token.getClaim("username").asString();
            String role = token.getClaim("role").asString();
            if (userId == null || username == null || username.trim().isEmpty() || !ALLOWED_ROLES.contains(role)) {
                writeUnauthorized(response);
                return;
            }

            JwtUserPrincipal principal = new JwtUserPrincipal(userId, username);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            request.setAttribute("role", role);
            chain.doFilter(request, response);
        } catch (JWTVerificationException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(response);
        }
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ResultVO.error(401, "未认证或登录凭证无效")));
    }
}
