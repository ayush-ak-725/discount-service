package com.unifize.discountService.models.dto.request;

import java.util.List;

import com.unifize.discountService.models.CartItem;
import com.unifize.discountService.models.CustomerProfile;
import com.unifize.discountService.models.PaymentInfo;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountRequest {
    private List<CartItem> cartItems;
    private CustomerProfile customer;
    private PaymentInfo paymentInfo;
}
