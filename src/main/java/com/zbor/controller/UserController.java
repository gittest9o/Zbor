package com.zbor.controller;

import com.zbor.dto.response.MyProfile;
import com.zbor.dto.response.ShortEventResponse;
import com.zbor.dto.response.UserResponse;
import com.zbor.mapper.ShortEventMapper;
import com.zbor.mapper.ShortUserMapper;
import com.zbor.mapper.UserMapper;
import com.zbor.service.EventService;
import com.zbor.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EventService eventService;
    private final UserMapper userMapper;
    private final ShortEventMapper shortEventMapper;

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userMapper.toResponse(userService.getById(id)));
    }

    @GetMapping("/{id}/events/organized")
    public Page<ShortEventResponse> getOrganizedEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @PathVariable Long id) {
        var events = eventService.getOrganizedEvents(id, PageRequest.of(page, size));
        return events.map(shortEventMapper::toShortResponse);
    }
}
