package com.projetointegrador.seumentor.security.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

  //TODO: Mover a secretkey para uma variavel de ambiente
  private static final String SECRET_KEY = "c4b4d5a2e1f982b3c5d6b7f8a9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c";

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  public String generateToken(UserDetails userDetails) {
    return generateToken(new HashMap<>(), userDetails);
  }

  public String generateToken(
      Map<String, Object> extraClaims,
      UserDetails userDetails) {
    long currentTimeMillis = System.currentTimeMillis();
    Date issuedAtDate = new Date(currentTimeMillis);
    long expirationMillis = currentTimeMillis + TimeUnit.DAYS.toMillis(1);
    Date expirationDate = new Date(expirationMillis);

    return Jwts.builder()
        .claims().add(extraClaims).and()
        .subject(userDetails.getUsername())
        .issuedAt(issuedAtDate)
        .expiration(expirationDate)
        .signWith(getSigninKey())
        .compact();
  }

  public Boolean isTokenValid(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
  }

  private boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  private Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  private Claims extractAllClaims(String token) {
    JwtParser parser = Jwts.parser()
        .verifyWith(getSigninKey())
        .build();

    Jws<Claims> claimsJws = parser.parseSignedClaims(token);

    return claimsJws.getPayload();
  }

  private SecretKey getSigninKey() {
    byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
    return Keys.hmacShaKeyFor(keyBytes);
  }

}
