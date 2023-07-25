package com.test.teamlog.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    private static final long ACCESS_TOKEN_VALIDATION_SECOND = 1000L * 60 * 60 * 24; // 1일
    private static final long REFRESH_TOKEN_VALIDATION_SECOND = 1000L * 60 * 60 * 24 * 7 * 2; // 2주

    @Value("${app.jwtSecret}")
    private String JWT_SECRET;

    private final UserDetailsService userDetailsService;

    public String generateAccessToken(String identification) {
        return doGenerateToken(identification, ACCESS_TOKEN_VALIDATION_SECOND);
    }

    public String generateRefreshToken(String identification) {
        return doGenerateToken(identification, REFRESH_TOKEN_VALIDATION_SECOND);
    }

    public String getUserId(String token) {
        return extractAllClaims(token).get("userId", String.class);
    }

    /**
     * 토큰 생성
     * @param userId
     * @param expireTime
     * @return 토큰, Access Token, Refersh Token 생성 시 사용
     */
    private String doGenerateToken(String userId, long expireTime) {
        Claims claims = Jwts.claims();
        claims.put("userId", userId);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expireTime))
                .signWith(SignatureAlgorithm.HS512, JWT_SECRET)
                .compact();
    }

    /**
     * 토큰 만료 여부 검사
     * @param token
     * @return boolean 토큰 만료 여부
     */
    public boolean isTokenExpired(String token) {
        final Date expiration = extractAllClaims(token).getExpiration();
        return expiration.before(new Date());
    }


    /**
     * 토큰 내 정보 추출
     * @param token
     * @return Claims 토큰에 저장된 정보
     * @throws ExpiredJwtException
     */
    private Claims extractAllClaims(String token) throws ExpiredJwtException {
        return Jwts.parser()
                .setSigningKey(JWT_SECRET)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 요청 토큰을 이용해 유효한 사용자인지 검증한다.
     * @param token
     * @return
     */
    public Authentication authenticate(String token) {
        final String userId = getUserId(token);
        final UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }
}