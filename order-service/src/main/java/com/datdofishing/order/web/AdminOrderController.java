package com.datdofishing.order.web;

import com.datdofishing.common.security.CurrentUser;
import com.datdofishing.common.web.PageResponse;
import com.datdofishing.order.domain.OrderStatus;
import com.datdofishing.order.service.OrderDtos.OrderResponse;
import com.datdofishing.order.service.OrderDtos.OrderSummary;
import com.datdofishing.order.service.OrderDtos.StatusStat;
import com.datdofishing.order.service.OrderDtos.UpdateStatusRequest;
import com.datdofishing.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

/** Quản lý đơn hàng cho site admin. */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orders;

    public AdminOrderController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping
    public PageResponse<OrderSummary> list(@RequestParam(required = false) String status,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return orders.adminSearch(status, q, page, size);
    }

    @GetMapping("/stats")
    public List<StatusStat> stats() {
        return orders.stats();
    }

    @GetMapping("/{code}")
    public OrderResponse get(@PathVariable String code) {
        return orders.getForUser(null, code, true);
    }

    /** Ví dụ body: {"status": "SHIPPING", "note": "Đã giao cho GHN"} */
    @PatchMapping("/{code}/status")
    public OrderResponse updateStatus(@PathVariable String code, @Valid @RequestBody UpdateStatusRequest req) {
        OrderStatus target = OrderStatus.valueOf(req.status().toUpperCase(Locale.ROOT));
        return orders.changeStatusByCode(code, target, req.note(), "admin:" + CurrentUser.email());
    }
}
