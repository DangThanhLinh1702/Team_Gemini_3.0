package auction.server.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Model Tests")
public class UserTest {

    private Bidder bidder;
    private Seller seller;

    @BeforeEach
    public void setUp() {
        bidder = new Bidder("john_doe", "password123");
        seller = new Seller("jane_smith", "password456");
    }

    @Test
    @DisplayName("Should create a bidder with correct properties")
    public void testBidderCreation() {
        assertNotNull(bidder);
        assertEquals("john_doe", bidder.getUsername());
        assertEquals("password123", bidder.getPassword());
        assertEquals("BIDDER", bidder.getRole());
        assertTrue(bidder.getId() > 0);
    }

    @Test
    @DisplayName("Should create a seller with correct properties")
    public void testSellerCreation() {
        assertNotNull(seller);
        assertEquals("jane_smith", seller.getUsername());
        assertEquals("password456", seller.getPassword());
        assertEquals("SELLER", seller.getRole());
        assertTrue(seller.getId() > 0);
    }

    @Test
    @DisplayName("Should set and get user ID")
    public void testSetAndGetId() {
        bidder.setId(100);
        assertEquals(100, bidder.getId());
    }

    @Test
    @DisplayName("Should have unique IDs for different users")
    public void testUniqueIds() {
        Bidder bidder2 = new Bidder("user2", "pass2");
        assertNotEquals(bidder.getId(), bidder2.getId());
    }

    @Test
    @DisplayName("Should get username correctly")
    public void testGetUsername() {
        assertEquals("john_doe", bidder.getUsername());
        assertEquals("jane_smith", seller.getUsername());
    }

    @Test
    @DisplayName("Should get password correctly")
    public void testGetPassword() {
        assertEquals("password123", bidder.getPassword());
        assertEquals("password456", seller.getPassword());
    }

    @Test
    @DisplayName("Should get role correctly")
    public void testGetRole() {
        assertEquals("BIDDER", bidder.getRole());
        assertEquals("SELLER", seller.getRole());
    }
}



