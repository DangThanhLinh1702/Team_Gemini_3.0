package auction.server.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Item Model Tests")
public class ItemTest {

    private Item item;
    private static final String ITEM_NAME = "Laptop Dell XPS";
    private static final String DESCRIPTION = "High-performance laptop in excellent condition";
    private static final double STARTING_PRICE = 500.0;
    private static final String SELLER_USERNAME = "seller123";
    private static final String IMAGE_URL = "http://example.com/laptop.jpg";
    private static final long END_TIME = System.currentTimeMillis() + 86400000; // 24 hours

    @BeforeEach
    public void setUp() {
        item = new Item(ITEM_NAME, DESCRIPTION, STARTING_PRICE, SELLER_USERNAME, IMAGE_URL, END_TIME);
    }

    @Test
    @DisplayName("Should create item with correct properties")
    public void testItemCreation() {
        assertNotNull(item);
        assertEquals(ITEM_NAME, item.getName());
        assertEquals(DESCRIPTION, item.getDescription());
        assertEquals(STARTING_PRICE, item.getStartingPrice());
        assertEquals(SELLER_USERNAME, item.getSellerUserName());
        assertEquals(IMAGE_URL, item.getImage());
        assertEquals(END_TIME, item.getEndTime());
    }

    @Test
    @DisplayName("Should set and get item ID")
    public void testSetAndGetId() {
        item.setId(1);
        assertEquals(1, item.getId());
    }

    @Test
    @DisplayName("Should update item name")
    public void testSetAndGetName() {
        String newName = "iMac 27 inch";
        item.setName(newName);
        assertEquals(newName, item.getName());
    }

    @Test
    @DisplayName("Should update item description")
    public void testSetAndGetDescription() {
        String newDescription = "Updated description";
        item.setDescription(newDescription);
        assertEquals(newDescription, item.getDescription());
    }

    @Test
    @DisplayName("Should update starting price")
    public void testSetAndGetStartingPrice() {
        double newPrice = 1000.0;
        item.setStartingPrice(newPrice);
        assertEquals(newPrice, item.getStartingPrice());
    }

    @Test
    @DisplayName("Should update seller username")
    public void testSetAndGetSellerUserName() {
        String newSeller = "newseller123";
        item.setSellerUserName(newSeller);
        assertEquals(newSeller, item.getSellerUserName());
    }

    @Test
    @DisplayName("Should update seller ID")
    public void testSetAndGetSellerId() {
        item.setSellerId(42);
        assertEquals(42, item.getSellerId());
    }

    @Test
    @DisplayName("Should update image URL")
    public void testSetAndGetImage() {
        String newImage = "http://example.com/newimagure.jpg";
        item.setImage(newImage);
        assertEquals(newImage, item.getImage());
    }

    @Test
    @DisplayName("Should update end time")
    public void testSetAndGetEndTime() {
        long newEndTime = System.currentTimeMillis() + 172800000; // 48 hours
        item.setEndTime(newEndTime);
        assertEquals(newEndTime, item.getEndTime());
    }

    @Test
    @DisplayName("Should create item with ID parameter")
    public void testCreateItemWithId() {
        Item itemWithId = new Item(5, "iPhone 15", "New iPhone", 800.0,
                                    "seller456", "http://image.jpg", END_TIME);
        assertEquals(5, itemWithId.getId());
        assertEquals("iPhone 15", itemWithId.getName());
    }

    @Test
    @DisplayName("Should create empty item")
    public void testCreateEmptyItem() {
        Item emptyItem = new Item();
        assertNotNull(emptyItem);
    }

    @Test
    @DisplayName("Should validate positive starting price")
    public void testPositiveStartingPrice() {
        assertTrue(item.getStartingPrice() > 0);
    }

    @Test
    @DisplayName("Should have future end time")
    public void testEndTimeInFuture() {
        assertTrue(item.getEndTime() > System.currentTimeMillis());
    }
}

