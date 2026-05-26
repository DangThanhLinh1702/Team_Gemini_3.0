package auction.server.service;

import auction.server.model.Item;
import auction.server.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho ItemService.
 * Dùng Mockito để mock ItemRepository — không cần kết nối DB thật.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ItemService Tests")
class ItemServiceTest {

    @Mock
    private ItemRepository mockItemRepository;

    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemService(mockItemRepository);
    }

    // ─── addItem() ──────────────────────────────────────────────

    @Test
    @DisplayName("Thêm sản phẩm thành công với thông tin hợp lệ")
    void addItem_validInput_returnsSuccess() {
        String result = itemService.addItem(
                "Laptop Dell", "Máy tính xách tay", 5000000.0,
                "seller_bob", null, System.currentTimeMillis() + 86400000L);

        assertEquals("success", result);
        verify(mockItemRepository, times(1)).saveItem(any(Item.class));
    }

    @Test
    @DisplayName("Thêm sản phẩm thất bại khi tên sản phẩm null")
    void addItem_nullName_returnsError() {
        String result = itemService.addItem(
                null, "Mô tả", 1000.0, "seller_bob", null, 0L);

        assertEquals("tên sản phẩm không được để trống", result);
        verify(mockItemRepository, never()).saveItem(any());
    }

    @Test
    @DisplayName("Thêm sản phẩm thất bại khi tên sản phẩm rỗng")
    void addItem_emptyName_returnsError() {
        String result = itemService.addItem(
                "   ", "Mô tả", 1000.0, "seller_bob", null, 0L);

        assertEquals("tên sản phẩm không được để trống", result);
    }

    @Test
    @DisplayName("Thêm sản phẩm thất bại khi giá khởi điểm = 0")
    void addItem_zeroPrize_returnsError() {
        String result = itemService.addItem(
                "Sản phẩm A", "Mô tả", 0.0, "seller_bob", null, 0L);

        assertEquals("giá khởi điểm phải lớn hơn 0", result);
    }

    @Test
    @DisplayName("Thêm sản phẩm thất bại khi giá khởi điểm âm")
    void addItem_negativePrice_returnsError() {
        String result = itemService.addItem(
                "Sản phẩm A", "Mô tả", -100.0, "seller_bob", null, 0L);

        assertEquals("giá khởi điểm phải lớn hơn 0", result);
    }

    @Test
    @DisplayName("Thêm sản phẩm thất bại khi tên người bán rỗng")
    void addItem_emptySellerName_returnsError() {
        String result = itemService.addItem(
                "Sản phẩm A", "Mô tả", 1000.0, "", null, 0L);

        assertEquals("tên người bán không được để trống", result);
    }

    @Test
    @DisplayName("Thêm sản phẩm thất bại khi tên người bán null")
    void addItem_nullSellerName_returnsError() {
        String result = itemService.addItem(
                "Sản phẩm A", "Mô tả", 1000.0, null, null, 0L);

        assertEquals("tên người bán không được để trống", result);
    }

    // ─── findItemById() ─────────────────────────────────────────

    @Test
    @DisplayName("Tìm sản phẩm theo ID — trả về đúng sản phẩm")
    void findItemById_existingId_returnsItem() {
        Item mockItem = new Item(1, "Laptop", "Mô tả", 5000000.0, "seller", null, 0L);
        when(mockItemRepository.findById(1)).thenReturn(mockItem);

        Item result = itemService.findItemById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Laptop", result.getName());
    }

    @Test
    @DisplayName("Tìm sản phẩm theo ID — trả về null nếu không tồn tại")
    void findItemById_notFound_returnsNull() {
        when(mockItemRepository.findById(999)).thenReturn(null);

        Item result = itemService.findItemById(999);

        assertNull(result);
    }

    // ─── getAllItem() ───────────────────────────────────────────

    @Test
    @DisplayName("Lấy danh sách sản phẩm — trả về danh sách đúng")
    void getAllItem_returnsAllItems() {
        ArrayList<Item> mockList = new ArrayList<>();
        mockList.add(new Item(1, "Item A", "Mô tả A", 100.0, "seller1", null, 0L));
        mockList.add(new Item(2, "Item B", "Mô tả B", 200.0, "seller2", null, 0L));
        when(mockItemRepository.getAllItemsFromDatabase()).thenReturn(mockList);

        ArrayList<Item> result = itemService.getAllItem();

        assertEquals(2, result.size());
        assertEquals("Item A", result.get(0).getName());
        assertEquals("Item B", result.get(1).getName());
    }

    @Test
    @DisplayName("Lấy danh sách sản phẩm — trả về danh sách rỗng")
    void getAllItem_emptyDatabase_returnsEmptyList() {
        when(mockItemRepository.getAllItemsFromDatabase()).thenReturn(new ArrayList<>());

        ArrayList<Item> result = itemService.getAllItem();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
