# Discount Service
**Discount Service for Unifize Interview**
A Spring Boot service to calculate discounts on products based on brand, customer profile, vouchers, and offers.

## Repostory
For now we have used InMemoryOffersRepository to 
hardcode the discount scenarios, to add more scenarios
just add into this class or can also store in the postgres
database and can fetch from there at run time or can be
also stored in cache depending upon the nature of
the data and changing business requirements.

## Project Structure
fashion-discount-service/
├─ pom.xml
├─ README.md
├─ COMMIT_LOG.md
└─ src
   ├─ main
   │  └─ java/com/example/discount/
   │     ├─ BrandTier.java
   │     ├─ Product.java
   │     ├─ CartItem.java
   │     ├─ PaymentInfo.java
   │     ├─ DiscountedPrice.java
   │     ├─ CustomerProfile.java
   │     ├─ DiscountService.java
   │     ├─ DiscountCalculationException.java
   │     ├─ DiscountValidationException.java
   │     ├─ Voucher.java
   │     ├─ OffersRepository.java
   │     ├─ InMemoryOffersRepository.java
   │     ├─ Money.java
   │     ├─ DefaultDiscountService.java
   │     └─ TestDataConfig.java
   └─ test
      └─ java/com/example/discount/
         ├─ TestData.java
         ├─ DefaultDiscountServiceTest.java
         └─ IntegrationFlowTest.java

## Prerequisites
- Java 17+
- Maven 3.6+
- Git
- cURL or Postman (for testing API)

## Setup & Run
1. Clone the repository:
   git clone https://github.com/ayush-ak-725/discount-service.git
   cd discount-service/discountService
2. Checkout feature branch:
   git checkout ai-commit
3. Install dependencies:
   mvn clean install
4. Run the service:
   mvn spring-boot:run
   Service will start on http://localhost:8080

## API Endpoint
POST /api/discounts/calculate

Success Case:
Request Body:
{
  "cartItems": [
    {
      "product": {
        "id": "PUMA-TS-2",
        "name": "Puma Tee",
        "brand": "PUMA",
        "category": "T-shirts",
        "basePrice": 1000.0
      },
      "quantity": 1,
      "size": "M"
    }
  ],
  "customer": {
    "id": "CUST005",
    "tier": "GOLD"
  },
  "paymentInfo": {
    "method": "CARD",
    "bankName": "ICICI",
    "cardType": "DEBIT"
  }
}
Response Body:
{
    "originalPrice": 1000.00,
    "finalPrice": 486.00,
    "appliedDiscounts": {
        "BRAND(PUMA)": 400.00,
        "CATEGORY(T-shirts)": 60.00,
        "BANK(ICICI)": 54.00
    },
    "message": "Discounts applied: [BRAND(PUMA), CATEGORY(T-shirts), BANK(ICICI)]"
}

Failure Case:
Request Body:
{
  "cartItems": [
    {
      "product": {
        "id": "PUMA-TS-1",
        "name": "Puma Tee",
        "brand": "PUMA",
        "category": "T-shirts",
        "basePrice": 1000.0
      },
      "quantity": 1,
      "size": "M"
    }
  ],
  "customer": {
    "id": "CUST001",
    "tier": "GOLD"
  },
  "paymentInfo": {
    "method": "VOUCHER:SUPER10"
  }
}

Response Body:
{
    "originalPrice": null,
    "finalPrice": null,
    "appliedDiscounts": null,
    "message": "Error calculating discounts: Unknown voucher: SUPER10"
}

## Refer to the Json the File for postman curl RR under
- discountService/src/main/resources/discount-service.postman_collection.json

Note: Voucher discounts are exclusive and replace other discounts.

## Assumptions
- Voucher discount is applied exclusively instead of combining with other discounts.
- All amounts are in the default currency (assumed INR).
- Service currently uses an in-memory offers repository (InMemoryOffersRepository).

## Running Tests
mvn test
Covers unit tests for DefaultDiscountService and integration flow tests.
- To run Integration test separately: mvn -Dtest=DiscountServiceImplTest test
- To run Unit tests separately: mvn -Dtest=DiscountServiceUnitTest test

## Contact
For questions or clarifications, contact: [Ayush Kumar Agrawal / ayush.ak3107@gmail.com / +91-7905244320]
