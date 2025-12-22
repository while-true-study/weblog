package com.example.blog.api.auth;

import com.example.blog.domain.user.dto.LoginRequest;
import com.example.blog.domain.user.dto.SignupRequest;
import com.example.blog.domain.user.dto.UserResponse;
import com.example.blog.domain.user.entity.CustomUserPrincipal;
import com.example.blog.domain.user.service.UserService;
import com.example.blog.global.common.ApiResponse;
import com.example.blog.global.security.jwt.JWTUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;

    private static final long ACCESS_TOKEN_EXPIRE_MS = 1000L * 60 * 60;

    @PostMapping("/signup")
    public ApiResponse<Void> signup(@RequestBody SignupRequest request) {
        userService.signup(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody LoginRequest request,
                                            HttpServletResponse response) {

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");

        String username = auth.getName(); // 보통 email

        // access: 짧게
        String accessToken = jwtUtil.createJwt(username, role, ACCESS_TOKEN_EXPIRE_MS);

        // refresh: 길게 (예: 14일)
        long refreshExpireMs = 1000L * 60 * 60 * 24 * 14;
        String refreshToken = jwtUtil.createRefreshJwt(username, refreshExpireMs); // 아래 JWTUtil에 추가

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)         // 로컬 http면 false, 운영(https)은 true
                .sameSite("Lax")       // 같은 site(localhost)면 보통 OK. 운영/크로스사이트면 None+Secure 고려
                .path("/api/v1/auth")  // refresh 요청 경로 범위
                .maxAge(refreshExpireMs / 1000)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.success(new TokenResponse(accessToken));
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.success(
                new UserResponse(principal.getId(), principal.getEmail(), principal.getNickname())
        );
    }

    public record TokenResponse(String accessToken) {}
}
