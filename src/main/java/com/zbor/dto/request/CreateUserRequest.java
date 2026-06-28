package com.zbor.dto.request;

import com.zbor.data.enums.Gender;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotNull
    @Min(12)
    @Max(80)
    private int age;
    @NotNull
    private Gender gender;
}
