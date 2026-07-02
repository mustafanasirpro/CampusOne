package com.campusone.marketplace.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.marketplace.dto.request.CreateMarketplaceListingRequest;
import com.campusone.marketplace.dto.request.MarketplaceImageRequest;
import com.campusone.marketplace.entity.MarketplaceCategory;
import com.campusone.marketplace.entity.MarketplaceItemCondition;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MarketplaceRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void createRequest_validValues_hasNoViolations() {
        Set<ConstraintViolation<CreateMarketplaceListingRequest>> violations =
                validator.validate(validRequest());

        assertThat(violations).isEmpty();
    }

    @Test
    void createRequest_invalidCoreFields_reportsViolations() {
        CreateMarketplaceListingRequest request =
                new CreateMarketplaceListingRequest(
                        "Book",
                        "Too short",
                        null,
                        BigDecimal.ZERO,
                        "pk",
                        null,
                        List.of());

        Set<ConstraintViolation<CreateMarketplaceListingRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains(
                        "title",
                        "description",
                        "category",
                        "price",
                        "condition");
    }

    @Test
    void createRequest_invalidImages_reportsViolations() {
        List<MarketplaceImageRequest> images = java.util.stream.IntStream
                .range(0, 7)
                .mapToObj(index -> new MarketplaceImageRequest(
                        index == 0
                                ? "javascript:alert(1)"
                                : "https://example.com/item-" + index + ".jpg",
                        null))
                .toList();
        CreateMarketplaceListingRequest request =
                new CreateMarketplaceListingRequest(
                        "Used OOP Textbook",
                        "A clean textbook with highlighted examples.",
                        MarketplaceCategory.BOOKS,
                        new BigDecimal("1800.00"),
                        "PKR",
                        MarketplaceItemCondition.USED,
                        images);

        Set<ConstraintViolation<CreateMarketplaceListingRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("images", "images[0].imageUrl");
    }

    private CreateMarketplaceListingRequest validRequest() {
        return new CreateMarketplaceListingRequest(
                "Used OOP Textbook",
                "A clean textbook with highlighted examples.",
                MarketplaceCategory.BOOKS,
                new BigDecimal("1800.00"),
                "pkr",
                MarketplaceItemCondition.USED,
                List.of(new MarketplaceImageRequest(
                        "https://example.com/oop-book.jpg",
                        "OOP textbook cover")));
    }
}
