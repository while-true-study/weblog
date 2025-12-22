//package com.example.blog.global.security.jwt;
//
//import com.example.blog.domain.user.dto.LoginRequest;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//import java.io.IOException;
//
//@RequiredArgsConstructor
//public class LoginFilter extends UsernamePasswordAuthenticationFilter {
//
//
//    private final AuthenticationManager authenticationManager;
//    private final JWTUtil jwtUtil;
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    // 토큰 유효 기간 (예: 1시간)
//    private static final long ACCESS_TOKEN_EXPIRE_MS = 1000L * 60 * 60;
//
//    @Override
//    public Authentication attemptAuthentication(HttpServletRequest request,
//                                                HttpServletResponse response)
//            throws AuthenticationException {
//
//        try {
//            // JSON Body -> LoginRequest 파싱
//            LoginRequest loginRequest = objectMapper.readValue(
//                    request.getInputStream(),
//                    LoginRequest.class
//            );
//
//            UsernamePasswordAuthenticationToken authToken =
//                    new UsernamePasswordAuthenticationToken(
//                            loginRequest.email(),    // username
//                            loginRequest.password()  // password
//                    );
//
//            return authenticationManager.authenticate(authToken);
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    @Override
//    protected void successfulAuthentication(HttpServletRequest request,
//                                            HttpServletResponse response,
//                                            FilterChain chain,
//                                            Authentication authResult)
//            throws IOException, ServletException {
//
//        String username = authResult.getName(); // UserDetails.getUsername()
//
//        // 권한 문자열 하나 꺼내기 (예: ROLE_USER)
//        String role = authResult.getAuthorities().stream()
//                .map(GrantedAuthority::getAuthority)
//                .findFirst()
//                .orElse("ROLE_USER");
//
//        String token = jwtUtil.createJwt(username, role, ACCESS_TOKEN_EXPIRE_MS);
//
//        // JSON 응답으로 토큰 내려주기
//        response.setContentType("application/json;charset=UTF-8");
//        response.getWriter().write("""
//                {
//                  "success": true,
//                  "data": { "accessToken": "%s" },
//                  "error": null
//                }
//                """.formatted(token));
//    }
//
//    @Override
//    protected void unsuccessfulAuthentication(HttpServletRequest request,
//                                              HttpServletResponse response,
//                                              AuthenticationException failed)
//            throws IOException, ServletException {
//
//        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//        response.setContentType("application/json;charset=UTF-8");
//        response.getWriter().write("""
//                {
//                  "success": false,
//                  "data": null,
//                  "error": "로그인 실패: %s"
//                }
//                """.formatted(failed.getMessage()));
//    }
//}
