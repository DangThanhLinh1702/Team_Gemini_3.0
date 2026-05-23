package auction.client.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ProductItem {

    private final String productId;
    private final StringProperty productName;
    private final StringProperty currentPrice;
    private final StringProperty leader;
    private final StringProperty status;
    private long rawPrice;

    // Lưu thông tin người bán và thời gian kết thúc để Client tự kiểm tra
    private final String seller;
    private final long endTime;

    // ĐÂY LÀ BIẾN MỚI ĐỂ LƯU ẢNH
    private String imageBase64;

    public ProductItem(String productId, String productName, long rawPrice, String leader, String status, String seller, long endTime) {
        this.productId    = productId;
        this.productName  = new SimpleStringProperty(productName);
        this.rawPrice     = rawPrice;
        this.currentPrice = new SimpleStringProperty(String.format("%,d VNĐ", rawPrice));
        this.leader       = new SimpleStringProperty(leader.isEmpty() ? "---" : leader);
        this.status       = new SimpleStringProperty(status);
        this.seller       = seller;
        this.endTime      = endTime;
    }

    public String getProductId()    { return productId; }

    public String getProductName()  { return productName.get(); }
    public StringProperty productNameProperty() { return productName; }

    public String getCurrentPrice() { return currentPrice.get(); }
    public void setCurrentPrice(String price) { this.currentPrice.set(price); }
    public StringProperty currentPriceProperty() { return currentPrice; }

    public String getLeader()       { return leader.get(); }
    public void setLeader(String leader) { this.leader.set(leader); }
    public StringProperty leaderProperty() { return leader; }

    public String getStatus()       { return status.get(); }
    public void setStatus(String status) { this.status.set(status); }
    public StringProperty statusProperty() { return status; }

    public long getRawPrice()       { return rawPrice; }
    public void setRawPrice(long rawPrice) { this.rawPrice = rawPrice; }

    public String getSeller()       { return seller; }

    public long getEndTime()        { return endTime; }

    // GETTER & SETTER CHO ẢNH
    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
}