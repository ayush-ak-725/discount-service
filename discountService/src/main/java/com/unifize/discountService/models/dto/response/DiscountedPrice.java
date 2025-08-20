package com.unifize.discountService.models.dto.response;

import java.math.BigDecimal;
import java.util.Map;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountedPrice {
    private BigDecimal originalPrice;
    private BigDecimal finalPrice;
    private Map<String, BigDecimal> appliedDiscounts; // discount_name -> amount
    private String message;
}
