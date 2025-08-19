package com.unifize.discountService;

import java.math.BigDecimal;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String id;
    private String brand;
    private BrandTier brandTier;
    private String category;
    private BigDecimal basePrice;
    private BigDecimal currentPrice; // After brand/category discount
}
