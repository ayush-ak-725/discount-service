package com.unifize.discountService.controller;

import com.unifize.discountService.exceptions.DiscountCalculationException;
import com.unifize.discountService.exceptions.DiscountValidationException;
import com.unifize.discountService.models.dto.response.DiscountedPrice;
import com.unifize.discountService.models.dto.request.DiscountRequest;
import com.unifize.discountService.services.DiscountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/discounts")
public class DiscountController {

    private final DiscountService discountService;

    @Autowired
    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    /**
     * Endpoint to calculate discounts for a cart.
     *
     * POST /api/discounts/calculate
     *
     * Body: JSON with cartItems, customer, paymentInfo (optional)
     */
    @PostMapping("/calculate")
    public ResponseEntity<DiscountedPrice> calculateDiscount(
            @RequestBody DiscountRequest request
    ) {
        try {
            DiscountedPrice discountedPrice = discountService.calculateCartDiscounts(
                    request.getCartItems(),
                    request.getCustomer(),
                    Optional.ofNullable(request.getPaymentInfo())
            );
            return ResponseEntity.ok(discountedPrice);
        } catch (DiscountCalculationException | DiscountValidationException e) {
            return ResponseEntity.badRequest().body(
                    DiscountedPrice.builder()
                            .originalPrice(null)
                            .finalPrice(null)
                            .appliedDiscounts(null)
                            .message("Error calculating discounts: " + e.getMessage())
                            .build()
            );
        }
    }
}

