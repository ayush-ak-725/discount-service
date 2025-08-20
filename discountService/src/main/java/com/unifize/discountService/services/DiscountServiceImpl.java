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
import jakarta.validation.constraints.NotEmpty;

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
            @NotEmpty List<CartItem> cartItems,
            CustomerProfile customer,
            Optional<PaymentInfo> paymentInfo
    ) throws DiscountCalculationException {
        try {
            log.info(offers.brandMinOff().toString());
            log.info(cartItems.toString());
            Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
            BigDecimal originalPrice = DiscountUtility.zero();
            BigDecimal afterBrandCategory = DiscountUtility.zero();

            // 1) Apply brand & category discounts
            afterBrandCategory = applyBrandCategoryDiscounts(cartItems, breakdown);

            // Running total after brand/category
            BigDecimal runningTotal = afterBrandCategory;

            // 2) Apply voucher if present
            if (paymentInfo.isPresent()) {
                runningTotal = applyVoucherIfPresent(paymentInfo.get(), cartItems, customer, runningTotal, breakdown);
            }

            // 3) Apply bank offer if present
            if (paymentInfo.isPresent()) {
                runningTotal = applyBankOfferIfPresent(paymentInfo.get(), runningTotal, breakdown);
            }

            // Compute original price
            originalPrice = cartItems.stream()
                    .map(ci -> DiscountUtility.mul(ci.getProduct().getBasePrice(), ci.getQuantity()))
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
            Product p = item.getProduct();
            BigDecimal price = p.getBasePrice();

            // Brand discount
            BigDecimal brandPct = offers.brandMinOff().getOrDefault(p.getBrand(), BigDecimal.ZERO);
            if (brandPct.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal discount = DiscountUtility.pct(price, brandPct);
                price = DiscountUtility.minus(price, discount);
                addDiscountBreakdown(breakdown, "BRAND(" + p.getBrand() + ")", DiscountUtility.mul(discount, item.getQuantity()));
            }

            // Category discount
            BigDecimal catPct = offers.categoryExtraOff().getOrDefault(p.getCategory(), BigDecimal.ZERO);
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

        String method = paymentInfo.getMethod();
        if (method != null && method.startsWith("VOUCHER:")) {
            String voucherCode = method.substring("VOUCHER:".length()).trim();
            if (!validateDiscountCode(voucherCode, cartItems, customer)) {
                throw new DiscountValidationException("Voucher not applicable: " + voucherCode);
            }

            Voucher v = offers.findVoucher(voucherCode)
                    .orElseThrow(() -> new DiscountValidationException("Unknown voucher: " + voucherCode));

            BigDecimal voucherDisc = DiscountUtility.pct(runningTotal, v.getPercentOff());
            if (v.getMaxDiscount() != null) {
                voucherDisc = voucherDisc.min(v.getMaxDiscount());
            }
            voucherDisc = voucherDisc.setScale(2, RoundingMode.HALF_UP);

            runningTotal = DiscountUtility.minus(runningTotal, voucherDisc);
            addDiscountBreakdown(breakdown, "VOUCHER(" + voucherCode + ")", voucherDisc);
        }

        return runningTotal;
    }

    private BigDecimal applyBankOfferIfPresent(PaymentInfo paymentInfo, BigDecimal runningTotal,
                                               Map<String, BigDecimal> breakdown) {
        String bank = paymentInfo.getBankName();
        if (bank != null) {
            BigDecimal bankPct = offers.bankOffers().getOrDefault(bank, BigDecimal.ZERO);
            if (bankPct.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal bankDisc = DiscountUtility.pct(runningTotal, bankPct).setScale(2, RoundingMode.HALF_UP);
                runningTotal = DiscountUtility.minus(runningTotal, bankDisc);
                addDiscountBreakdown(breakdown, "BANK(" + bank + ")", bankDisc);
            }
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

        Voucher v = offers.findVoucher(code).orElse(null);
        if (v == null) return false;

        // Brand exclusions
        if (v.getExcludedBrands() != null && !v.getExcludedBrands().isEmpty()) {
            for (CartItem ci : cartItems) {
                if (v.getExcludedBrands().contains(ci.getProduct().getBrand())) return false;
            }
        }

        // Category inclusions
        if (v.getIncludedCategories() != null && !v.getIncludedCategories().isEmpty()) {
            boolean anyMatch = cartItems.stream()
                    .anyMatch(ci -> v.getIncludedCategories().contains(ci.getProduct().getCategory()));
            if (!anyMatch) return false;
        }

        // Customer tier requirement
        if (v.getMinCustomerTier() != null) {
            if (customer == null || customer.getTier() == null ||
                    !customer.getTier().equalsIgnoreCase(v.getMinCustomerTier())) {
                return false;
            }
        }

        return true;
    }
}
