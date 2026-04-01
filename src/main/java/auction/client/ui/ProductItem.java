package auction.client.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * ProductItem - Model đại diện cho 1 sản phẩm trong TableView
 *
 * Dùng JavaFX Property để TableView tự động cập nhật khi dữ liệu thay đổi
 */
public class ProductItem {

    private final String productId;                      // ID sản phẩm (không đổi)
    private final StringProperty productName;            // Tên sản phẩm
    private final StringProperty currentPrice;           // Giá hiển thị (dạng text có format)
    private final StringProperty leader;                 // Người dẫn đầu
    private final StringProperty status;                 // Trạng thái (Đang đấu / Kết thúc)
    private long rawPrice;                               // Giá thực (dạng số để so sánh)

    public ProductItem(String productId, String productName, long rawPrice, String leader, String status) {
        this.productId    = productId;
        this.productName  = new SimpleStringProperty(productName);
        this.rawPrice     = rawPrice;
        this.currentPrice = new SimpleStringProperty(String.format("%,d VNĐ", rawPrice));
        this.leader       = new SimpleStringProperty(leader.isEmpty() ? "---" : leader);
        this.status       = new SimpleStringProperty(status);
    }

    // ==================== Getter / Setter ====================

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
}