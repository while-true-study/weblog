package com.example.blog.global.security.jwt;

import com.example.blog.user.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

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

        // Authorization 헤더 없거나 Bearer 아니면 그냥 통과
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            // 만료면 401로 종료 (서버 에러로 터뜨리지 않음)
            if (jwtUtil.isTokenExpired(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("""
                {"success":false,"error":{"code":"TOKEN_EXPIRED","message":"JWT expired"}}
            """);

                return;
            }

            String username = jwtUtil.getUsername(token);

            UserDetails userDetails =
                    customUserDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);

        } catch (io.jsonwebtoken.JwtException e) {
            // 서명 위조/형식 오류 등도 401 처리
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
            {"success":false,"error":{"code":"INVALID_TOKEN","message":"Invalid token"}}
        """);
        } catch (Exception e) {
            // 기타 예외는 401 또는 500 중 정책 선택 가능.
            // 인증 필터 단계에서는 보통 401로 묶는 게 깔끔합니다.
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
            {"success":false,"error":{"code":"AUTH_ERROR","message":"Authentication error"}}
        """);
        }
    }

}