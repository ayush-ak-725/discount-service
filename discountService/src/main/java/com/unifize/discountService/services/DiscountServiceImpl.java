package com.unifize.discountService.services;

import java.math.BigDecimal;
import java.util.*;

import com.unifize.discountService.exceptions.DiscountCalculationException;
import com.unifize.discountService.exceptions.DiscountValidationException;
import com.unifize.discountService.models.*;
import com.unifize.discountService.models.dto.response.DiscountedPrice;
import com.unifize.discountService.repository.InMemoryOffersRepository;
import com.unifize.discountService.utility.DiscountUtility;
import com.unifize.discountService.repository.OffersRepository;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.math.RoundingMode;
import org.springframework.validation.annotation.Validated;

@Service
@Slf4j
@Validated
public class DiscountServiceImpl implements DiscountService {

    private final OffersRepository offers;

    /**
     * Since we don't have a database,we are taking the
     * values from InMemory Offers Repository so defining it
     * under default constructor
     */
    public DiscountServiceImpl() {
        this.offers = InMemoryOffersRepository.defaultRepository();
    }

    public DiscountServiceImpl(OffersRepository offers) {
        this.offers = Objects.requireNonNull(offers, "OffersRepository cannot be null");
    }

    @Override
    public DiscountedPrice calculateCartDiscounts(
            List<CartItem> cartItems,
            CustomerProfile customer,
            Optional<PaymentInfo> paymentInfo
    ) throws DiscountCalculationException {
        try {
            if (cartItems == null || cartItems.isEmpty()) {
                return DiscountedPrice.builder()
                        .originalPrice(DiscountUtility.zero())
                        .finalPrice(DiscountUtility.zero())
                        .appliedDiscounts(Collections.emptyMap())
                        .message("No items in cart")
                        .build();
            }

            log.info("Brand offers: {}", offers.brandMinOff());
            log.info("CartItems: {}", cartItems);

            Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
            BigDecimal originalPrice = DiscountUtility.zero();

            BigDecimal afterBrandCategory = applyBrandCategoryDiscounts(cartItems, breakdown);
            BigDecimal runningTotal = afterBrandCategory;

            if (paymentInfo != null && paymentInfo.isPresent()) {
                runningTotal = applyVoucherIfPresent(paymentInfo.get(), cartItems, customer, runningTotal, breakdown);
            }

            if (paymentInfo != null && paymentInfo.isPresent()) {
                runningTotal = applyBankOfferIfPresent(paymentInfo.get(), runningTotal, breakdown);
            }

            originalPrice = cartItems.stream()
                    .filter(ci -> ci != null && ci.getProduct() != null)
                    .map(ci -> DiscountUtility.mul(Optional.ofNullable(ci.getProduct().getBasePrice()).orElse(BigDecimal.ZERO), ci.getQuantity()))
                    .reduce(DiscountUtility.zero(), DiscountUtility::sum);

            log.info("Discounts applied: {}", breakdown);

            return DiscountedPrice.builder()
                    .originalPrice(originalPrice)
                    .finalPrice(runningTotal)
                    .appliedDiscounts(breakdown)
                    .message("Discounts applied: " + breakdown.keySet())
                    .build();

        } catch (DiscountValidationException ex) {
            log.warn("Discount validation failed: {}", ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Failed calculating discounts", ex);
            throw new DiscountCalculationException("Failed calculating discounts", ex);
        }
    }

    private BigDecimal applyBrandCategoryDiscounts(List<CartItem> cartItems, Map<String, BigDecimal> breakdown) {
        BigDecimal total = DiscountUtility.zero();

        for (CartItem item : cartItems) {
            if (item == null) continue;

            Product p = item.getProduct();
            if (p == null) {
                log.warn("CartItem has null product: {}", item);
                continue;
            }

            BigDecimal basePrice = Optional.ofNullable(p.getBasePrice()).orElse(BigDecimal.ZERO);
            BigDecimal price = basePrice;

            BigDecimal brandPct = Optional.ofNullable(offers.brandMinOff().get(p.getBrand())).orElse(BigDecimal.ZERO);
            if (brandPct.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal discount = DiscountUtility.pct(price, brandPct);
                price = DiscountUtility.minus(price, discount);
                addDiscountBreakdown(breakdown, "BRAND(" + p.getBrand() + ")", DiscountUtility.mul(discount, item.getQuantity()));
            }

            BigDecimal catPct = Optional.ofNullable(offers.categoryExtraOff().get(p.getCategory())).orElse(BigDecimal.ZERO);
            if (catPct.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal discount = DiscountUtility.pct(price, catPct);
                price = DiscountUtility.minus(price, discount);
                addDiscountBreakdown(breakdown, "CATEGORY(" + p.getCategory() + ")", DiscountUtility.mul(discount, item.getQuantity()));
            }

            p.setCurrentPrice(price);
            total = DiscountUtility.sum(total, DiscountUtility.mul(price, item.getQuantity()));
        }

        return total;
    }

    private BigDecimal applyVoucherIfPresent(PaymentInfo paymentInfo, List<CartItem> cartItems,
                                             CustomerProfile customer, BigDecimal runningTotal,
                                             Map<String, BigDecimal> breakdown)
            throws DiscountValidationException {

        if (paymentInfo == null || paymentInfo.getMethod() == null) return runningTotal;

        String method = paymentInfo.getMethod().trim();
        if (!method.startsWith("VOUCHER:")) return runningTotal;

        String voucherCode = method.substring("VOUCHER:".length()).trim();
        if (voucherCode.isEmpty()) {
            log.warn("Voucher code is empty");
            return runningTotal;
        }

        Voucher v = offers.findVoucher(voucherCode).orElse(null);
        if (v == null) {
            throw new DiscountValidationException("Unknown voucher: " + voucherCode);
        }

        boolean applicable = true;

        if (v.getExcludedBrands() != null && !v.getExcludedBrands().isEmpty()) {
            for (CartItem ci : cartItems) {
                if (ci != null && ci.getProduct() != null && v.getExcludedBrands().contains(ci.getProduct().getBrand())) {
                    applicable = false;
                    break;
                }
            }
        }

        if (applicable && v.getIncludedCategories() != null && !v.getIncludedCategories().isEmpty()) {
            boolean anyMatch = cartItems.stream()
                    .filter(Objects::nonNull)
                    .filter(ci -> ci.getProduct() != null)
                    .anyMatch(ci -> v.getIncludedCategories().contains(ci.getProduct().getCategory()));
            if (!anyMatch) applicable = false;
        }

        if (applicable && v.getMinCustomerTier() != null) {
            String tier = customer != null ? customer.getTier() : null;
            if (tier == null || !tier.equalsIgnoreCase(v.getMinCustomerTier())) {
                applicable = false;
            }
        }

        if (!applicable) {
            log.warn("Voucher {} not applicable", voucherCode);
            return runningTotal;
        }

        BigDecimal voucherDisc = Optional.of(DiscountUtility.pct(runningTotal, v.getPercentOff()))
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        if (v.getMaxDiscount() != null) {
            voucherDisc = voucherDisc.min(v.getMaxDiscount());
        }

        runningTotal = DiscountUtility.minus(runningTotal, voucherDisc);
        addDiscountBreakdown(breakdown, "VOUCHER(" + voucherCode + ")", voucherDisc);

        return runningTotal;
    }

    private BigDecimal applyBankOfferIfPresent(PaymentInfo paymentInfo, BigDecimal runningTotal,
                                               Map<String, BigDecimal> breakdown) {
        if (paymentInfo == null || runningTotal == null) {
            return Optional.ofNullable(runningTotal).orElse(DiscountUtility.zero());
        }

        String bank = paymentInfo.getBankName();
        if (bank == null || bank.isEmpty()) {
            return runningTotal;
        }

        BigDecimal bankPct = Optional.ofNullable(offers.bankOffers().get(bank)).orElse(BigDecimal.ZERO);
        if (bankPct.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal bankDisc = DiscountUtility.pct(runningTotal, bankPct).setScale(2, RoundingMode.HALF_UP);
            runningTotal = DiscountUtility.minus(runningTotal, bankDisc);
            addDiscountBreakdown(breakdown, "BANK(" + bank + ")", bankDisc);
        }

        return runningTotal;
    }


    private void addDiscountBreakdown(Map<String, BigDecimal> breakdown, String key, BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) > 0) {
            breakdown.merge(key, value, BigDecimal::add);
        }
    }

    @Override
    public boolean validateDiscountCode(String code, List<CartItem> cartItems, CustomerProfile customer)
            throws DiscountValidationException {

        if (code == null || code.isEmpty()) return false;

        Voucher v = offers.findVoucher(code).orElse(null);
        if (v == null) return false;
        if (v.getExcludedBrands() != null && !v.getExcludedBrands().isEmpty() && cartItems != null) {
            for (CartItem ci : cartItems) {
                if (ci != null && ci.getProduct() != null && v.getExcludedBrands().contains(ci.getProduct().getBrand())) {
                    return false;
                }
            }
        }
        if (v.getIncludedCategories() != null && !v.getIncludedCategories().isEmpty() && cartItems != null) {
            boolean anyMatch = cartItems.stream()
                    .filter(Objects::nonNull)
                    .filter(ci -> ci.getProduct() != null)
                    .anyMatch(ci -> v.getIncludedCategories().contains(ci.getProduct().getCategory()));
            if (!anyMatch) return false;
        }
        if (v.getMinCustomerTier() != null) {
            String tier = customer != null ? customer.getTier() : null;
            return tier != null && tier.equalsIgnoreCase(v.getMinCustomerTier());
        }
        return true;
    }
}
