package com.zbor.service;

import com.zbor.data.entity.Event;
import com.zbor.data.entity.User;
import com.zbor.data.enums.EventCategory;
import com.zbor.data.enums.EventStatus;
import com.zbor.data.enums.Gender;
import com.zbor.dto.request.CreateEventRequest;
import com.zbor.exceptions.EventNotFoundException;
import com.zbor.exceptions.ZborException;
import com.zbor.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private EventService eventService;

    // ── Фабричные методы ────────────────────────────────────────────────────

    private User buildUser(Long id, Long telegramId) {
        return User.builder()
                .id(id)
                .telegramId(telegramId)
                .firstName("Иван")
                .age(25)
                .gender(Gender.MALE)
                .events(new HashSet<>())
                .organizedEvents(new ArrayList<>())
                .build();
    }

    private Event buildPublishedEvent(Long id, User organizer) {
        return Event.builder()
                .id(id)
                .title("Тест ивент")
                .category(EventCategory.TECHNOLOGY)
                .status(EventStatus.PUBLISHED)
                .organizer(organizer)
                .startsAt(LocalDateTime.now().plusDays(3))
                .participants(new HashSet<>())
                .build();
    }

    private CreateEventRequest buildRequest() {
        CreateEventRequest req = new CreateEventRequest();
        req.setTitle("Spring Meetup");
        req.setDescription("Описание");
        req.setCategory(EventCategory.TECHNOLOGY);
        req.setStartsAt(LocalDateTime.now().plusDays(5));
        req.setMaxParticipants(30);
        req.setPrice(BigDecimal.valueOf(500));
        return req;
    }

    // ── create() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("Создаёт событие с правильным организатором")
        void create_setsOrganizer() {
            User organizer = buildUser(1L, 100L);
            CreateEventRequest req = buildRequest();

            when(userService.findByTelegramId(100L)).thenReturn(organizer);
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            Event result = eventService.create(100L, req);

            assertThat(result.getOrganizer()).isEqualTo(organizer);
            assertThat(result.getTitle()).isEqualTo("Spring Meetup");
            assertThat(result.getCategory()).isEqualTo(EventCategory.TECHNOLOGY);
            assertThat(result.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        }

        @Test
        @DisplayName("Передаёт все поля из запроса в сущность")
        void create_mapsAllFields() {
            User organizer = buildUser(1L, 100L);
            CreateEventRequest req = buildRequest();
            req.setAddress("Москва, ул. Пушкина, 1");
            req.setLatitude(55.75);
            req.setLongitude(37.62);

            when(userService.findByTelegramId(100L)).thenReturn(organizer);
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Event result = eventService.create(100L, req);

            assertThat(result.getAddress()).isEqualTo("Москва, ул. Пушкина, 1");
            assertThat(result.getLatitude()).isEqualTo(55.75);
            assertThat(result.getLongitude()).isEqualTo(37.62);
            assertThat(result.getMaxParticipants()).isEqualTo(30);
            assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(500));
        }

        @Test
        @DisplayName("Вызывает save() ровно один раз")
        void create_savesOnce() {
            when(userService.findByTelegramId(anyLong())).thenReturn(buildUser(1L, 100L));
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            eventService.create(100L, buildRequest());

            verify(eventRepository, times(1)).save(any(Event.class));
        }
    }

    // ── update() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("Обновляет поля существующего события (не создаёт новое)")
        void update_mutatesExistingEvent() {
            User organizer = buildUser(1L, 100L);
            Event existing = buildPublishedEvent(42L, organizer);

            CreateEventRequest req = buildRequest();
            req.setTitle("Новое название");
            req.setDescription("Новое описание");

            when(eventRepository.findById(42L)).thenReturn(Optional.of(existing));
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Event result = eventService.update(42L, 100L, req);

            // Убеждаемся, что id и организатор не потерялись
            assertThat(result.getId()).isEqualTo(42L);
            assertThat(result.getOrganizer()).isEqualTo(organizer);
            assertThat(result.getTitle()).isEqualTo("Новое название");
            assertThat(result.getDescription()).isEqualTo("Новое описание");
        }

        @Test
        @DisplayName("Бросает ZborException, если телеграм-id не совпадает с организатором")
        void update_throwsIfNotOrganizer() {
            User organizer = buildUser(1L, 100L);
            Event existing = buildPublishedEvent(42L, organizer);

            when(eventRepository.findById(42L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> eventService.update(42L, 999L, buildRequest()))
                    .isInstanceOf(ZborException.class)
                    .hasMessageContaining("Permission denied");

            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Бросает EventNotFoundException, если событие не найдено")
        void update_throwsIfEventNotFound() {
            when(eventRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.update(99L, 100L, buildRequest()))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("save() получает тот же объект, что был найден (не новый)")
        void update_saveReceivesOriginalObject() {
            User organizer = buildUser(1L, 100L);
            Event existing = buildPublishedEvent(42L, organizer);

            when(eventRepository.findById(42L)).thenReturn(Optional.of(existing));
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            eventService.update(42L, 100L, buildRequest());

            ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
            verify(eventRepository).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(existing);
        }
    }

    // ── getById() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById()")
    class GetByIdTests {

        @Test
        @DisplayName("Возвращает событие, если оно существует")
        void getById_returnsEvent() {
            User organizer = buildUser(1L, 100L);
            Event event = buildPublishedEvent(1L, organizer);

            when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

            Event result = eventService.getById(1L);

            assertThat(result).isEqualTo(event);
        }

        @Test
        @DisplayName("Бросает EventNotFoundException, если события нет")
        void getById_throwsIfNotFound() {
            when(eventRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.getById(999L))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

    // ── register() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("Успешная запись на событие добавляет участника")
        void register_addsParticipant() {
            User organizer = buildUser(1L, 100L);
            User participant = buildUser(2L, 200L);
            Event event = buildPublishedEvent(10L, organizer);

            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
            when(userService.getByTelegramId(200L)).thenReturn(participant);
            when(eventRepository.isParticipant(10L, 2L)).thenReturn(false);
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            eventService.register(10L, 200L);

            assertThat(event.getParticipants()).contains(participant);
            verify(eventRepository).save(event);
        }

        @Test
        @DisplayName("Бросает ZborException, если событие не опубликовано")
        void register_throwsIfNotPublished() {
            User organizer = buildUser(1L, 100L);
            Event event = buildPublishedEvent(10L, organizer);
            event.setStatus(EventStatus.CANCELLED);

            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.register(10L, 200L))
                    .isInstanceOf(ZborException.class)
                    .hasMessageContaining("not available");
        }

        @Test
        @DisplayName("Бросает ZborException, если событие заполнено")
        void register_throwsIfEventFull() {
            User organizer = buildUser(1L, 100L);
            User existing = buildUser(3L, 300L);
            Event event = buildPublishedEvent(10L, organizer);
            event.setMaxParticipants(1);
            event.addParticipant(existing);

            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.register(10L, 200L))
                    .isInstanceOf(ZborException.class)
                    .hasMessageContaining("full");
        }

        @Test
        @DisplayName("Бросает ZborException, если пользователь уже зарегистрирован")
        void register_throwsIfAlreadyRegistered() {
            User organizer = buildUser(1L, 100L);
            User participant = buildUser(2L, 200L);
            Event event = buildPublishedEvent(10L, organizer);

            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
            when(userService.getByTelegramId(200L)).thenReturn(participant);
            when(eventRepository.isParticipant(10L, 2L)).thenReturn(true);

            assertThatThrownBy(() -> eventService.register(10L, 200L))
                    .isInstanceOf(ZborException.class)
                    .hasMessageContaining("already registered");
        }
    }

    // ── unregister() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("unregister()")
    class UnregisterTests {

        @Test
        @DisplayName("Удаляет участника, если он был зарегистрирован")
        void unregister_removesParticipant() {
            User organizer = buildUser(1L, 100L);
            User participant = buildUser(2L, 200L);
            Event event = buildPublishedEvent(10L, organizer);
            event.addParticipant(participant);

            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
            when(userService.getByTelegramId(200L)).thenReturn(participant);
            when(eventRepository.isParticipant(10L, 2L)).thenReturn(true);
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            eventService.unregister(10L, 200L);

            assertThat(event.getParticipants()).doesNotContain(participant);
        }

        @Test
        @DisplayName("Бросает ZborException, если пользователь не был зарегистрирован")
        void unregister_throwsIfNotRegistered() {
            User organizer = buildUser(1L, 100L);
            User participant = buildUser(2L, 200L);
            Event event = buildPublishedEvent(10L, organizer);

            when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
            when(userService.getByTelegramId(200L)).thenReturn(participant);
            when(eventRepository.isParticipant(10L, 2L)).thenReturn(false);

            assertThatThrownBy(() -> eventService.unregister(10L, 200L))
                    .isInstanceOf(ZborException.class)
                    .hasMessageContaining("not registered");
        }
    }
}
