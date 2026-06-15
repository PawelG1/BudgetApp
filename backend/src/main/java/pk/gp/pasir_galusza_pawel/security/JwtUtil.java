package pk.gp.pasir_galusza_pawel.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pk.gp.pasir_galusza_pawel.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private static final long EXPIRATION_MS = 3_600_000L;

    private final SecretKey key;

    public JwtUtil(
            @Value("${JWT_SECRET:d4a8e632f79c4a529ad7c9e0d1b4c3e8a7f6c5b4df3b219e8a7f6c5b4d3c2b1a_this_is_a_default_local_secret}")
            String jwtSecret
    ) {
        if(jwtSecret == null || jwtSecret.isBlank()) {
            throw new NullPointerException("JWT_SECRET must be configured");
        }
        if(jwtSecret.getBytes(StandardCharsets.UTF_8).length < 64) {
            throw new IllegalStateException("JWT_SECRET is too short, must be at least 64 bytes");
        }
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }


//    private final SecretKey key = Keys.hmacShaKeyFor(SECRET_STRING.getBytes());

//    public String generateToken(String email) {
//        long expirationMs = 3600000;
//        return Jwts.builder()
//                .subject(email)
//                .issuedAt(new Date())
//                .expiration(new Date(System.currentTimeMillis() + expirationMs))
//                .signWith(key)
//                .compact();
//    }

//    public String extractUsername(String token) {
//        return Jwts.parser()
//                .verifyWith(key)
//                .build()
//                .parseSignedClaims(token)
//                .getPayload()
//                .getSubject();
//    }

//    public boolean validateToken(String token) {
//        try {
//            Jwts.parser()
//                    .verifyWith(key)
//                    .build()
//                    .parseSignedClaims(token);
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }

    public String generateToken(User user){
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("email", user.getEmail());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token){
        return extractAllClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        }catch (Exception e){
            return false;
        }
    }
}
