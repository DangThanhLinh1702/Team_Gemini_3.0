package auction.server.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests cho Item model.
 * Kiểm tra constructor, getter và setter — không cần DB hay mock.
 */
@DisplayName("Item Model Tests")
class ItemTest {

    @Test
    @DisplayName("Constructor 6 tham số tạo Item đúng")
    void constructor_sixParams_createsItemCorrectly() {
        long endTime = System.currentTimeMillis() + 86400000L;
        Item item = new Item("Laptop", "Máy tính xách tay Dell", 5000000.0,
                             "seller_bob", "base64imagedata", endTime);

        assertEquals("Laptop", item.getName());
        assertEquals("Máy tính xách tay Dell", item.getDescription());
        assertEquals(5000000.0, item.getStartingPrice());
        assertEquals("seller_bob", item.getSellerUserName());
        assertEquals("base64imagedata", item.getImage());
        assertEquals(endTime, item.getEndTime());
    }

    @Test
    @DisplayName("Constructor 7 tham số (có ID) tạo Item đúng")
    void constructor_sevenParams_createsItemWithId() {
        Item item = new Item(42, "Điện thoại", "iPhone 15", 25000000.0,
                             "seller_alice", null, 0L);

        assertEquals(42, item.getId());
        assertEquals("Điện thoại", item.getName());
    }

    @Test
    @DisplayName("Setter thay đổi giá trị đúng")
    void setters_updateValuesCorrectly() {
        Item item = new Item("Sản phẩm cũ", "Mô tả", 100.0, "seller", null, 0L);

        item.setName("Sản phẩm mới");
        item.setDescription("Mô tả mới");
        item.setStartingPrice(999.0);
        item.setSellerUserName("new_seller");
        item.setId(10);
        item.setImage("newImageData");
        item.setEndTime(12345L);

        assertEquals("Sản phẩm mới", item.getName());
        assertEquals("Mô tả mới", item.getDescription());
        assertEquals(999.0, item.getStartingPrice());
        assertEquals("new_seller", item.getSellerUserName());
        assertEquals(10, item.getId());
        assertEquals("newImageData", item.getImage());
        assertEquals(12345L, item.getEndTime());
    }

    @Test
    @DisplayName("Item cho phép image null")
    void item_nullImage_isAllowed() {
        Item item = new Item("Sản phẩm", "Mô tả", 100.0, "seller", null, 0L);

        assertNull(item.getImage());
    }

    @Test
    @DisplayName("Giá khởi điểm là số thực (double)")
    void startingPrice_isDouble() {
        Item item = new Item("Sản phẩm", "Mô tả", 1500.75, "seller", null, 0L);

        assertEquals(1500.75, item.getStartingPrice(), 0.001);
    }
}
