package com.zbor.service;

import com.zbor.data.entity.User;
import com.zbor.data.enums.Gender;
import com.zbor.dto.request.TelegramUserData;
import com.zbor.dto.response.MyProfile;
import com.zbor.exceptions.UserNotFoundException;
import com.zbor.mapper.ProfileMapper;
import com.zbor.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileMapper profileMapper;

    @InjectMocks
    private UserService userService;

    private User buildUser(Long id, Long telegramId) {
        return User.builder()
                .id(id)
                .telegramId(telegramId)
                .firstName("Иван")
                .lastName("Иванов")
                .username("ivan")
                .age(25)
                .gender(Gender.MALE)
                .build();
    }

    // ── getByTelegramId() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getByTelegramId()")
    class GetByTelegramIdTests {

        @Test
        @DisplayName("Возвращает пользователя, если он существует")
        void getByTelegramId_returnsUser() {
            User user = buildUser(1L, 123L);
            when(userRepository.findByTelegramId(123L)).thenReturn(Optional.of(user));

            User result = userService.getByTelegramId(123L);

            assertThat(result).isEqualTo(user);
        }

        @Test
        @DisplayName("Бросает UserNotFoundException, если пользователь не найден")
        void getByTelegramId_throwsIfNotFound() {
            when(userRepository.findByTelegramId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getByTelegramId(999L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ── getById() ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById()")
    class GetByIdTests {

        @Test
        @DisplayName("Возвращает пользователя по внутреннему id")
        void getById_returnsUser() {
            User user = buildUser(1L, 123L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            User result = userService.getById(1L);

            assertThat(result).isEqualTo(user);
        }

        @Test
        @DisplayName("Бросает UserNotFoundException, если id не найден")
        void getById_throwsIfNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getById(999L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ── updateByTgData() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateByTgData()")
    class UpdateByTgDataTests {

        @Test
        @DisplayName("Обновляет имя, фамилию и username из TelegramUserData")
        void updateByTgData_updatesFields() {
            User user = buildUser(1L, 123L);
            when(userRepository.findByTelegramId(123L)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TelegramUserData tgData = new TelegramUserData(123L, "Пётр", "Петров", "petr");
            userService.updateByTgData(tgData);

            assertThat(user.getFirstName()).isEqualTo("Пётр");
            assertThat(user.getLastName()).isEqualTo("Петров");
            assertThat(user.getUsername()).isEqualTo("petr");
        }

        @Test
        @DisplayName("Бросает UserNotFoundException, если пользователь не найден")
        void updateByTgData_throwsIfNotFound() {
            when(userRepository.findByTelegramId(999L)).thenReturn(Optional.empty());

            TelegramUserData tgData = new TelegramUserData(999L, "X", null, null);

            assertThatThrownBy(() -> userService.updateByTgData(tgData))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Обновляет поля даже если last_name и username — null")
        void updateByTgData_handlesNullLastNameAndUsername() {
            User user = buildUser(1L, 123L);
            when(userRepository.findByTelegramId(123L)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TelegramUserData tgData = new TelegramUserData(123L, "Аноним", null, null);
            userService.updateByTgData(tgData);

            assertThat(user.getFirstName()).isEqualTo("Аноним");
            assertThat(user.getLastName()).isNull();
            assertThat(user.getUsername()).isNull();
        }
    }

    // ── createUser() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createUser()")
    class CreateUserTests {

        @Test
        @DisplayName("Сохраняет пользователя через репозиторий")
        void createUser_callsSave() {
            User user = buildUser(null, 123L);
            when(userRepository.save(any())).thenReturn(user);

            userService.createUser(user);

            verify(userRepository, times(1)).save(user);
        }
    }

    // ── getProfile() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getProfile()")
    class GetProfileTests {

        @Test
        @DisplayName("Возвращает MyProfile через маппер")
        void getProfile_returnsProfile() {
            User user = buildUser(1L, 123L);
            MyProfile profile = new MyProfile();
            when(userRepository.findByTelegramId(123L)).thenReturn(Optional.of(user));
            when(profileMapper.toProfile(user)).thenReturn(profile);

            MyProfile result = userService.getProfile(123L);

            assertThat(result).isSameAs(profile);
        }

        @Test
        @DisplayName("Бросает UserNotFoundException, если пользователь не найден")
        void getProfile_throwsIfNotFound() {
            when(userRepository.findByTelegramId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getProfile(999L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}
