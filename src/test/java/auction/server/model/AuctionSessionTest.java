package auction.server.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuctionSession Tests")
public class AuctionSessionTest {

    private AuctionSession session;
    private static final int ITEM_ID = 1;
    private static final int SELLER_ID = 100;
    private static final double STARTING_PRICE = 500.0;
    private Timestamp startTime;
    private Timestamp endTime;

    @BeforeEach
    public void setUp() {
        startTime = new Timestamp(System.currentTimeMillis());
        endTime = new Timestamp(System.currentTimeMillis() + 3600000); // 1 hour later
        session = new AuctionSession(ITEM_ID, SELLER_ID, STARTING_PRICE, startTime, endTime);
    }

    @Test
    @DisplayName("Should create auction session with correct properties")
    public void testAuctionSessionCreation() {
        assertNotNull(session);
        assertEquals(ITEM_ID, session.getItemId());
        assertEquals(SELLER_ID, session.getSellerId());
        assertEquals(STARTING_PRICE, session.getCurrentPrice());
        assertEquals("Chưa có ai", session.getHighestBidder());
        assertFalse(session.isFinished());
    }

    @Test
    @DisplayName("Should accept valid bid")
    public void testPlaceBidValid() {
        boolean result = session.placeBid("bidder1", 600.0);
        assertTrue(result);
        assertEquals(600.0, session.getCurrentPrice());
        assertEquals("bidder1", session.getHighestBidder());
    }

    @Test
    @DisplayName("Should reject bid equal to or lower than current price")
    public void testPlaceBidTooLow() {
        session.placeBid("bidder1", 600.0);

        boolean result1 = session.placeBid("bidder2", 600.0);
        assertFalse(result1);

        boolean result2 = session.placeBid("bidder3", 500.0);
        assertFalse(result2);
    }

    @Test
    @DisplayName("Should allow multiple bids from different bidders")
    public void testMultipleBids() {
        assertTrue(session.placeBid("bidder1", 600.0));
        assertEquals("bidder1", session.getHighestBidder());

        assertTrue(session.placeBid("bidder2", 700.0));
        assertEquals("bidder2", session.getHighestBidder());

        assertTrue(session.placeBid("bidder3", 800.0));
        assertEquals("bidder3", session.getHighestBidder());

        assertEquals(800.0, session.getCurrentPrice());
    }

    @Test
    @DisplayName("Should determine auction is running when within time window")
    public void testIsAuctionRunning() {
        assertTrue(session.isAuctionRunning());
    }

    @Test
    @DisplayName("Should determine auction is not running after finish")
    public void testIsAuctionRunningAfterFinish() {
        session.finishAuction();
        assertFalse(session.isAuctionRunning());
    }

    @Test
    @DisplayName("Should reject bid when auction is finished")
    public void testPlaceBidAfterFinish() {
        session.finishAuction();
        boolean result = session.placeBid("bidder1", 600.0);
        assertFalse(result);
    }

    @Test
    @DisplayName("Should finish auction correctly")
    public void testFinishAuction() {
        session.placeBid("bidder1", 600.0);
        session.finishAuction();

        assertTrue(session.isFinished());
        assertFalse(session.isAuctionRunning());
    }

    @Test
    @DisplayName("Should maintain bid history")
    public void testBidHistory() {
        session.placeBid("bidder1", 600.0);
        session.placeBid("bidder2", 700.0);

        List<String> history = session.getBidHistory();
        assertEquals(2, history.size());
        assertTrue(history.get(0).contains("bidder1"));
        assertTrue(history.get(1).contains("bidder2"));
    }

    @Test
    @DisplayName("Should record finish message in history")
    public void testBidHistoryWithFinishMessage() {
        session.placeBid("bidder1", 600.0);
        session.finishAuction();

        List<String> history = session.getBidHistory();
        assertTrue(history.get(history.size() - 1).contains("Phiên kết thúc"));
        assertTrue(history.get(history.size() - 1).contains("bidder1"));
    }

    @Test
    @DisplayName("Should load bid history correctly")
    public void testLoadBidHistory() {
        List<String> testHistory = List.of("Bid 1", "Bid 2", "Bid 3");
        session.loadBidHistory(testHistory);

        assertEquals(testHistory, session.getBidHistory());
    }

    @Test
    @DisplayName("Should clear existing bid history when loading new one")
    public void testLoadBidHistoryClearsOldHistory() {
        session.placeBid("bidder1", 600.0);
        assertEquals(1, session.getBidHistory().size());

        session.loadBidHistory(List.of("New bid 1"));
        List<String> history = session.getBidHistory();
        assertEquals(1, history.size());
        assertTrue(history.get(0).contains("New bid 1"));
    }

    @Test
    @DisplayName("Should set and get auction ID")
    public void testSetAndGetAuctionId() {
        session.setAuctionId(42);
        assertEquals(42, session.getAuctionId());
    }

    @Test
    @DisplayName("Should set and get item ID")
    public void testSetAndGetItemId() {
        session.setItemId(999);
        assertEquals(999, session.getItemId());
    }

    @Test
    @DisplayName("Should set and get seller ID")
    public void testSetAndGetSellerId() {
        session.setSellerId(888);
        assertEquals(888, session.getSellerId());
    }

    @Test
    @DisplayName("Should set and get current price")
    public void testSetAndGetCurrentPrice() {
        session.setCurrentPrice(1000.0);
        assertEquals(1000.0, session.getCurrentPrice());
    }

    @Test
    @DisplayName("Should set and get highest bidder")
    public void testSetAndGetHighestBidder() {
        session.setHighestBidder("champion");
        assertEquals("champion", session.getHighestBidder());
    }

    @Test
    @DisplayName("Should set and get start time")
    public void testSetAndGetStartTime() {
        Timestamp newStartTime = new Timestamp(System.currentTimeMillis());
        session.setStartTime(newStartTime);
        assertEquals(newStartTime, session.getStartTime());
    }

    @Test
    @DisplayName("Should set and get end time")
    public void testSetAndGetEndTime() {
        Timestamp newEndTime = new Timestamp(System.currentTimeMillis() + 7200000);
        session.setEndTime(newEndTime);
        assertEquals(newEndTime, session.getEndTime());
    }
}

