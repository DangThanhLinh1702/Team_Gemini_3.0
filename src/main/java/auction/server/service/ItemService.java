package auction.server.service;

import auction.server.model.Item;
import auction.server.repository.ItemRepository;

import java.util.ArrayList;

public class ItemService {
    private final ItemRepository itemRepository;

    // Constructor cho dependency injection (dùng cho testing)
    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    // Constructor mặc định
    public ItemService() {
        this.itemRepository = new ItemRepository();
    }


    // ĐÃ SỬA: Nhận thêm biến image và endTime
    public String addItem(String name, String description, double startingPrice, String sellerUserName, String image, long endTime){
        if (name == null || name.trim().isEmpty()) return "tên sản phẩm không được để trống";
        if (startingPrice <= 0) return "giá khởi điểm phải lớn hơn 0";
        if (sellerUserName == null || sellerUserName.trim().isEmpty()) return "tên người bán không được để trống";

        // ĐÃ SỬA: Dùng hàm khởi tạo 6 tham số để gộp cả Ảnh và Thời gian vào
        Item newItem = new Item(name, description, startingPrice, sellerUserName, image, endTime);
        itemRepository.saveItem(newItem);
        return "success";
    }

    public Item getLastInsertedItem() {
        return itemRepository.findLastInserted();
    }

    public Item findItemById(int id) {
        return itemRepository.findById(id);
    }

    public ArrayList<Item> getAllItem(){
        return itemRepository.getAllItemsFromDatabase();
    }
}