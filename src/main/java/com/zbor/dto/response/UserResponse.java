package com.zbor.dto.response;

import com.zbor.data.enums.Gender;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Data
public class UserResponse {

    private Long id;
    private Long telegramId;
    private String username;
    private String firstName;
    private String lastName;
    private String imageUrl;
    private int age;
    private Gender gender;
    private LocalDateTime createdAt;
}
