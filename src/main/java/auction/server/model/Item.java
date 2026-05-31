package auction.server.model;

import auction.server.repository.ItemRepository;

public class Item {
    private int id;
    private String name;
    private String description;
    private double startingPrice;
    private String sellerUserName;
    private int sellerId;
    private final ItemRepository itemRepository = new ItemRepository();

    // ĐÃ ĐỔI TÊN: imageData thành image để khớp với Client
    private String image;
    private long endTime;

    public Item(String name, String description, double startingPrice, String sellerUserName, String image, long endTime) {
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.sellerUserName = sellerUserName;
        this.image = image;
        this.endTime = endTime;
    }
    public Item() {
    }

    public Item(int id, String name, String description, double startingPrice, String sellerUserName, String image, long endTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.sellerUserName = sellerUserName;
        this.image = image;
        this.endTime = endTime;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public String getSellerUserName() { return sellerUserName; }
    public void setSellerUserName(String sellerUserName) { this.sellerUserName = sellerUserName; }

    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }

    // ĐÃ ĐỔI TÊN HÀM CHO KHỚP
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
}