package com.unifize.discountService.repository;

import com.unifize.discountService.models.Voucher;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryOffersRepository implements OffersRepository {
    private final Map<String, BigDecimal> brand;
    private final Map<String, BigDecimal> category;
    private final Map<String, BigDecimal> bank;
    private final Map<String, Voucher> vouchers;

    public InMemoryOffersRepository(
            Map<String, BigDecimal> brand,
            Map<String, BigDecimal> category,
            Map<String, BigDecimal> bank,
            Map<String, Voucher> vouchers) {
        this.brand = brand;
        this.category = category;
        this.bank = bank;
        this.vouchers = vouchers;
    }

    @Override public Map<String, BigDecimal> brandMinOff() { return brand; }
    @Override public Map<String, BigDecimal> categoryExtraOff() { return category; }
    @Override public Map<String, BigDecimal> bankOffers() { return bank; }
    @Override public Optional<Voucher> findVoucher(String code) {
        return Optional.ofNullable(vouchers.get(code));
    }

    public static InMemoryOffersRepository defaultRepository() {
        Map<String, BigDecimal> brand = java.util.Map.of(
                "PUMA", new BigDecimal("0.40")
        );
        Map<String, BigDecimal> category = java.util.Map.of(
                "T-shirts", new BigDecimal("0.10")
        );
        Map<String, BigDecimal> bank = java.util.Map.of(
                "ICICI", new BigDecimal("0.10")
        );
        Map<String, Voucher> vouchers = java.util.Map.of(
                "SUPER69", Voucher.builder()
                        .code("SUPER69")
                        .percentOff(new BigDecimal("0.69"))
                        .excludedBrands(java.util.Set.of())
                        .includedCategories(java.util.Set.of()) // all
                        .minCustomerTier(null)
                        .maxDiscount(null)
                        .build()
        );
        return new InMemoryOffersRepository(brand, category, bank, vouchers);
    }
}
