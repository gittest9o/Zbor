package com.zbor.controller;

import com.zbor.dto.response.MyProfile;
import com.zbor.dto.response.ShortEventResponse;
import com.zbor.mapper.ShortEventMapper;
import com.zbor.service.EventService;
import com.zbor.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class CurrentUserController {

    private final UserService userService;
    private final EventService eventService;
    private final ShortEventMapper shortEventMapper;

    @GetMapping
    public MyProfile getProfile(HttpServletRequest request) {
        Long telegramId = (Long) request.getAttribute("telegramId");
        return userService.getProfile(telegramId);
    }

    @GetMapping("/events")
    public Page<ShortEventResponse> getMyEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long telegramId = (Long) request.getAttribute("telegramId");
        var events = eventService.getParticipatedEventsByTelegramId(
                telegramId,
                PageRequest.of(page, size)
        );
        return events.map(shortEventMapper::toShortResponse);
    }

    @GetMapping("/events/organized")
    public Page<ShortEventResponse> getMyOrganizedEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long telegramId = (Long) request.getAttribute("telegramId");
        var events = eventService.getOrganizedEventsByTelegramId(
                telegramId,
                PageRequest.of(page, size)
        );
        return events.map(shortEventMapper::toShortResponse);
    }
}