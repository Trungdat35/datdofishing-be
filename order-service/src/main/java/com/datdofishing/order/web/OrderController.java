package com.datdofishing.order.web;

import com.datdofishing.common.security.CurrentUser;
import com.datdofishing.common.web.PageResponse;
import com.datdofishing.order.service.OrderDtos.CancelRequest;
import com.datdofishing.order.service.OrderDtos.CreateOrderRequest;
import com.datdofishing.order.service.OrderDtos.OrderResponse;
import com.datdofishing.order.service.OrderDtos.OrderSummary;
import com.datdofishing.order.service.OrderService;
import com.datdofishing.order.service.PlaceOrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** API đặt hàng của khách. Tất cả đều cần đăng nhập. */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final PlaceOrderService placeOrder;
    private final OrderService orders;
    private final String instanceId;

    public OrderController(PlaceOrderService placeOrder, OrderService orders,
                           @Value("${datdofishing.instance-id}") String instanceId) {
        this.placeOrder = placeOrder;
        this.orders = orders;
        this.instanceId = instanceId;
    }

    /** Trả 202: đơn đã tạo ở trạng thái PENDING, kho sẽ xác nhận sau vài giây (theo dõi qua SSE). */
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest req,
                                                @RequestHeader(value = "Idempotency-Key", required = false)
                                                String idempotencyKey) {
        OrderService.Buyer buyer = new OrderService.Buyer(CurrentUser.id(), CurrentUser.email(), CurrentUser.fullName());
        PlaceOrderService.Result r = placeOrder.place(buyer, req, idempotencyKey);
        return ResponseEntity.status(r.replayed() ? HttpStatus.OK : HttpStatus.ACCEPTED).body(r.order());
    }

    @GetMapping
    public PageResponse<OrderSummary> myOrders(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        return orders.myOrders(CurrentUser.id(), page, size);
    }

    /** Xem request đang được instance nào xử lý (kiểm tra load balancer). */
    @GetMapping("/instance")
    public Map<String, String> instance() {
        return Map.of("instanceId", instanceId);
    }

    @GetMapping("/{code}")
    public OrderResponse get(@PathVariable String code) {
        return orders.getForUser(CurrentUser.id(), code, CurrentUser.isAdmin());
    }

    @PostMapping("/{code}/cancel")
    public OrderResponse cancel(@PathVariable String code, @Valid @RequestBody(required = false) CancelRequest req) {
        return orders.cancelByCustomer(CurrentUser.id(), code, req == null ? null : req.reason());
    }
}
