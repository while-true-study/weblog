package com.example.blog.auth.presentation;

import com.example.blog.global.common.ApiResponse;
import com.example.blog.global.security.jwt.JWTUtil;
import com.example.blog.user.entity.CustomUserPrincipal;
import com.example.blog.user.presentation.dto.request.LoginRequest;
import com.example.blog.user.presentation.dto.request.SignupRequest;
import com.example.blog.user.presentation.dto.response.LoginResponse;
import com.example.blog.user.presentation.dto.response.UserResponse;
import com.example.blog.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Auth", description = "회원가입/로그인/본인 조회 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;

    private static final long ACCESS_TOKEN_EXPIRE_MS = 1000L * 60 * 60;

    @Operation(
            summary = "회원가입",
            description = "이메일/비밀번호/닉네임으로 회원가입합니다.",
            security = {} // 로그인 전이므로 문서상 인증 제거
    )
    @PostMapping("/signup")
    public ApiResponse<Void> signup(@RequestBody SignupRequest request) {
        userService.signup(request);
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "로그인",
            description = "이메일, 비밀번호로 인증후 AccessToken, RefreshToken 발급",
            security = {} // 로그인전 이므로 문서상 인증 제거
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            )
    })
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request,
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

        return ApiResponse.success(new LoginResponse(accessToken, refreshToken));
    }

    @Operation(
            summary = "내 정보 조회",
            description = "Authorization: Bearer {accessToken} 필요"
    )
    @SecurityRequirement(name = "bearerAuth") // JWT필요하다는 것
    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.success(
                new UserResponse(principal.getId(), principal.getEmail(), principal.getNickname())
        );
    }
}
