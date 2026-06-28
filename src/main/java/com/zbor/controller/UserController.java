package com.zbor.controller;

import com.zbor.dto.response.MyProfile;
import com.zbor.dto.response.UserResponse;
import com.zbor.mapper.UserMapper;
import com.zbor.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return userMapper.toResponse(userService.getById(id));
    }

    @GetMapping("/me")
    public MyProfile me(HttpServletRequest request) {
        Long id = (Long) request.getAttribute("telegramId");
        return userService.getProfile(id);
    }

}
