package com.unifize.discountService.exceptions;

public class DiscountCalculationException extends RuntimeException {
    public DiscountCalculationException(String message) {
        super(message);
    }
    public DiscountCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}

