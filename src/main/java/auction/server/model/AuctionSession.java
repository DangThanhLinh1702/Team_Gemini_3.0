package auction.server.model;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Đại diện cho một phiên đấu giá trong RAM.
 * Mỗi phiên gắn với 1 item, theo dõi giá hiện tại, người dẫn đầu, lịch sử đặt giá.
 */
public class AuctionSession {
    private int auctionId;   // ID trong bảng auctions (DB)
    private int itemId;
    private int sellerId;
    private double currentPrice;
    private String highestBidder;
    private boolean isFinished;
    private Timestamp startTime;
    private Timestamp endTime;

    // Lịch sử đặt giá trong phiên (chỉ lưu trong RAM, hiển thị realtime)
    private final List<String> bidHistory = new ArrayList<>();

    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");

    /**
     * Constructor chính - dùng khi tạo phiên mới từ code.
     */
    public AuctionSession(int itemId, int sellerId, double startingPrice,
                          Timestamp startTime, Timestamp endTime) {
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.currentPrice = startingPrice;
        this.highestBidder = "Chưa có ai";
        this.isFinished = false;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Đặt giá mới. Trả về true nếu hợp lệ và được chấp nhận.
     * Yêu cầu: phiên đang chạy VÀ giá mới PHẢI lớn hơn giá hiện tại.
     */
    public synchronized boolean placeBid(String username, double newPrice) {
        if (!isAuctionRunning()) return false;
        if (newPrice <= this.currentPrice) return false;

        this.currentPrice = newPrice;
        this.highestBidder = username;

        // Ghi lịch sử vào RAM (hiển thị realtime cho client)
        String timeStr = SDF.format(new Timestamp(System.currentTimeMillis()));
        bidHistory.add(String.format("[%s] %s → %,.0f VNĐ", timeStr, username, newPrice));
        return true;
    }

    /**
     * Kiểm tra phiên có đang chạy không (chưa kết thúc và chưa hết giờ).
     */
    public boolean isAuctionRunning() {
        if (isFinished) return false;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return now.before(endTime);
    }

    /**
     * Đánh dấu phiên kết thúc, ghi dòng cuối vào lịch sử.
     */
    public synchronized void finishAuction() {
        this.isFinished = true;
        String timeStr = SDF.format(new Timestamp(System.currentTimeMillis()));
        bidHistory.add(String.format("[%s] ⏰ Phiên kết thúc — Người thắng: %s với giá %,.0f VNĐ",
                timeStr, highestBidder, currentPrice));
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int getAuctionId()              { return auctionId; }
    public void setAuctionId(int id)       { this.auctionId = id; }

    public int getItemId()                 { return itemId; }
    public void setItemId(int id)          { this.itemId = id; }

    public int getSellerId()               { return sellerId; }
    public void setSellerId(int id)        { this.sellerId = id; }

    public double getCurrentPrice()        { return currentPrice; }
    public void setCurrentPrice(double p)  { this.currentPrice = p; }

    public String getHighestBidder()       { return highestBidder; }
    public void setHighestBidder(String b) { this.highestBidder = b; }

    public boolean isFinished()            { return isFinished; }
    public void setFinished(boolean f)     { this.isFinished = f; }

    public Timestamp getStartTime()        { return startTime; }
    public void setStartTime(Timestamp t)  { this.startTime = t; }

    public Timestamp getEndTime()          { return endTime; }
    public void setEndTime(Timestamp t)    { this.endTime = t; }

    /** Trả về bản sao danh sách lịch sử (tránh bị sửa từ bên ngoài) */
    public List<String> getBidHistory()    { return new ArrayList<>(bidHistory); }

    /**
     * Nạp lại lịch sử từ DB vào RAM (gọi sau khi server khởi động lại).
     * Xóa dữ liệu cũ trong RAM trước khi nạp để tránh trùng lặp.
     */
    public synchronized void loadBidHistory(List<String> history) {
        bidHistory.clear();
        if (history != null) {
            bidHistory.addAll(history);
        }
    }
}