package com.zbor.security;

import com.zbor.dto.request.TelegramUserData;
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


    public TelegramUserData parseTelegramUserData(String initData) {
        try {
            Map<String, String> params = parseQuery(initData);
            String userJson = params.get("user");
            if (userJson == null) {
                throw new com.zbor.exceptions.ZborException("The user field is missing from initData");
            }

            Long   id        = extractLong(userJson,   "\"id\":");
            String firstName = extractString(userJson, "\"first_name\":");
            String lastName  = extractString(userJson, "\"last_name\":");
            String username  = extractString(userJson, "\"username\":");

            return new TelegramUserData(id, firstName, lastName, username);
        } catch (com.zbor.exceptions.ZborException e) {
            throw e;
        } catch (Exception e) {
            throw new com.zbor.exceptions.ZborException("Failed to parse Telegram data: " + e.getMessage());
        }
    }

    public Optional<Long> extractTelegramId(String initData) {
        try {
            Map<String, String> params = parseQuery(initData);
            String userJson = params.get("user");
            if (userJson == null) return Optional.empty();

            int idx = userJson.indexOf("\"id\":");
            if (idx < 0) return Optional.empty();

            String digits = userJson.substring(idx + 5).replaceAll("[^0-9].*", "");
            return Optional.of(Long.parseLong(digits));
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

    private Long extractLong(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        String digits = json.substring(idx + key.length()).replaceAll("[^0-9].*", "");
        return digits.isEmpty() ? null : Long.parseLong(digits);
    }

    private String extractString(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        String rest = json.substring(idx + key.length()).stripLeading();
        if (!rest.startsWith("\"")) return null;
        rest = rest.substring(1);
        int end = rest.indexOf("\"");
        return end < 0 ? null : rest.substring(0, end);
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
