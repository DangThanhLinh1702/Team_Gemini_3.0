package auction.client.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.text.NumberFormat;
import java.util.Locale;

public class ProductItem {

    private static final NumberFormat VN_FORMAT = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private final String productId;
    private final StringProperty productName;
    private final StringProperty currentPrice;
    private final StringProperty leader;
    private final StringProperty leaderTitle;   // "Người dẫn đầu:" hoặc "Người thắng:"
    private final StringProperty status;
    private long rawPrice;
    private final String seller;
    private long endTime;
    private String imageBase64;
    private boolean finished;   // true khi phiên đã kết thúc

    public ProductItem(String productId, String productName, long rawPrice, String leader,
                       String status, String seller, long endTime) {
        this.productId    = productId;
        this.productName  = new SimpleStringProperty(productName);
        this.rawPrice     = rawPrice;
        this.currentPrice = new SimpleStringProperty(formatPrice(rawPrice));
        String displayLeader = (leader == null || leader.isEmpty()) ? "Chưa có ai" : leader;
        this.leader       = new SimpleStringProperty(displayLeader);
        this.leaderTitle  = new SimpleStringProperty("Người dẫn đầu:");
        this.status       = new SimpleStringProperty(status);
        this.seller       = seller;
        this.endTime      = endTime;
        this.finished     = "Kết thúc".equals(status);
    }

    public static String formatPrice(long price) {
        return VN_FORMAT.format(price) + " VNĐ";
    }

    public String getProductId()    { return productId; }

    public String getProductName()  { return productName.get(); }
    public StringProperty productNameProperty() { return productName; }

    public String getCurrentPrice() { return currentPrice.get(); }
    public StringProperty currentPriceProperty() { return currentPrice; }

    public String getLeader()       { return leader.get(); }
    public void setLeader(String l) { this.leader.set(l == null || l.isEmpty() ? "Chưa có ai" : l); }
    public StringProperty leaderProperty() { return leader; }

    /** Tiêu đề leader: đổi khi phiên kết thúc */
    public String getLeaderTitle()  { return leaderTitle.get(); }
    public StringProperty leaderTitleProperty() { return leaderTitle; }

    public String getStatus()       { return status.get(); }
    public void setStatus(String s) {
        this.status.set(s);
        if ("Kết thúc".equals(s)) {
            this.finished = true;
            this.leaderTitle.set("🏆 Người thắng:");
        }
    }
    public StringProperty statusProperty() { return status; }

    public long getRawPrice()       { return rawPrice; }
    public void setRawPrice(long rawPrice) {
        this.rawPrice = rawPrice;
        this.currentPrice.set(formatPrice(rawPrice));
    }

    public String getSeller()       { return seller; }

    public long getEndTime()        { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public String getImageBase64()  { return imageBase64; }
    public void setImageBase64(String b64) { this.imageBase64 = b64; }

    public boolean isFinished()     { return finished; }
    public void setFinished(boolean f) {
        this.finished = f;
        if (f) {
            this.status.set("Kết thúc");
            this.leaderTitle.set("🏆 Người thắng:");
        }
    }
}