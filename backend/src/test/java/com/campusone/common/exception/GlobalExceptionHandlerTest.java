package com.campusone.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.common.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    @Test
    void noResourceFound_isReportedAsNotFound() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        HttpMethod.GET.name(),
                        "/missing-resource");

        var response = handler.handleNoResourceFound(
                new NoResourceFoundException(
                        HttpMethod.GET,
                        "/missing-resource"),
                request);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("RESOURCE_NOT_FOUND");
    }
}
