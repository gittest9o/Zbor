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
        ReflectionTestUtils.setField(filter, "maxAgeSeconds", 3600L);
    }

    // ── Вспомогательные методы ───────────────────────────────────────────────

    /**
     * Строит валидный initData по официальному алгоритму Telegram, с произвольным auth_date.
     * https://core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app
     */
    private String buildValidInitData(String userJson, long authDate) throws Exception {
        String encodedUser = URLEncoder.encode(userJson, StandardCharsets.UTF_8);
        String rawParams = "auth_date=" + authDate + "&user=" + encodedUser;

        // Собираем data_check_string (параметры в алфавитном порядке, без hash)
        String dataCheckString = "auth_date=" + authDate + "\nuser=" + userJson;

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

    // Старый фиксированный auth_date (2023 год) — используется в тестах,
    // которые проверяют только подпись/парсинг и не должны зависеть от текущего времени.
    private static final long LEGACY_AUTH_DATE = 1700000000L;

    private String buildValidInitData(String userJson) throws Exception {
        return buildValidInitData(userJson, LEGACY_AUTH_DATE);
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

    // ── isFresh() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isFresh()")
    class IsFreshTests {

        @Test
        @DisplayName("Возвращает true для свежего auth_date (только что подписан)")
        void isFresh_returnsTrue_forRecentAuthDate() throws Exception {
            long now = java.time.Instant.now().getEpochSecond();
            String initData = buildValidInitData("{\"id\":123}", now);

            assertThat(filter.isFresh(initData)).isTrue();
        }

        @Test
        @DisplayName("Возвращает false, если auth_date старше maxAgeSeconds (защита от replay)")
        void isFresh_returnsFalse_forExpiredAuthDate() throws Exception {
            long now = java.time.Instant.now().getEpochSecond();
            long expired = now - 7200; // 2 часа назад, лимит по умолчанию 1 час
            String initData = buildValidInitData("{\"id\":123}", expired);

            assertThat(filter.isFresh(initData)).isFalse();
        }

        @Test
        @DisplayName("Возвращает false, если auth_date находится далеко в будущем")
        void isFresh_returnsFalse_forFutureAuthDate() throws Exception {
            long now = java.time.Instant.now().getEpochSecond();
            String initData = buildValidInitData("{\"id\":123}", now + 600);

            assertThat(filter.isFresh(initData)).isFalse();
        }

        @Test
        @DisplayName("Возвращает false, если auth_date отсутствует")
        void isFresh_returnsFalse_whenAuthDateMissing() {
            String initData = "user=%7B%22id%22%3A123%7D&hash=abc";

            assertThat(filter.isFresh(initData)).isFalse();
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

        @Test
        @DisplayName("Корректно парсит photo_url аватарки")
        void parse_returnsPhotoUrl() throws Exception {
            String userJson = "{\"id\":123,\"first_name\":\"Иван\",\"photo_url\":" +
                    "\"https://t.me/i/userpic/320/example.jpg\"}";
            String initData = buildValidInitData(userJson);

            TelegramUserData result = filter.parseTelegramUserData(initData);

            assertThat(result.getPhotoUrl()).isEqualTo("https://t.me/i/userpic/320/example.jpg");
        }

        @Test
        @DisplayName("photoUrl равен null, если Telegram не передал photo_url")
        void parse_photoUrlIsNull_whenAbsent() throws Exception {
            String initData = buildValidInitDataForUser(123L, "Иван", "Иванов", "ivan");

            TelegramUserData result = filter.parseTelegramUserData(initData);

            assertThat(result.getPhotoUrl()).isNull();
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