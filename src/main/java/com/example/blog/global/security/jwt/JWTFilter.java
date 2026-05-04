package com.example.blog.global.security.jwt;

import com.example.blog.user.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                if (jwtUtil.isTokenExpired(token)) {
                    unauthorized(response, "TOKEN_EXPIRED", "JWT expired");
                    return;
                }

                String username = jwtUtil.getUsername(token);
                if (username == null || username.isBlank()) {
                    unauthorized(response, "INVALID_TOKEN", "Missing username claim");
                    return;
                }

                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );

                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (UsernameNotFoundException e) {
                SecurityContextHolder.clearContext();
                unauthorized(response, "INVALID_TOKEN", "User not found");
                return;
            } catch (io.jsonwebtoken.JwtException e) {
                SecurityContextHolder.clearContext();
                unauthorized(response, "INVALID_TOKEN", "Invalid token");
                return;
            }
        }

        // JWT 관련 처리 끝났으면 무조건 다음으로 넘긴다.
        filterChain.doFilter(request, response);
    }

    private void unauthorized(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
            {"success":false,"error":{"code":"%s","message":"%s"}}
        """.formatted(code, message));
    }
}
