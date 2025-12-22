package com.example.blog.global.security.jwt;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JWTUtil {

    private final SecretKey secretKey;

    /**
     * 생성자에서 application.properties에 저장된 SecretKey 값을 가져와 설정
     */
    public JWTUtil(@Value("${spring.jwt.secret}") String secret) {
        // secret 길이는 최소 32바이트(256비트) 이상으로 설정하는 게 좋음
        this.secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm()
        );
    }

    /**
     * JWT에서 username 추출
     */
    public String getUsername(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)      // ★ parseClaimsJws -> parseSignedClaims
                .getPayload()
                .get("username", String.class);
    }

    /**
     * JWT에서 role(권한) 추출
     */
    public String getRole(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    /**
     * JWT 만료 여부 확인
     */
    public Boolean isTokenExpired(String token) {
        Date expiration = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();

        return expiration.before(new Date());
    }

    /**
     * JWT 생성 메서드
     * - username, role(권한), 만료 시간(expiredMs)을 포함한 JWT 발급
     */
    public String createJwt(String username, String role, Long expiredMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiredMs);

        return Jwts.builder()
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * refresh 발급 메서드
     */
    public String createRefreshJwt(String username, long expireMs) {
        // refresh도 JWT로 쓸 거면 type 같은 claim을 넣어 구분하는 게 좋습니다.
        return Jwts.builder()
                .claim("type", "refresh")
                .claim("username", username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireMs))
                .signWith(secretKey)
                .compact();
    }
}
