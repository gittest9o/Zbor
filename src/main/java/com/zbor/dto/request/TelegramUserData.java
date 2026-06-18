package com.zbor.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TelegramUserData {
    private Long id;
    private String firstName;
    private String lastName;
    private String username;
}

