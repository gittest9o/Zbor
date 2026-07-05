package com.zbor.service;

import com.zbor.data.entity.Event;
import com.zbor.data.entity.User;
import com.zbor.data.enums.EventCategory;
import com.zbor.data.enums.EventStatus;
import com.zbor.repository.EventRepository;
import com.zbor.dto.request.CreateEventRequest;
import com.zbor.exceptions.EventNotFoundException;
import com.zbor.exceptions.ZborException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final UserService userService;

    public Event create(Long telegramId, CreateEventRequest req){
        User organizer = userService.findByTelegramId(telegramId);
        Event event = Event.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .category(req.getCategory())
                .organizer(organizer)
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .address(req.getAddress())
                .startsAt(req.getStartsAt())
                .endsAt(req.getEndsAt())
                .maxParticipants(req.getMaxParticipants())
                .price(req.getPrice())
                .imageUrl(req.getImageUrl())
                .build();
        return eventRepository.save(event);
    }

    public Page<Event> findUpcoming(Pageable pageable){
        return eventRepository.findByStatusAndStartsAtAfter(EventStatus.PUBLISHED, LocalDateTime.now(),pageable);
    }


    public Page<Event> findByCategory(EventCategory category, Pageable pageable) {
        return eventRepository.findByCategoryAndStatusAndStartsAtAfterOrderByStartsAtAsc(
                category, EventStatus.PUBLISHED, LocalDateTime.now(), pageable);
    }

    public Page<Event> searchByKeyword(String keyword, Pageable pageable) {
        return eventRepository.searchByKeyword(EventStatus.PUBLISHED, keyword,pageable);
    }

    public Page<Event> searchByKeywordAndCategory(String keyword, EventCategory category, Pageable pageable) {
        return eventRepository.searchByKeywordAndCategory(EventStatus.PUBLISHED, category, keyword, pageable);
    }


    public Event getById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
    }

    public Event update(Long eventId, Long telegramId, @Valid CreateEventRequest req) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
        if (!Objects.equals(event.getOrganizer().getTelegramId(), telegramId))
            throw new ZborException("Permission denied");
        event.setTitle(req.getTitle());
        event.setDescription(req.getDescription());
        event.setCategory(req.getCategory());
        event.setLatitude(req.getLatitude());
        event.setLongitude(req.getLongitude());
        event.setAddress(req.getAddress());
        event.setStartsAt(req.getStartsAt());
        event.setEndsAt(req.getEndsAt());
        event.setMaxParticipants(req.getMaxParticipants());
        event.setPrice(req.getPrice());
        event.setImageUrl(req.getImageUrl());
        return eventRepository.save(event);
    }



    public void register(Long eventId, Long userTelegramId) {
        Event event = getById(eventId);

        if (!event.isPublished()) {
            throw new ZborException("The event is not available for registration");
        }
        if (event.isFull()) {
            throw new ZborException("Event is full");
        }

        User user = userService.getByTelegramId(userTelegramId);

        if (eventRepository.isParticipant(eventId, user.getId())) {
            throw new ZborException("You are already registered for this event.");
        }

        event.addParticipant(user);
        eventRepository.save(event);
        //log.info("User {} registered for event {}", userTelegramId, eventId);
    }


    public void unregister(Long eventId, Long userTelegramId) {
        Event event = getById(eventId);
        User user = userService.getByTelegramId(userTelegramId);
        if (eventRepository.isParticipant(eventId, user.getId())) {
            event.removeParticipant(user);
            eventRepository.save(event);
            //log.info("User {} unregistered from event {}", userTelegramId, eventId);
        } else
            throw new ZborException("You are not registered for this event");
    }

    public Page<Event> getOrganizedEvents(Long userId, Pageable pageable) {
        return eventRepository.findByOrganizerId(userId, pageable);
    }

    public Page<Event> getParticipatedEventsByTelegramId(Long userId, Pageable pageable) {
        return eventRepository.findByParticipants_telegramId(userId,pageable);
    }

    public Page<Event> getOrganizedEventsByTelegramId(Long telegramId, Pageable pageable) {
        return eventRepository.findByOrganizer_telegramId(telegramId, pageable);
    }

    public boolean isParticipant(Long eventId, Long telegramId){
        return eventRepository.isParticipantByTelegramId(eventId, telegramId);
    }
}
