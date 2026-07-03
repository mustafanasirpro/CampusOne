package com.campusone.search.controller;

import com.campusone.search.dto.SearchSort;
import com.campusone.search.dto.SearchType;
import com.campusone.search.dto.response.GlobalSearchResponse;
import com.campusone.search.dto.response.SearchSuggestionResponse;
import com.campusone.search.dto.response.SearchTypeResponse;
import com.campusone.search.service.GlobalSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@Validated
@Tag(name = "Global Search")
public class SearchController {

    private static final String VALID_QUERY =
            "^\\s*\\S(?:.*\\S)\\s*$";

    private final GlobalSearchService searchService;

    public SearchController(GlobalSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @Operation(summary = "Search public CampusOne content")
    public ResponseEntity<GlobalSearchResponse> search(
            @RequestParam("q")
            @NotBlank
            @Size(min = 2, max = 100)
            @Pattern(
                    regexp = VALID_QUERY,
                    message = "must contain at least 2 characters after trimming")
            String query,
            @RequestParam(required = false)
            @Parameter(
                    description = "Comma-separated search types, for example NOTE,EVENT")
            Set<SearchType> types,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "RELEVANCE") SearchSort sort) {
        return ResponseEntity.ok(searchService.search(
                query,
                types,
                page,
                size,
                sort));
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get public search suggestions")
    public ResponseEntity<SearchSuggestionResponse> suggestions(
            @RequestParam("q")
            @NotBlank
            @Size(min = 2, max = 100)
            @Pattern(
                    regexp = VALID_QUERY,
                    message = "must contain at least 2 characters after trimming")
            String query,
            @RequestParam(defaultValue = "5") @Min(1) @Max(10)
            int limit) {
        return ResponseEntity.ok(searchService.suggestions(
                query,
                limit));
    }

    @GetMapping("/types")
    @Operation(summary = "List supported global search types")
    public ResponseEntity<List<SearchTypeResponse>> types() {
        return ResponseEntity.ok(searchService.types());
    }
}
