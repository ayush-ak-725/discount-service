package com.unifize.discountService;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfile {
    private String id;
    private String tier; // e.g., GOLD, SILVER, PREMIUM
}

