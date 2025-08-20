package com.unifize.discountService.models;

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
    private BigDecimal percentOff;
    private Set<String> excludedBrands;
    private Set<String> includedCategories;
    private String minCustomerTier;
    private BigDecimal maxDiscount;
}
