package com.unifize.discountService.utility;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class DiscountUtility {
    public static BigDecimal pct(BigDecimal amount, BigDecimal percent) {
        if (amount == null || percent == null)
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return amount.multiply(percent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal minus(BigDecimal amount, BigDecimal discount) {
        return amount.subtract(discount).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal sum(BigDecimal a, BigDecimal b) {
        return a.add(b).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal mul(BigDecimal a, int qty) {
        return a.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal zero() { return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP); }
}
