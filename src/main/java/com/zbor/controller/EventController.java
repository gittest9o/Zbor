package com.zbor.controller;

import com.zbor.data.entity.Event;
import com.zbor.data.enums.EventCategory;
import com.zbor.data.enums.UserRole;
import com.zbor.dto.request.CreateEventRequest;
import com.zbor.dto.response.EventResponse;
import com.zbor.dto.response.ShortEventResponse;
import com.zbor.dto.response.ShortUserResponse;
import com.zbor.exceptions.ZborException;
import com.zbor.mapper.EventMapper;
import com.zbor.mapper.ShortEventMapper;
import com.zbor.mapper.ShortUserMapper;
import com.zbor.service.EventService;
import com.zbor.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventMapper eventMapper;
    private final ShortEventMapper shortEventMapper;
    private final UserService userService;
    private final ShortUserMapper shortUserMapper;

    @GetMapping
    public Page<ShortEventResponse> getUpcomingEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) EventCategory category,
            @RequestParam(required = false) String keyword) {
        Page<Event> events;
        if (category == null && keyword == null) {
            events = eventService.findUpcoming(PageRequest.of(page, size));
        } else if (category == null) {
            events = eventService.searchByKeyword(keyword, PageRequest.of(page, size));
        } else if (keyword == null) {
            events = eventService.findByCategory(category, PageRequest.of(page, size));
        } else {
            events = eventService.searchByKeywordAndCategory(
                    keyword,
                    category,
                    PageRequest.of(page, size)
            );
        }

        return events.map(shortEventMapper::toShortResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long id,
        HttpServletRequest request){
        Long telegramId = getTelegramId(request);
        EventResponse eventResponse = eventMapper.toResponse(eventService.getById(id));
        if (Objects.equals(telegramId, eventResponse.getOrganizer().getTelegramId())){
            eventResponse.setUserRole(UserRole.ORGANIZER);
        } else if (eventService.isParticipant(eventResponse.getId(),telegramId)) {
            eventResponse.setUserRole(UserRole.PARTICIPANT);
        } else {
            eventResponse.setUserRole(UserRole.NONE);
        }

        return ResponseEntity.ok(eventResponse);
    }

    @GetMapping("/{id}/participants")
    public Page<ShortUserResponse> getParticipants(@PathVariable Long id,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size){
        return userService.getParticipants(id,PageRequest.of(page, size))
                .map(shortUserMapper::toShortResponse);
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest req,
            HttpServletRequest request) {

        Long telegramId = getTelegramId(request);
        Event event = eventService.create(telegramId, req);
        return ResponseEntity.ok(eventMapper.toResponse(event));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody CreateEventRequest req,
            HttpServletRequest request) {

        Long telegramId = getTelegramId(request);
        Event event = eventService.update(id, telegramId, req);
        return ResponseEntity.ok(eventMapper.toResponse(event));
    }

     @PostMapping("/{id}/register")
     public ResponseEntity<Map<String, String>> register(@PathVariable Long id, HttpServletRequest request) {
         eventService.register(id, getTelegramId(request));
         return ResponseEntity.ok(Map.of("status", "registered"));
     }

     @DeleteMapping("/{id}/register")
     public ResponseEntity<Map<String, String>> unregister(@PathVariable Long id, HttpServletRequest request) {
         eventService.unregister(id, getTelegramId(request));
         return ResponseEntity.ok(Map.of("status", "unregistered"));
     }

    private Long getTelegramId(HttpServletRequest request) {
        Long id = (Long) request.getAttribute("telegramId");
        if (id == null) throw new ZborException("Unauthorized");
        return id;
    }
}
