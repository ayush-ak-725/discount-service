package com.unifize.discountService.models;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
@Getter
public class TestScenario {
    public String name;
    public CartItem cartItem;
    public CustomerProfile customer;
    public PaymentInfo paymentInfo;
    public int expectedDiscounts;
}
