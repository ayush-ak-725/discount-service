package com.unifize.discountService.services;

import com.unifize.discountService.exceptions.DiscountCalculationException;
import com.unifize.discountService.exceptions.DiscountValidationException;
import com.unifize.discountService.models.*;
import com.unifize.discountService.models.dto.response.DiscountedPrice;
import com.unifize.discountService.repository.OffersRepository;
import com.unifize.discountService.utility.DiscountUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiscountServiceUnitTest {

    @Mock
    private OffersRepository offersRepository;

    @InjectMocks
    private DiscountServiceImpl discountService;

    private Product product;
    private CartItem cartItem;
    private CustomerProfile customer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        product = Product.builder()
                .brand("PUMA")
                .category("T-shirts")
                .basePrice(new BigDecimal("1000"))
                .build();

        cartItem = CartItem.builder()
                .product(product)
                .quantity(2)
                .build();

        customer = CustomerProfile.builder()
                .tier("GOLD")
                .build();
    }

    @Test
    void testCalculateCartDiscounts_withBrandAndCategory() throws Exception {
        when(offersRepository.brandMinOff()).thenReturn(Map.of("PUMA", new BigDecimal("0.40")));
        when(offersRepository.categoryExtraOff()).thenReturn(Map.of("T-shirts", new BigDecimal("0.10")));
        when(offersRepository.bankOffers()).thenReturn(Collections.emptyMap());

        DiscountedPrice result = discountService.calculateCartDiscounts(
                List.of(cartItem), customer, Optional.empty());

        assertEquals(new BigDecimal("2000.00"), result.getOriginalPrice());
        assertTrue(result.getFinalPrice().compareTo(result.getOriginalPrice()) < 0);
        assertTrue(result.getAppliedDiscounts().containsKey("BRAND(PUMA)"));
        assertTrue(result.getAppliedDiscounts().containsKey("CATEGORY(T-shirts)"));
    }

    @Test
    void testCalculateCartDiscounts_withVoucher() throws Exception {
        Voucher voucher = Voucher.builder()
                .code("SUPER10")
                .percentOff(new BigDecimal("0.10"))
                .maxDiscount(new BigDecimal("100"))
                .excludedBrands(Collections.emptySet())
                .includedCategories(Collections.emptySet())
                .minCustomerTier(null)
                .build();

        when(offersRepository.brandMinOff()).thenReturn(Collections.emptyMap());
        when(offersRepository.categoryExtraOff()).thenReturn(Collections.emptyMap());
        when(offersRepository.bankOffers()).thenReturn(Collections.emptyMap());
        when(offersRepository.findVoucher("SUPER10")).thenReturn(Optional.of(voucher));

        PaymentInfo paymentInfo = PaymentInfo.builder()
                .method("VOUCHER:SUPER10")
                .build();

        DiscountedPrice result = discountService.calculateCartDiscounts(
                List.of(cartItem), customer, Optional.of(paymentInfo));

        assertTrue(result.getAppliedDiscounts().containsKey("VOUCHER(SUPER10)"));
    }

    @Test
    void testCalculateCartDiscounts_withInvalidVoucher() {
        when(offersRepository.findVoucher("FAKE")).thenReturn(Optional.empty());

        PaymentInfo paymentInfo = PaymentInfo.builder()
                .method("VOUCHER:FAKE")
                .build();

        assertThrows(DiscountValidationException.class, () ->
                discountService.calculateCartDiscounts(List.of(cartItem), customer, Optional.of(paymentInfo)));
    }

    @Test
    void testCalculateCartDiscounts_withBankOffer() throws Exception {
        when(offersRepository.brandMinOff()).thenReturn(Collections.emptyMap());
        when(offersRepository.categoryExtraOff()).thenReturn(Collections.emptyMap());
        when(offersRepository.bankOffers()).thenReturn(Map.of("ICICI", new BigDecimal("0.10")));

        PaymentInfo paymentInfo = PaymentInfo.builder()
                .bankName("ICICI")
                .build();

        DiscountedPrice result = discountService.calculateCartDiscounts(
                List.of(cartItem), customer, Optional.of(paymentInfo));

        assertTrue(result.getAppliedDiscounts().containsKey("BANK(ICICI)"));
    }

    @Test
    void testValidateDiscountCode_withExcludedBrand() throws Exception {
        Voucher voucher = Voucher.builder()
                .code("EXCLUDEPUMA")
                .percentOff(new BigDecimal("0.20"))
                .excludedBrands(Set.of("PUMA"))
                .build();

        when(offersRepository.findVoucher("EXCLUDEPUMA")).thenReturn(Optional.of(voucher));

        boolean valid = discountService.validateDiscountCode("EXCLUDEPUMA", List.of(cartItem), customer);
        assertFalse(valid);
    }

    @Test
    void testValidateDiscountCode_withIncludedCategoryMismatch() throws Exception {
        Voucher voucher = Voucher.builder()
                .code("ONLYSHOES")
                .percentOff(new BigDecimal("0.20"))
                .includedCategories(Set.of("Shoes"))
                .build();

        when(offersRepository.findVoucher("ONLYSHOES")).thenReturn(Optional.of(voucher));

        boolean valid = discountService.validateDiscountCode("ONLYSHOES", List.of(cartItem), customer);
        assertFalse(valid);
    }

    @Test
    void testValidateDiscountCode_withCustomerTierRequirement() throws Exception {
        Voucher voucher = Voucher.builder()
                .code("ONLYPLATINUM")
                .percentOff(new BigDecimal("0.20"))
                .minCustomerTier("PLATINUM")
                .build();

        when(offersRepository.findVoucher("ONLYPLATINUM")).thenReturn(Optional.of(voucher));

        boolean valid = discountService.validateDiscountCode("ONLYPLATINUM", List.of(cartItem), customer);
        assertFalse(valid); // customer tier is GOLD, requires PLATINUM
    }

    @Test
    void testCalculateCartDiscounts_runtimeException() {
        when(offersRepository.brandMinOff()).thenThrow(new RuntimeException("DB failure"));

        assertThrows(DiscountCalculationException.class, () ->
                discountService.calculateCartDiscounts(List.of(cartItem), customer, Optional.empty()));
    }
}
