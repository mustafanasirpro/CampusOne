package com.campusone.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.config.PasswordResetProperties;
import com.campusone.user.entity.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ResendPasswordResetMailerTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PasswordResetProperties properties;
    private ResendPasswordResetMailer mailer;

    @BeforeEach
    void setUp() {
        properties = new PasswordResetProperties();
        properties.setFrontendUrl("https://campusone.dev");
        properties.setResendApiKey("re_secret_key");
        properties.setResendFrom("CampusOne <onboarding@resend.dev>");
        properties.setResendApiUrl(URI.create("https://api.resend.com/emails"));
        properties.setResendTimeout(Duration.ofSeconds(10));
        properties.setTokenTtl(Duration.ofMinutes(30));
        mailer = new ResendPasswordResetMailer(
                properties,
                httpClient,
                objectMapper);
    }

    @Test
    void sendResetLinkBuildsResendHttpRequest() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        User user = new User("student@example.com", "$2a$12$password");
        ReflectionTestUtils.setField(
                user,
                "id",
                UUID.fromString("10000000-0000-4000-8000-000000000001"));

        mailer.sendResetLink(user, "raw-token");

        ArgumentCaptor<HttpRequest> requestCaptor =
                ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(
                requestCaptor.capture(),
                any(HttpResponse.BodyHandler.class));
        HttpRequest request = requestCaptor.getValue();
        assertThat(request.uri())
                .isEqualTo(URI.create("https://api.resend.com/emails"));
        assertThat(request.timeout()).isEqualTo(Optional.of(Duration.ofSeconds(10)));
        assertThat(request.headers().firstValue("Authorization"))
                .contains("Bearer re_secret_key");
        assertThat(request.headers().firstValue("Content-Type"))
                .contains("application/json");

        Map<String, Object> body = objectMapper.readValue(
                requestBody(request),
                new TypeReference<>() {
                });
        assertThat(body.get("from"))
                .isEqualTo("CampusOne <onboarding@resend.dev>");
        assertThat(body.get("to")).isEqualTo(List.of("student@example.com"));
        assertThat(body.get("subject")).isEqualTo("CampusOne password reset");
        assertThat((String) body.get("html"))
                .contains("https://campusone.dev/reset-password?token=raw-token")
                .contains("CampusOne password reset");
        assertThat((String) body.get("text"))
                .contains("https://campusone.dev/reset-password?token=raw-token")
                .contains("This link expires in 30 minutes");
    }

    @Test
    void sendResetLinkMissingApiKeyFailsSafelyWithoutHttpRequest()
            throws Exception {
        properties.setResendApiKey("");
        User user = new User("student@example.com", "$2a$12$password");

        assertThatThrownBy(() -> mailer.sendResetLink(user, "raw-token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RESEND_API_KEY is missing");
        verify(httpClient, never()).send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class));
    }

    @Test
    void sendResetLinkHttpFailureFailsSafely() throws Exception {
        when(httpResponse.statusCode()).thenReturn(403);
        when(httpResponse.body()).thenReturn("{\"message\":\"bad api key\"}");
        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        User user = new User("student@example.com", "$2a$12$password");

        assertThatThrownBy(() -> mailer.sendResetLink(user, "raw-token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("status 403");
    }

    @Test
    void sendTestEmailUsesResendProvider() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        mailer.sendTestEmail("admin@example.com");

        ArgumentCaptor<HttpRequest> requestCaptor =
                ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(
                requestCaptor.capture(),
                any(HttpResponse.BodyHandler.class));
        Map<String, Object> body = objectMapper.readValue(
                requestBody(requestCaptor.getValue()),
                new TypeReference<>() {
                });
        assertThat(body.get("to")).isEqualTo(List.of("admin@example.com"));
        assertThat(body.get("subject")).isEqualTo("CampusOne test email");
    }

    private String requestBody(HttpRequest request) throws Exception {
        HttpRequest.BodyPublisher publisher = request.bodyPublisher()
                .orElseThrow();
        StringBuilder body = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                byte[] bytes = new byte[item.remaining()];
                item.get(bytes);
                body.append(new String(bytes, StandardCharsets.UTF_8));
            }

            @Override
            public void onError(Throwable throwable) {
                error.set(throwable);
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });
        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        if (error.get() != null) {
            throw new IllegalStateException(error.get());
        }
        return body.toString();
    }
}
