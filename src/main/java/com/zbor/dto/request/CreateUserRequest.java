package com.zbor.dto.request;

import com.zbor.data.enums.Gender;
import lombok.Data;

@Data
public class CreateUserRequest {
    private int age;
    private Gender gender;
}
