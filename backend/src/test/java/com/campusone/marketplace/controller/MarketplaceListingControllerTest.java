package com.campusone.marketplace.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusone.marketplace.dto.request.CreateMarketplaceListingRequest;
import com.campusone.marketplace.dto.response.MarketplaceListingDetailResponse;
import com.campusone.marketplace.dto.response.MarketplaceListingPageResponse;
import com.campusone.marketplace.dto.response.MarketplaceSellerResponse;
import com.campusone.marketplace.entity.MarketplaceCategory;
import com.campusone.marketplace.entity.MarketplaceItemCondition;
import com.campusone.marketplace.entity.MarketplaceListingStatus;
import com.campusone.marketplace.service.MarketplaceListingService;
import com.campusone.security.CampusOneUserDetailsService;
import com.campusone.security.CampusOneUserPrincipal;
import com.campusone.security.JwtAuthenticationFilter;
import com.campusone.security.JwtService;
import com.campusone.security.RestAccessDeniedHandler;
import com.campusone.security.RestAuthenticationEntryPoint;
import com.campusone.security.SecurityConfig;
import com.campusone.security.SecurityErrorResponseWriter;
import com.campusone.user.entity.AccountStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MarketplaceListingController.class)
@ActiveProfiles("test")
@Import({
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    RestAuthenticationEntryPoint.class,
    RestAccessDeniedHandler.class,
    SecurityErrorResponseWriter.class
})
class MarketplaceListingControllerTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-4000-8000-000000000001");
    private static final UUID LISTING_ID = UUID.fromString(
            "20000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-07-02T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MarketplaceListingService listingService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CampusOneUserDetailsService userDetailsService;

    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        CampusOneUserPrincipal principal = new CampusOneUserPrincipal(
                USER_ID,
                "student@example.com",
                "$2a$12$encoded-password",
                AccountStatus.ACTIVE,
                Set.of("STUDENT"));
        authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities());
    }

    @Test
    void listActiveListings_withoutAuthentication_isUnauthorized()
            throws Exception {
        mockMvc.perform(get("/api/v1/marketplace/listings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listActiveListings_withAuthentication_returnsPage()
            throws Exception {
        when(listingService.listActiveListings(
                null,
                null,
                0,
                20))
                .thenReturn(new MarketplaceListingPageResponse(
                        List.of(),
                        0,
                        20,
                        0,
                        0,
                        true,
                        true));

        mockMvc.perform(get("/api/v1/marketplace/listings")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void createListing_withAuthentication_returnsCreated()
            throws Exception {
        when(listingService.createListing(
                eq(USER_ID),
                any(CreateMarketplaceListingRequest.class)))
                .thenReturn(detailResponse());

        mockMvc.perform(post("/api/v1/marketplace/listings")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/marketplace/listings/" + LISTING_ID))
                .andExpect(jsonPath("$.id").value(LISTING_ID.toString()));
    }

    @Test
    void createListing_invalidRequest_returnsValidationError()
            throws Exception {
        mockMvc.perform(post("/api/v1/marketplace/listings")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Bad",
                                  "description": "short",
                                  "category": "BOOKS",
                                  "price": 0,
                                  "currency": "PKR",
                                  "condition": "USED",
                                  "images": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private MarketplaceListingDetailResponse detailResponse() {
        return new MarketplaceListingDetailResponse(
                LISTING_ID,
                "Java Programming Textbook",
                "A clean textbook suitable for first-year programming courses.",
                MarketplaceCategory.BOOKS,
                new BigDecimal("1800.00"),
                "PKR",
                MarketplaceItemCondition.USED,
                MarketplaceListingStatus.ACTIVE,
                List.of(),
                new MarketplaceSellerResponse(
                        USER_ID,
                        "Ali Khan",
                        null,
                        "COMSATS University Islamabad"),
                NOW,
                NOW);
    }

    private String validCreateJson() {
        return """
                {
                  "title": "Java Programming Textbook",
                  "description": "A clean textbook suitable for first-year programming courses.",
                  "category": "BOOKS",
                  "price": 1800.00,
                  "currency": "PKR",
                  "condition": "USED",
                  "images": [
                    {
                      "imageUrl": "https://example.com/java-book.jpg",
                      "altText": "Java textbook cover"
                    }
                  ]
                }
                """;
    }
}
