package auction.server.handler;

import auction.server.core.AuctionManager;
import auction.server.service.ItemService;
import auction.shared.dto.ResponseDTO;
import auction.shared.util.HttpResponseUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class ItemListHandler implements HttpHandler {
    private final ItemService itemService = new ItemService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if(!"GET".equals(exchange.getRequestMethod())){
            HttpResponseUtil.sendHttpResponse(exchange, 405,
                    new ResponseDTO("fail", "phương thức không hỗ trợ", null));
            return;
        }
        try {
            String path = exchange.getRequestURI().getPath();

            if ("/items".equals(path)) {
                // Gson sẽ tự động đóng gói cả imageData và endTime gửi về cho Client
                var listItems = itemService.getAllItem();
                ResponseDTO response = new ResponseDTO("success",
                        "Lấy danh sách sản phẩm thành công", listItems);
                HttpResponseUtil.sendHttpResponse(exchange, 200, response);
            }
            else if ("/auctions".equals(path)) {
                var auctionSessions = AuctionManager.getInstance().getAllSessions();
                ResponseDTO response = new ResponseDTO("success",
                        "Lấy danh sách phiên đấu giá thành công", auctionSessions);
                HttpResponseUtil.sendHttpResponse(exchange, 200, response);
            }
            else {
                HttpResponseUtil.sendHttpResponse(exchange, 404,
                        new ResponseDTO("fail", "Đường dẫn không tồn tại", null));
            }
        } catch (Exception e) {
            HttpResponseUtil.sendHttpResponse(exchange, 500,
                    new ResponseDTO("error", "Lỗi Server: " + e.getMessage(), null));
        }
    }
}