package com.unifize.discountService.testconfig;

import com.unifize.discountService.models.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Configuration
public class TestDataConfig {

    @Bean
    public TestData testData() {
        return new TestData(getScenarios());
    }

    private List<TestScenario> getScenarios() {
        return Arrays.asList(

                // 1. Multiple Discount Scenario (Brand + Category + Bank)
                TestScenario.builder()
                        .name("PUMA T-shirt - Min 40% + Category 10% + ICICI 10%")
                        .cartItem(cartItem("PUMA-TS-1", "PUMA Tee", "PUMA", "T-shirts", 1000.0, 1))
                        .customer(customer("C1", "BRONZE", 20))
                        .paymentInfo(payment("ICICI"))
                        .expectedDiscounts(3)
                        .build(),

                // 2. PUMA T-shirt - Only Brand + Category (No bank offer)
                TestScenario.builder()
                        .name("PUMA T-shirt - Min 40% + Category 10% only")
                        .cartItem(cartItem("PUMA-TS-2", "PUMA Tee", "PUMA", "T-shirts", 1200.0, 1))
                        .customer(customer("C2", "SILVER", 10))
                        .paymentInfo(payment("HDFC")) // No ICICI offer
                        .expectedDiscounts(2)
                        .build(),

                // 3. PUMA Shoes - Only Brand Discount
                TestScenario.builder()
                        .name("PUMA Shoes - Min 40% only")
                        .cartItem(cartItem("PUMA-SH-1", "Running Shoes", "PUMA", "Shoes", 5000.0, 1))
                        .customer(customer("C3", "GOLD", 100))
                        .paymentInfo(payment("AXIS"))
                        .expectedDiscounts(1)
                        .build(),

                // 4. Adidas Jeans - No Discount (since Adidas/Jeans not in repo)
                TestScenario.builder()
                        .name("ADIDAS Jeans - No offers")
                        .cartItem(cartItem("AD-01", "Jeans", "ADIDAS", "Jeans", 2000.0, 1))
                        .customer(customer("C4", "SILVER", 10))
                        .paymentInfo(payment("HDFC"))
                        .expectedDiscounts(0)
                        .build(),

                // 5. Generic Belt - No Discount
                TestScenario.builder()
                        .name("Generic Belt - No offers")
                        .cartItem(cartItem("GB-01", "Belt", "GENERIC", "Accessories", 500.0, 1))
                        .customer(customer("C5", "BRONZE", 0))
                        .paymentInfo(payment("PNB"))
                        .expectedDiscounts(0)
                        .build(),

                // 6. Voucher Scenario (SUPER69, applies universally)
                TestScenario.builder()
                        .name("Voucher SUPER69 - 69% off")
                        .cartItem(cartItem("ANY-01", "Some Item", "PUMA", "T-shirts", 2000.0, 1))
                        .customer(customer("C6", "PLATINUM", 0))
                        .paymentInfo(payment("ICICI")) // bank + voucher + brand + category
                        .expectedDiscounts(3)
                        .build()
        );
    }

    private static CartItem cartItem(String id, String name, String brand, String category, double price, int qty) {
        return CartItem.builder()
                .product(Product.builder()
                        .id(id)
                        .brand(brand)
                        .category(category)
                        .basePrice(BigDecimal.valueOf(price))
                        .build())
                .quantity(qty)
                .build();
    }

    private static CustomerProfile customer(String id, String tier, int points) {
        return CustomerProfile.builder()
                .id(id).
                tier(tier)
                .build();
    }

    private static PaymentInfo payment(String bankName) {
        return PaymentInfo.builder()
                .method("CARD")
                .bankName(bankName)
                .cardType("DEBIT")
                .build();
    }
}
