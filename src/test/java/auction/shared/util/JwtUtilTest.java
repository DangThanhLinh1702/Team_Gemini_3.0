package auction.shared.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import com.auth0.jwt.interfaces.DecodedJWT;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JWT Utility Tests")
public class JwtUtilTest {

    @Test
    @DisplayName("Should create a valid token")
    public void testCreateToken() {
        String token = JwtUtil.createToken("testuser", "BIDDER");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Should verify a valid token")
    public void testVerifyToken() {
        String token = JwtUtil.createToken("testuser", "BIDDER");
        DecodedJWT decoded = JwtUtil.verifyToken(token);

        assertNotNull(decoded);
        assertEquals("testuser", decoded.getSubject());
        assertEquals("BIDDER", decoded.getClaim("role").asString());
    }

    @Test
    @DisplayName("Should return null for invalid token")
    public void testVerifyInvalidToken() {
        DecodedJWT decoded = JwtUtil.verifyToken("invalid.token.here");
        assertNull(decoded);
    }

    @Test
    @DisplayName("Should extract username from token")
    public void testGetUsernameFromToken() {
        String token = JwtUtil.createToken("john_doe", "SELLER");
        String username = JwtUtil.getUsernameFromToken(token);

        assertEquals("john_doe", username);
    }

    @Test
    @DisplayName("Should return null username for invalid token")
    public void testGetUsernameFromInvalidToken() {
        String username = JwtUtil.getUsernameFromToken("invalid.token");
        assertNull(username);
    }

    @Test
    @DisplayName("Should extract role from token")
    public void testGetRoleFromToken() {
        String token = JwtUtil.createToken("admin_user", "ADMIN");
        String role = JwtUtil.getRoleFromToken(token);

        assertEquals("ADMIN", role);
    }

    @Test
    @DisplayName("Should return null role for invalid token")
    public void testGetRoleFromInvalidToken() {
        String role = JwtUtil.getRoleFromToken("invalid.token");
        assertNull(role);
    }

    @Test
    @DisplayName("Should create tokens with different roles")
    public void testCreateTokensWithDifferentRoles() {
        String bidderToken = JwtUtil.createToken("bidder1", "BIDDER");
        String sellerToken = JwtUtil.createToken("seller1", "SELLER");
        String adminToken = JwtUtil.createToken("admin1", "ADMIN");

        assertEquals("BIDDER", JwtUtil.getRoleFromToken(bidderToken));
        assertEquals("SELLER", JwtUtil.getRoleFromToken(sellerToken));
        assertEquals("ADMIN", JwtUtil.getRoleFromToken(adminToken));
    }

    @Test
    @DisplayName("Should have different tokens for different users")
    public void testDifferentTokensForDifferentUsers() {
        String token1 = JwtUtil.createToken("user1", "BIDDER");
        String token2 = JwtUtil.createToken("user2", "BIDDER");

        assertNotEquals(token1, token2);
        assertEquals("user1", JwtUtil.getUsernameFromToken(token1));
        assertEquals("user2", JwtUtil.getUsernameFromToken(token2));
    }
}

