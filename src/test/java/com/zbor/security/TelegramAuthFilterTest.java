package com.zbor.security;

import com.zbor.dto.request.TelegramUserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class TelegramAuthFilterTest {

    private TelegramAuthFilter filter;

    private static final String BOT_TOKEN = "test_bot_token_12345";

    @BeforeEach
    void setUp() {
        filter = new TelegramAuthFilter();
        ReflectionTestUtils.setField(filter, "botToken", BOT_TOKEN);
    }

    // ── Вспомогательные методы ───────────────────────────────────────────────

    /**
     * Строит валидный initData по официальному алгоритму Telegram.
     * https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app
     */
    private String buildValidInitData(String userJson) throws Exception {
        String encodedUser = URLEncoder.encode(userJson, StandardCharsets.UTF_8);
        String rawParams = "auth_date=1700000000&user=" + encodedUser;

        // Собираем data_check_string (параметры в алфавитном порядке, без hash)
        String dataCheckString = "auth_date=1700000000\nuser=" + userJson;

        // secret_key = HMAC("WebAppData", bot_token)
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                "WebAppData".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] secretKey = mac.doFinal(BOT_TOKEN.getBytes(StandardCharsets.UTF_8));

        // hash = HMAC(secret_key, data_check_string)
        mac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
        byte[] hashBytes = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
        String hash = HexFormat.of().formatHex(hashBytes);

        return rawParams + "&hash=" + hash;
    }

    private String buildValidInitDataForUser(long id, String firstName, String lastName, String username)
            throws Exception {
        String userJson = String.format(
                "{\"id\":%d,\"first_name\":\"%s\",\"last_name\":\"%s\",\"username\":\"%s\"}",
                id, firstName, lastName, username);
        return buildValidInitData(userJson);
    }

    // ── isValid() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isValid()")
    class IsValidTests {

        @Test
        @DisplayName("Возвращает true для корректно подписанного initData")
        void isValid_returnsTrue_forCorrectSignature() throws Exception {
            String initData = buildValidInitDataForUser(123L, "Иван", "Иванов", "ivan");

            assertThat(filter.isValid(initData)).isTrue();
        }

        @Test
        @DisplayName("Возвращает false, если подпись изменена")
        void isValid_returnsFalse_forTamperedHash() throws Exception {
            String initData = buildValidInitDataForUser(123L, "Иван", "Иванов", "ivan");
            String tampered = initData.replaceFirst("hash=[a-f0-9]+", "hash=0000000000000000");

            assertThat(filter.isValid(tampered)).isFalse();
        }

        @Test
        @DisplayName("Возвращает false, если поле hash отсутствует")
        void isValid_returnsFalse_whenHashMissing() {
            String initData = "auth_date=1700000000&user=%7B%22id%22%3A123%7D";

            assertThat(filter.isValid(initData)).isFalse();
        }

        @Test
        @DisplayName("Возвращает false для пустой строки")
        void isValid_returnsFalse_forEmptyString() {
            assertThat(filter.isValid("")).isFalse();
        }

        @Test
        @DisplayName("Возвращает false, если данные изменены после подписи")
        void isValid_returnsFalse_ifDataTamperedAfterSigning() throws Exception {
            String initData = buildValidInitDataForUser(123L, "Иван", "Иванов", "ivan");
            // Меняем auth_date — подпись уже не будет совпадать
            String tampered = initData.replace("auth_date=1700000000", "auth_date=9999999999");

            assertThat(filter.isValid(tampered)).isFalse();
        }

        @Test
        @DisplayName("Возвращает false при неверном боттокене (другой фильтр)")
        void isValid_returnsFalse_forDifferentBotToken() throws Exception {
            // Создаём фильтр с другим токеном
            TelegramAuthFilter otherFilter = new TelegramAuthFilter();
            ReflectionTestUtils.setField(otherFilter, "botToken", "other_token");

            // initData подписан нашим BOT_TOKEN — для другого токена должен быть false
            String initData = buildValidInitDataForUser(123L, "Иван", "Иванов", "ivan");

            assertThat(otherFilter.isValid(initData)).isFalse();
        }
    }

    // ── parseTelegramUserData() ───────────────────────────────────────────────

    @Nested
    @DisplayName("parseTelegramUserData()")
    class ParseTelegramUserDataTests {

        @Test
        @DisplayName("Корректно парсит все поля пользователя")
        void parse_returnsCorrectData() throws Exception {
            String initData = buildValidInitDataForUser(456789L, "Мария", "Иванова", "maria_art");

            TelegramUserData result = filter.parseTelegramUserData(initData);

            assertThat(result.getId()).isEqualTo(456789L);
            assertThat(result.getFirstName()).isEqualTo("Мария");
            assertThat(result.getLastName()).isEqualTo("Иванова");
            assertThat(result.getUsername()).isEqualTo("maria_art");
        }

        @Test
        @DisplayName("Корректно парсит id как long (большие числа Telegram)")
        void parse_handlesLargeTelegramId() throws Exception {
            long bigId = 9_876_543_210L;
            String initData = buildValidInitDataForUser(bigId, "Алексей", "Петров", "alex");

            TelegramUserData result = filter.parseTelegramUserData(initData);

            assertThat(result.getId()).isEqualTo(bigId);
        }
    }

    // ── extractTelegramId() ──────────────────────────────────────────────────

    @Nested
    @DisplayName("extractTelegramId()")
    class ExtractTelegramIdTests {

        @Test
        @DisplayName("Возвращает telegramId из валидного initData")
        void extract_returnsTelegramId() throws Exception {
            String initData = buildValidInitDataForUser(123456L, "Иван", "Иванов", "ivan");

            Optional<Long> result = filter.extractTelegramId(initData);

            assertThat(result).isPresent().hasValue(123456L);
        }

        @Test
        @DisplayName("Возвращает empty, если поле user отсутствует")
        void extract_returnsEmpty_whenUserFieldMissing() {
            String initData = "auth_date=1700000000&hash=abc";

            Optional<Long> result = filter.extractTelegramId(initData);

            assertThat(result).isEmpty();
        }
    }
}
