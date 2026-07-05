package com.zbor.repository;

import com.zbor.data.entity.Event;
import com.zbor.data.enums.EventCategory;
import com.zbor.data.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByOrganizerId(Long organizerId, Pageable pageable);

    Page<Event> findByParticipants_Id(Long userId, Pageable pageable);

    Page<Event> findByParticipants_telegramId(Long telegramId, Pageable pageable);

    // Опубликованные события с пагинацией
    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    // Поиск по категории
    Page<Event> findByStatusAndCategory(EventStatus status, EventCategory category, Pageable pageable);

    // Поиск по ключевому слову в названии или описании
    @Query("SELECT e FROM Event e WHERE e.status = :status AND " +
            "(LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Event> searchByKeyword(@Param("status") EventStatus status,
                                @Param("keyword") String keyword,
                                Pageable pageable);

    // Поиск по категории и ключевому слову
    @Query("SELECT e FROM Event e WHERE e.status = :status AND e.category = :category AND " +
            "(LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Event> searchByKeywordAndCategory(@Param("status") EventStatus status,
                                           @Param("category") EventCategory category,
                                           @Param("keyword") String keyword,
                                           Pageable pageable);

    // Предстоящие события
    Page<Event> findByStatusAndStartsAtAfter(EventStatus status, LocalDateTime after, Pageable pageable);

    // Бесплатные события (price = 0 или null)
    @Query("SELECT e FROM Event e WHERE e.status = :status AND (e.price IS NULL OR e.price = 0)")
    Page<Event> findFreeEvents(@Param("status") EventStatus status, Pageable pageable);

    // Количество участников (без загрузки коллекции)
    @Query("SELECT COUNT(p) FROM Event e JOIN e.participants p WHERE e.id = :eventId")
    int countParticipants(@Param("eventId") Long eventId);

    // Проверка — записан ли пользователь на событие
    @Query("SELECT COUNT(p) > 0 FROM Event e JOIN e.participants p WHERE e.id = :eventId AND p.id = :userId")
    boolean isParticipant(@Param("eventId") Long eventId, @Param("userId") Long userId);

    @Query("SELECT COUNT(p) > 0 FROM Event e JOIN e.participants p WHERE e.id = :eventId AND p.id = :telegramId")
    boolean isParticipantByTelegramId(@Param("eventId") Long eventId, @Param("telegramId") Long telegramId);

    Page<Event> findByCategoryAndStatusAndStartsAtAfterOrderByStartsAtAsc(
            EventCategory category, EventStatus status, LocalDateTime after, Pageable pageable);

    Page<Event> findByOrganizer_telegramId(Long telegramId, Pageable pageable);
}