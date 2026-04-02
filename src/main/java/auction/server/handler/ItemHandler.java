package auction.server.handler;

import auction.server.service.ItemService;
import auction.server.util.HttpServerUtil;
import auction.shared.dto.ItemDTO;
import auction.shared.dto.ResponseDTO;
import auction.shared.util.HttpResponseUtil;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class ItemHandler implements HttpHandler {
    private ItemService itemService = new ItemService();
    private Gson gson = new Gson();
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        //lay ve request method tu client gui len
        try {


            String method = exchange.getRequestMethod().toUpperCase();
            if ("GET".equals(method)) {
                var listItem = itemService.getAllItem();
                ResponseDTO response = new ResponseDTO("success", "lây danh sách sản phẩm thành công", listItem);
                HttpResponseUtil.sendHttpResponse(exchange, 200, response);
                return;
            }
            if ("POST".equals(method)) {
                String jsonBody = HttpServerUtil.readRequestBody(exchange);
                if (jsonBody.isEmpty()) {
                    HttpResponseUtil.sendHttpResponse(exchange, 400, new ResponseDTO("fail", "dữ liệu trống"));
                    return;
                }
                // chuyen json -> obj thuoc class ItemDTO
                ItemDTO itemDTO = gson.fromJson(jsonBody, ItemDTO.class);
                String resultMessage = itemService.addItem(itemDTO.getName(), itemDTO.getDescription(), itemDTO.getStartingPrice(), itemDTO.getSellerUsername());
                if ("success".equals(resultMessage)) {
                    HttpResponseUtil.sendHttpResponse(exchange, 201, new ResponseDTO("success", "thêm sản phẩm thành công"));
                } else {
                    HttpResponseUtil.sendHttpResponse(exchange, 400, new ResponseDTO("fail", resultMessage));
                }
                return;
            }
            HttpResponseUtil.sendHttpResponse(exchange, 405, new ResponseDTO("fail", "phương thức không hỗ trợ"));
        }catch (Exception e) {
            HttpResponseUtil.sendHttpResponse(exchange, 500, new ResponseDTO("error", "Lỗi nội bộ Server: " + e.getMessage()));
        }
    }
}
