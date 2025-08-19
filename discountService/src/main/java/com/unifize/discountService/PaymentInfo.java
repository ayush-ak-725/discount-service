package com.unifize.discountService;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInfo {
    private String method; // CARD, UPI, etc or "VOUCHER:CODE"
    private String bankName; // Optional
    private String cardType; // Optional: CREDIT, DEBIT
}

