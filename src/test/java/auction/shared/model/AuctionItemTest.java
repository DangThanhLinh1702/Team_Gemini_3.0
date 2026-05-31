package auction.shared.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuctionItem Model Tests")
public class AuctionItemTest {

    private AuctionItem item;
    private static final String ITEM_ID = UUID.randomUUID().toString();
    private static final String ITEM_NAME = "Vintage Watch";
    private static final String DESCRIPTION = "Beautiful vintage watch from 1970s";
    private static final long STARTING_PRICE = 100;
    private static final String SELLER_USERNAME = "collector123";

    @BeforeEach
    public void setUp() {
        LocalDateTime endTime = LocalDateTime.now().plusDays(3);
        item = new AuctionItem(ITEM_ID, ITEM_NAME, DESCRIPTION, STARTING_PRICE,
                              SELLER_USERNAME, endTime);
    }

    @Test
    @DisplayName("Should create auction item with correct properties")
    public void testAuctionItemCreation() {
        assertNotNull(item);
        assertEquals(ITEM_ID, item.getId());
        assertEquals(ITEM_NAME, item.getName());
        assertEquals(DESCRIPTION, item.getDescription());
        assertEquals(STARTING_PRICE, item.getStartingPrice());
        assertEquals(STARTING_PRICE, item.getCurrentPrice());
        assertEquals(SELLER_USERNAME, item.getSellerUsername());
        assertEquals("ACTIVE", item.getStatus());
    }

    @Test
    @DisplayName("Should set and get item ID")
    public void testSetAndGetId() {
        String newId = UUID.randomUUID().toString();
        item.setId(newId);
        assertEquals(newId, item.getId());
    }

    @Test
    @DisplayName("Should set and get name")
    public void testSetAndGetName() {
        String newName = "Modern Smartwatch";
        item.setName(newName);
        assertEquals(newName, item.getName());
    }

    @Test
    @DisplayName("Should set and get description")
    public void testSetAndGetDescription() {
        String newDescription = "Updated description";
        item.setDescription(newDescription);
        assertEquals(newDescription, item.getDescription());
    }

    @Test
    @DisplayName("Should set and get starting price")
    public void testSetAndGetStartingPrice() {
        long newPrice = 500;
        item.setStartingPrice(newPrice);
        assertEquals(newPrice, item.getStartingPrice());
    }

    @Test
    @DisplayName("Should update current price for bidding")
    public void testUpdateCurrentPrice() {
        long newBid = 150;
        item.setCurrentPrice(newBid);
        assertEquals(newBid, item.getCurrentPrice());
    }

    @Test
    @DisplayName("Should set and get seller username")
    public void testSetAndGetSellerUsername() {
        String newSeller = "seller999";
        item.setSellerUsername(newSeller);
        assertEquals(newSeller, item.getSellerUsername());
    }

    @Test
    @DisplayName("Should set and get current bidder")
    public void testSetAndGetCurrentBidder() {
        String bidder = "bidder456";
        item.setCurrentBidder(bidder);
        assertEquals(bidder, item.getCurrentBidder());
    }

    @Test
    @DisplayName("Should set and get start time")
    public void testSetAndGetStartTime() {
        LocalDateTime startTime = LocalDateTime.now();
        item.setStartTime(startTime);
        assertEquals(startTime, item.getStartTime());
    }

    @Test
    @DisplayName("Should set and get end time")
    public void testSetAndGetEndTime() {
        LocalDateTime newEndTime = LocalDateTime.now().plusDays(7);
        item.setEndTime(newEndTime);
        assertEquals(newEndTime, item.getEndTime());
    }

    @Test
    @DisplayName("Should set and get status")
    public void testSetAndGetStatus() {
        item.setStatus("ENDED");
        assertEquals("ENDED", item.getStatus());

        item.setStatus("CANCELLED");
        assertEquals("CANCELLED", item.getStatus());
    }

    @Test
    @DisplayName("Should create empty auction item")
    public void testCreateEmptyAuctionItem() {
        AuctionItem emptyItem = new AuctionItem();
        assertNotNull(emptyItem);
    }

    @Test
    @DisplayName("Should have initial status as ACTIVE")
    public void testInitialStatusIsActive() {
        assertEquals("ACTIVE", item.getStatus());
    }

    @Test
    @DisplayName("Current price should match starting price initially")
    public void testCurrentPriceEqualsStartingPriceInitially() {
        assertEquals(item.getStartingPrice(), item.getCurrentPrice());
    }
}

