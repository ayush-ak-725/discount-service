package com.unifize.discountService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public interface OffersRepository {
    Map<String, BigDecimal> brandMinOff(); // brand -> percent (0.40 means 40%)
    Map<String, BigDecimal> categoryExtraOff(); // category -> percent (0.10 means 10%)
    Map<String, BigDecimal> bankOffers(); // bank -> percent (0.10 means 10%)
    Optional<Voucher> findVoucher(String code);
}

