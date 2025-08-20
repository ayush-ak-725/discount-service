package com.unifize.discountService;

import com.unifize.discountService.models.TestData;
import com.unifize.discountService.models.dto.response.DiscountedPrice;
import com.unifize.discountService.services.DiscountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class DiscountServiceImplTest {

    @Autowired
    private TestData testData;

    @Autowired
    private DiscountService discountService;

    @Test
    void testAllScenarios() {
        testData.getScenarios().forEach(scenario -> {
            DiscountedPrice result = discountService.calculateCartDiscounts(
                    List.of(scenario.getCartItem()),
                    scenario.getCustomer(),
                    java.util.Optional.ofNullable(scenario.getPaymentInfo())
            );
            assertThat(result.getAppliedDiscounts().size())
                    .as("Scenario: " + scenario.getName())
                    .isEqualTo(scenario.getExpectedDiscounts());
        });
    }
}
