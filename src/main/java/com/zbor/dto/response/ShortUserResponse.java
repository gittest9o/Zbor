package com.zbor.dto.response;

import lombok.Data;

@Data
public class ShortUserResponse {
    private Long id;
    private Long telegramId;
    private String username;
    private String firstName;
    private String lastName;
    private String imageUrl;
}
