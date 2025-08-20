package com.unifize.discountService.utility;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class DiscountUtility {

    public static BigDecimal pct(BigDecimal amount, BigDecimal percent) {
        if (amount == null) amount = BigDecimal.ZERO;
        if (percent == null) percent = BigDecimal.ZERO;
        return amount.multiply(percent).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal minus(BigDecimal amount, BigDecimal discount) {
        if (amount == null) amount = BigDecimal.ZERO;
        if (discount == null) discount = BigDecimal.ZERO;
        return amount.subtract(discount).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal sum(BigDecimal a, BigDecimal b) {
        if (a == null) a = BigDecimal.ZERO;
        if (b == null) b = BigDecimal.ZERO;
        return a.add(b).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal mul(BigDecimal a, int qty) {
        if (a == null) a = BigDecimal.ZERO;
        return a.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
}
