package com.zbor.controller;

import com.zbor.data.entity.User;
import com.zbor.dto.request.CreateUserRequest;
import com.zbor.dto.request.TelegramUserData;
import com.zbor.dto.response.AuthResponse;
import com.zbor.exceptions.UserNotFoundException;
import com.zbor.security.TelegramAuthFilter;
import com.zbor.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Первый эндпоинт который вызывает фронт при открытии Mini App.
 *
 * Флоу:
 *   1. Фронт при старте вызывает POST /auth с заголовком X-Telegram-Init-Data
 *   2. Бэк валидирует подпись, ищет юзера в БД
 *      - Юзер есть  → 200 { needsRegistration: false, user: {...} }
 *      - Подпись неверна → 403
 *   3. Если needsRegistration == true, фронт показывает форму age+gender
 *      и вызывает POST /auth/registration
 *   4. После регистрации фронт получает профиль и пускает юзера в приложение
 */

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TelegramAuthFilter telegramAuthFilter;
    private final UserService userService;


    /**
     * POST /auth
     * Header: X-Telegram-Init-Data: <Telegram.WebApp.initData>
     *
     *
     * Ответы:
     *   403 — невалидная подпись Telegram
     *   200 { needsRegistration: false, user: { telegramId, firstName, ... } }
     *   200 { needsRegistration: true  }
     */
    @PostMapping
    public ResponseEntity<AuthResponse> auth(
            @RequestHeader("X-Telegram-Init-Data") String initData) {

        if (!telegramAuthFilter.isValid(initData)) {
            return ResponseEntity.status(403).build();
        }

        TelegramUserData tgData = telegramAuthFilter.parseTelegramUserData(initData);
        AuthResponse authResponse = new AuthResponse();
        try {
            userService.updateByTgData(tgData);
            authResponse.setNeedsRegistration(false);
        } catch (UserNotFoundException e) {
            authResponse.setNeedsRegistration(true);

        }
        return ResponseEntity.ok(authResponse);
    }


     // Body: { "age": 25, "gender": "MALE" }

    @PostMapping("/registration")
    public ResponseEntity<AuthResponse> registration(
            @RequestHeader("X-Telegram-Init-Data") String initData,
            @RequestBody CreateUserRequest createUserRequest) {

        if (!telegramAuthFilter.isValid(initData)) {
            return ResponseEntity.status(403).build();
        }

        TelegramUserData tgData = telegramAuthFilter.parseTelegramUserData(initData);

        try {
            userService.getByTelegramId(tgData.getId());
            return ResponseEntity.status(409).build();
        } catch (UserNotFoundException ignored) {
        }

        User user = User.builder()
                .age(createUserRequest.getAge())
                .gender(createUserRequest.getGender())
                .telegramId(tgData.getId())
                .firstName(tgData.getFirstName())
                .lastName(tgData.getLastName())
                .username(tgData.getUsername())
                .build();
        userService.createUser(user);
        AuthResponse authResponse = new AuthResponse();
        authResponse.setNeedsRegistration(false);
        return ResponseEntity.ok(authResponse);
    }
}
