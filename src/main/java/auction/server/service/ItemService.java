package auction.server.service;

import auction.server.model.Item;
import auction.server.repository.ItemRepository;

import java.util.ArrayList;

public class ItemService {
    private final ItemRepository itemRepository = new ItemRepository();
    public String addItem(String name, String description, double startingPrice, String sellerUserName){
        if (name == null || name.trim().isEmpty()) return "tên sản phẩm không được để trống";
        if (startingPrice <= 0) return "giá khởi điểm phải lớn hơn 0";
        if (sellerUserName == null || sellerUserName.trim().isEmpty()) return "tên người bán không được để trống";
        Item newItem = new Item(name, description, startingPrice, sellerUserName);
        itemRepository.saveItem(newItem);
        return "success";
    }
    public ArrayList<Item> getAllItem(){
        return itemRepository.getAllItems();
    }
}
