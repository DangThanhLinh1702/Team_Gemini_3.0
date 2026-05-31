package auction.server.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Admin Model Tests")
public class AdminTest {

    private Admin admin;

    @BeforeEach
    public void setUp() {
        admin = new Admin("admin_user", "admin_password");
    }

    @Test
    @DisplayName("Should create admin with correct role")
    public void testAdminCreation() {
        assertNotNull(admin);
        assertEquals("admin_user", admin.getUsername());
        assertEquals("admin_password", admin.getPassword());
        assertEquals("ADMIN", admin.getRole());
    }

    @Test
    @DisplayName("Admin should have ADMIN role assigned")
    public void testAdminRoleAssignment() {
        assertEquals("ADMIN", admin.getRole());
    }

    @Test
    @DisplayName("Admin should have positive ID")
    public void testAdminHasPositiveId() {
        assertTrue(admin.getId() > 0);
    }

    @Test
    @DisplayName("Should set and get admin ID")
    public void testSetAndGetAdminId() {
        admin.setId(10);
        assertEquals(10, admin.getId());
    }

    @Test
    @DisplayName("Two admins should have different IDs")
    public void testMultipleAdminsHaveDifferentIds() {
        Admin admin2 = new Admin("admin2", "pass2");
        assertNotEquals(admin.getId(), admin2.getId());
    }
}

