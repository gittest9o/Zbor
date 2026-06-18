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

/**
 * Фильтр для всех защищённых запросов.
 *
 * Что делает:
 * 1. Берёт заголовок X-Telegram-Init-Data
 * 2. Проверяет HMAC-SHA256 подпись (официальный алгоритм Telegram)
 * 3. Если подпись валидна — достаёт telegramId и кладёт в request.setAttribute("telegramId")
 * 4. Если невалидна — возвращает 401
 *
 * Контроллеры читают:  Long id = (Long) request.getAttribute("telegramId");
 *
 * ВАЖНО: /auth/** исключён из фильтра через shouldNotFilter —
 * AuthController сам вызывает isValid() и parseTelegramUserData().
 */
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

    /**
     * Пути, которые фильтр пропускает без проверки.
     * /auth/** — проверяется вручную внутри AuthController.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/")
                || path.equals("/auth")
                || path.equals("/")
                || path.startsWith("/actuator");
    }

    // ── Публичные методы (используются в AuthController) ────────────────────

    /**
     * Валидация по официальному алгоритму Telegram:
     * https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app
     *
     * 1. Убираем hash из параметров
     * 2. Сортируем остальные параметры по ключу, собираем строку "key=value\n..."
     * 3. secret_key = HMAC_SHA256("WebAppData", bot_token)
     * 4. Считаем HMAC_SHA256(secret_key, data_check_string)
     * 5. Сравниваем с hash
     */
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
            System.out.println("=== isValid called ===");
            System.out.println(hash);
            System.out.println(dataCheckString);
            System.out.println(HexFormat.of().formatHex(computed));
            System.out.println(HexFormat.of().formatHex(computed).equals(hash));
            return HexFormat.of().formatHex(computed).equals(hash);

        } catch (Exception e) {
            log.error("Ошибка валидации initData: {}", e.getMessage());
            return false;
        }

    }

    /**
     * Парсит все данные пользователя из initData.
     * Вызывается из AuthController после успешного isValid().
     *
     * Пример поля user в initData (после URL-decode):
     * {"id":123456,"first_name":"Ivan","last_name":"Ivanov","username":"ivan"}
     */
    public TelegramUserData parseTelegramUserData(String initData) {
        try {
            Map<String, String> params = parseQuery(initData);
            String userJson = params.get("user");
            if (userJson == null) {
                throw new com.zbor.exceptions.ZborException("Поле user отсутствует в initData");
            }

            Long   id        = extractLong(userJson,   "\"id\":");
            String firstName = extractString(userJson, "\"first_name\":");
            String lastName  = extractString(userJson, "\"last_name\":");
            String username  = extractString(userJson, "\"username\":");

            return new TelegramUserData(id, firstName, lastName, username);
        } catch (com.zbor.exceptions.ZborException e) {
            throw e;
        } catch (Exception e) {
            throw new com.zbor.exceptions.ZborException("Не удалось распарсить данные Telegram: " + e.getMessage());
        }
    }

    /**
     * Извлекает только telegramId — используется в doFilterInternal.
     */
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
            log.error("Не удалось извлечь telegramId: {}", e.getMessage());
            return Optional.empty();
        }
    }

    // ── Вспомогательные методы ───────────────────────────────────────────────

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
