package auction.shared.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.util.Date;

public class JwtUtil {
    // Khóa bí mật - Chỉ Server được biết. Nếu lộ khóa này, hệ thống sẽ bị hack.
    private static final String SECRET_KEY = "nhom_3_dang_cap_may_con_ga_biet_gi";
    private static final Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);

    public static String createToken(String username, String role) {
        return JWT.create()
                .withSubject(username)
                .withClaim("role", role)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 86400000))
                .sign(algorithm);
    }

    public static DecodedJWT verifyToken(String token) {
        try {
            // Kiểm tra xem thẻ có phải do mình phát ra không và còn hạn không
            JWTVerifier verifier = JWT.require(algorithm).build();
            return verifier.verify(token);
        } catch (Exception e) {
            System.err.println("Lỗi xác thực Token: " + e.getMessage());
            return null;
        }
    }

    public static String getUsernameFromToken(String token) {
        DecodedJWT decoded = verifyToken(token);
        return decoded != null ? decoded.getSubject() : null;
    }

    public static String getRoleFromToken(String token) {
        DecodedJWT decoded = verifyToken(token);
        return decoded != null ? decoded.getClaim("role").asString() : null;
    }
}