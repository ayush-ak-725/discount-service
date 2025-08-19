package com.unifize.discountService;

import java.math.BigDecimal;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Voucher {
    private String code;
    private BigDecimal percentOff; // e.g., 0.69 == 69%
    private Set<String> excludedBrands;
    private Set<String> includedCategories; // empty or null => all categories
    private String minCustomerTier; // optional
    private BigDecimal maxDiscount; // optional cap
}

