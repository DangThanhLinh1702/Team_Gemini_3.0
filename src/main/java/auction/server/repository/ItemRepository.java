package auction.server.repository;

import auction.server.model.Item;

import java.util.ArrayList;

public class ItemRepository {
    private static final ArrayList<Item> allItems = new ArrayList<>();
    public void saveItem(Item item){
        allItems.add(item);
        System.out.println("da them san pham : " + item.getName());
    }
    public ArrayList<Item> getAllItems(){
        return allItems;
    }

}
