package com.datdofishing.cart.web;

import com.datdofishing.cart.service.CartService;
import com.datdofishing.cart.service.CartService.Cart;
import com.datdofishing.cart.service.CartService.ItemInput;
import com.datdofishing.common.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService carts;

    public CartController(CartService carts) {
        this.carts = carts;
    }

    public record AddItemRequest(@NotNull Long variantId, @Min(1) int quantity) {
    }

    public record QuantityRequest(@Min(0) int quantity) {
    }

    public record MergeRequest(List<ItemInput> items) {
    }

    @GetMapping
    public Cart get() {
        return carts.get(CurrentUser.id());
    }

    @PostMapping("/items")
    public Cart add(@Valid @RequestBody AddItemRequest req) {
        return carts.add(CurrentUser.id(), new ItemInput(req.variantId(), req.quantity()));
    }

    @PutMapping("/items/{variantId}")
    public Cart setQuantity(@PathVariable Long variantId, @Valid @RequestBody QuantityRequest req) {
        return carts.setQuantity(CurrentUser.id(), variantId, req.quantity());
    }

    @DeleteMapping("/items/{variantId}")
    public Cart remove(@PathVariable Long variantId) {
        return carts.remove(CurrentUser.id(), variantId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear() {
        carts.clear(CurrentUser.id());
    }

    @PostMapping("/merge")
    public Cart merge(@RequestBody MergeRequest req) {
        return carts.merge(CurrentUser.id(), req.items());
    }
}
