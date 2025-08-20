package com.unifize.discountService.models;

import com.unifize.discountService.models.CartItem;
import com.unifize.discountService.models.CustomerProfile;
import com.unifize.discountService.models.PaymentInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
@AllArgsConstructor
public class TestData {
    private List<TestScenario> scenarios;
}
