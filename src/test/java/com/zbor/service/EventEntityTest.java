package com.zbor.service;

import com.zbor.data.entity.Event;
import com.zbor.data.entity.User;
import com.zbor.data.enums.EventCategory;
import com.zbor.data.enums.EventStatus;
import com.zbor.data.enums.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.*;

/**
 * Тесты для бизнес-логики внутри сущности Event.
 * Не требуют Spring-контекста — чистые unit-тесты.
 */
class EventEntityTest {

    private User buildUser(Long id) {
        return User.builder()
                .id(id)
                .telegramId(id * 100)
                .firstName("Пользователь " + id)
                .age(25)
                .gender(Gender.MALE)
                .events(new HashSet<>())
                .organizedEvents(new ArrayList<>())
                .build();
    }

    private Event buildEvent(EventStatus status, Integer maxParticipants) {
        return Event.builder()
                .id(1L)
                .title("Тест")
                .category(EventCategory.TECHNOLOGY)
                .status(status)
                .organizer(buildUser(99L))
                .startsAt(LocalDateTime.now().plusDays(1))
                .maxParticipants(maxParticipants)
                .participants(new HashSet<>())
                .build();
    }

    // ── isFull() ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isFull() → false, если maxParticipants не задан")
    void isFull_false_whenMaxParticipantsNull() {
        Event event = buildEvent(EventStatus.PUBLISHED, null);
        assertThat(event.isFull()).isFalse();
    }

    @Test
    @DisplayName("isFull() → false, если участников меньше лимита")
    void isFull_false_whenBelowLimit() {
        Event event = buildEvent(EventStatus.PUBLISHED, 3);
        event.addParticipant(buildUser(1L));
        assertThat(event.isFull()).isFalse();
    }

    @Test
    @DisplayName("isFull() → true, когда достигнут лимит участников")
    void isFull_true_whenAtLimit() {
        Event event = buildEvent(EventStatus.PUBLISHED, 2);
        event.addParticipant(buildUser(1L));
        event.addParticipant(buildUser(2L));
        assertThat(event.isFull()).isTrue();
    }

    // ── isPublished() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("isPublished() → true для статуса PUBLISHED")
    void isPublished_true() {
        Event event = buildEvent(EventStatus.PUBLISHED, null);
        assertThat(event.isPublished()).isTrue();
    }

    @Test
    @DisplayName("isPublished() → false для статуса CANCELLED")
    void isPublished_false_cancelled() {
        Event event = buildEvent(EventStatus.CANCELLED, null);
        assertThat(event.isPublished()).isFalse();
    }

    @Test
    @DisplayName("isPublished() → false для статуса FINISHED")
    void isPublished_false_finished() {
        Event event = buildEvent(EventStatus.FINISHED, null);
        assertThat(event.isPublished()).isFalse();
    }

    // ── isParticipant() ──────────────────────────────────────────────────────

    @Test
    @DisplayName("isParticipant() → true, если пользователь зарегистрирован")
    void isParticipant_true() {
        Event event = buildEvent(EventStatus.PUBLISHED, null);
        User user = buildUser(1L);
        event.addParticipant(user);
        assertThat(event.isParticipant(user)).isTrue();
    }

    @Test
    @DisplayName("isParticipant() → false, если пользователь не зарегистрирован")
    void isParticipant_false() {
        Event event = buildEvent(EventStatus.PUBLISHED, null);
        User user = buildUser(1L);
        assertThat(event.isParticipant(user)).isFalse();
    }

    // ── addParticipant() / removeParticipant() ───────────────────────────────

    @Test
    @DisplayName("addParticipant() добавляет событие и в коллекцию пользователя")
    void addParticipant_updatesUserEventsList() {
        Event event = buildEvent(EventStatus.PUBLISHED, null);
        User user = buildUser(1L);

        event.addParticipant(user);

        assertThat(event.getParticipants()).contains(user);
        assertThat(user.getEvents()).contains(event);
    }

    @Test
    @DisplayName("removeParticipant() удаляет из обеих сторон связи")
    void removeParticipant_updatesUserEventsList() {
        Event event = buildEvent(EventStatus.PUBLISHED, null);
        User user = buildUser(1L);
        event.addParticipant(user);

        event.removeParticipant(user);

        assertThat(event.getParticipants()).doesNotContain(user);
        assertThat(user.getEvents()).doesNotContain(event);
    }

    @Test
    @DisplayName("getRegistrationCount() возвращает актуальное число участников")
    void getRegistrationCount_correct() {
        Event event = buildEvent(EventStatus.PUBLISHED, null);
        assertThat(event.getRegistrationCount()).isZero();

        event.addParticipant(buildUser(1L));
        event.addParticipant(buildUser(2L));
        assertThat(event.getRegistrationCount()).isEqualTo(2);
    }
}
