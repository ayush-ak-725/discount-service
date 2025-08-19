package com.unifize.discountService;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class DefaultDiscountService implements DiscountService {

    private final OffersRepository offers;

    public DefaultDiscountService() {
        this.offers = InMemoryOffersRepository.defaultRepository();
    }

    public DefaultDiscountService(OffersRepository offers) {
        this.offers = offers;
    }

    @Override
    public DiscountedPrice calculateCartDiscounts(
            List<CartItem> cartItems,
            CustomerProfile customer,
            Optional<PaymentInfo> paymentInfo) throws DiscountCalculationException {
        try {
            Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
            BigDecimal original = Money.zero();
            BigDecimal afterBrandCategory = Money.zero();

            // 1) Apply brand/category to each product -> set currentPrice
            for (CartItem item : cartItems) {
                Product p = item.getProduct();
                BigDecimal base = p.getBasePrice();
                original = Money.sum(original, Money.mul(base, item.getQuantity()));

                BigDecimal price = base;

                // brand discounts
                BigDecimal brandPct = offers.brandMinOff()
                        .getOrDefault(p.getBrand(), BigDecimal.ZERO);
                if (brandPct.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal disc = Money.pct(price, brandPct);
                    price = Money.minus(price, disc);
                    breakdown.merge(
                            "BRAND(" + p.getBrand() + ")",
                            Money.mul(disc, item.getQuantity()),
                            BigDecimal::add
                    );
                }

                // category discounts
                BigDecimal catPct = offers.categoryExtraOff()
                        .getOrDefault(p.getCategory(), BigDecimal.ZERO);
                if (catPct.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal disc = Money.pct(price, catPct);
                    price = Money.minus(price, disc);
                    breakdown.merge(
                            "CATEGORY(" + p.getCategory() + ")",
                            Money.mul(disc, item.getQuantity()),
                            BigDecimal::add
                    );
                }

                p.setCurrentPrice(price);
                afterBrandCategory = Money.sum(
                        afterBrandCategory,
                        Money.mul(price, item.getQuantity())
                );
            }

            BigDecimal runningTotal = afterBrandCategory;

            // 2) Apply voucher if present in PaymentInfo.method
            String voucherCode;
            if (paymentInfo.isPresent()
                    && paymentInfo.get().getMethod() != null
                    && paymentInfo.get().getMethod().startsWith("VOUCHER:")) {
                voucherCode = paymentInfo.get().getMethod()
                        .substring("VOUCHER:".length())
                        .trim();
            } else {
                voucherCode = null;
            }

            if (voucherCode != null && !voucherCode.isEmpty()) {
                if (!validateDiscountCode(voucherCode, cartItems, customer)) {
                    throw new DiscountValidationException(
                            "Voucher not applicable: " + voucherCode
                    );
                }
                Voucher v = offers.findVoucher(voucherCode)
                        .orElseThrow(() -> new DiscountValidationException(
                                "Unknown voucher: " + voucherCode
                        ));

                BigDecimal voucherDisc = Money.pct(runningTotal, v.getPercentOff());
                if (v.getMaxDiscount() != null) {
                    voucherDisc = voucherDisc.min(v.getMaxDiscount());
                }

                runningTotal = Money.minus(runningTotal, voucherDisc);

                // ✅ Fix: use a final variable for lambda
                final BigDecimal appliedVoucherDisc = voucherDisc;
                breakdown.merge(
                        "VOUCHER(" + voucherCode + ")",
                        appliedVoucherDisc,
                        BigDecimal::add
                );
            }

            // 3) Bank offer
            if (paymentInfo.isPresent() && paymentInfo.get().getBankName() != null) {
                String bank = paymentInfo.get().getBankName();
                BigDecimal bankPct = offers.bankOffers().getOrDefault(bank, BigDecimal.ZERO);
                if (bankPct.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal bankDisc = Money.pct(runningTotal, bankPct);
                    runningTotal = Money.minus(runningTotal, bankDisc);

                    // ✅ fix for lambda
                    final BigDecimal appliedBankDisc = bankDisc;
                    breakdown.merge("BANK(" + bank + ")", appliedBankDisc, BigDecimal::add);
                }
            }


            return DiscountedPrice.builder()
                    .originalPrice(original)
                    .finalPrice(runningTotal)
                    .appliedDiscounts(breakdown)
                    .message("Discounts applied: " + breakdown.keySet())
                    .build();
        } catch (RuntimeException ex) {
            throw new DiscountCalculationException("Failed calculating discounts", ex);
        }
    }

    @Override
    public boolean validateDiscountCode(
            String code,
            List<CartItem> cartItems,
            CustomerProfile customer
    ) throws DiscountValidationException {
        Voucher v = offers.findVoucher(code).orElse(null);
        if (v == null) return false;

        // Brand exclusions
        if (v.getExcludedBrands() != null && !v.getExcludedBrands().isEmpty()) {
            for (CartItem ci : cartItems) {
                if (v.getExcludedBrands().contains(ci.getProduct().getBrand())) {
                    return false;
                }
            }
        }

        // Category restrictions
        if (v.getIncludedCategories() != null && !v.getIncludedCategories().isEmpty()) {
            boolean anyMatch = cartItems.stream()
                    .anyMatch(ci -> v.getIncludedCategories()
                            .contains(ci.getProduct().getCategory()));
            if (!anyMatch) return false;
        }

        // Customer tier requirement
        if (v.getMinCustomerTier() != null
                && customer != null
                && customer.getTier() != null) {
            if (!customer.getTier().equalsIgnoreCase(v.getMinCustomerTier())) {
                return false;
            }
        } else if (v.getMinCustomerTier() != null) {
            return false;
        }

        return true;
    }
}
