package com.zbor.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zbor.dto.request.TelegramUserData;
import com.zbor.exceptions.ZborException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.List;

@Component
@Slf4j
public class TelegramAuthFilter extends OncePerRequestFilter {

    @Value("${telegram.bot.token}")
    private String botToken;

    // Максимальный возраст initData в секундах (защита от replay-атак).
    // Telegram не обновляет initData пока Mini App открыт, поэтому лимит должен покрывать
    // реалистичную длительность одной сессии пользователя, а не только момент открытия.
    @Value("${telegram.auth.max-age-seconds:86400}")
    private long maxAgeSeconds;

    // Допуск на расхождение часов сервера/клиента.
    private static final long CLOCK_SKEW_TOLERANCE_SECONDS = 60;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String initData = request.getHeader("X-Telegram-Init-Data");

        if (initData == null || initData.isBlank()) {
            sendUnauthorized(response, "Header missing X-Telegram-Init-Data");
            return;
        }

        if (!isValid(initData)) {
            sendUnauthorized(response, "invalid initData");
            return;
        }

        if (!isFresh(initData)) {
            sendUnauthorized(response, "initData expired");
            return;
        }

        extractTelegramId(initData).ifPresentOrElse(
                id -> {
                    request.setAttribute("telegramId", id);

                    var auth = new UsernamePasswordAuthenticationToken(
                            id, null, List.of()
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                },
                () -> log.warn("telegramId not found in initData")
        );

        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/")
                || path.equals("/auth")
                || path.equals("/")
                || path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/api-docs")
                || path.startsWith("/webjars/")
                || path.startsWith("/swagger-resources/");
    }

    public boolean isValid(String initData) {
        try {
            Map<String, String> params = parseQuery(initData);
            String hash = params.remove("hash");
            if (hash == null) return false;

            String dataCheckString = params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("\n"));

            byte[] secretKey = hmac("WebAppData".getBytes(StandardCharsets.UTF_8),
                    botToken.getBytes(StandardCharsets.UTF_8));
            byte[] computed  = hmac(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(computed).equals(hash);

        } catch (Exception e) {
            log.error("initData validation error: {}", e.getMessage());
            return false;
        }

    }

    /**
     * Проверяет, что initData не "протух" — то есть auth_date не старше maxAgeSeconds
     * и не находится в будущем (с учётом небольшого допуска на расхождение часов).
     * Без этой проверки один раз перехваченный initData можно использовать бесконечно долго (replay-атака),
     * поскольку HMAC-подпись сама по себе никак не ограничена по времени.
     */
    public boolean isFresh(String initData) {
        try {
            Map<String, String> params = parseQuery(initData);
            String authDateStr = params.get("auth_date");
            if (authDateStr == null) {
                log.warn("initData missing auth_date");
                return false;
            }

            long authDate = Long.parseLong(authDateStr);
            long now = Instant.now().getEpochSecond();

            if (authDate > now + CLOCK_SKEW_TOLERANCE_SECONDS) {
                log.warn("initData auth_date is in the future");
                return false;
            }
            if (now - authDate > maxAgeSeconds) {
                log.warn("initData is expired: age={}s, maxAge={}s", now - authDate, maxAgeSeconds);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("initData freshness check error: {}", e.getMessage());
            return false;
        }
    }


    public TelegramUserData parseTelegramUserData(String initData) {
        try {
            Map<String, String> params = parseQuery(initData);
            String userJson = params.get("user");
            if (userJson == null) {
                throw new ZborException("The user field is missing from initData");
            }

            return objectMapper.readValue(userJson, TelegramUserData.class);
        } catch (ZborException e) {
            throw e;
        } catch (Exception e) {
            throw new ZborException("Failed to parse Telegram data: " + e.getMessage());
        }
    }

    public Optional<Long> extractTelegramId(String initData) {
        try {
            Map<String, String> params = parseQuery(initData);
            String userJson = params.get("user");
            if (userJson == null) return Optional.empty();

            TelegramUserData data = objectMapper.readValue(userJson, TelegramUserData.class);
            return Optional.ofNullable(data.getId());
        } catch (Exception e) {
            log.error("Failed to restore telegramId: {}", e.getMessage());
            return Optional.empty();
        }
    }


    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String pair : query.split("&")) {
            int eq = pair.indexOf("=");
            if (eq > 0) {
                String key   = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                result.put(key, value);
            }
        }
        return result;
    }

    private byte[] hmac(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}