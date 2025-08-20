# discount-service
Discount Service for Unifize Interview

Initially the project structure given by the AI was as below:

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

Steps to run the service:

Step 1: Clone the service to your local.
Step 2: cd discount-service
Step 3: cd discountService
Step 4: run "mvn clean install" to install required dependencies.
Step 5: run: "mvn spring-boot:run" , to run the service
and then execute the below curl either from postman or terminal.

curl to hit the api:
curl --location 'http://localhost:8080/api/discounts/calculate' \
--header 'Content-Type: application/json' \
--data '{
  "cartItems": [
    {
      "id": "PUMA-TS-1",
      "name": "Puma Tee",
      "brand": "PUMA",
      "category": "T-shirts",
      "price": 1000.0,
      "quantity": 1
    }
  ],
  "paymentInfo": {
    "method": "CARD",
    "bank": "ICICI",
    "cardType": "DEBIT"
  },
  "voucher": "SUPER69"
}'

Expected Response:

{
  "finalPrice": 310.0,
  "totalDiscount": 690.0,
  "appliedDiscounts": [
    "Voucher SUPER69 - 69% off"
  ]
}

Assumption: We are treating voucher discount as exclusive(applied instead of other discounts).

