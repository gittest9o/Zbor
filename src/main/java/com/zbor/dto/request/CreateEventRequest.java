package com.zbor.dto.request;

import com.zbor.data.enums.EventCategory;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateEventRequest {

    @NotBlank
    @Size(max = 35)
    private String title;
    @Size(max = 4000)
    private String description;

    @NotNull
    private EventCategory category;

    private Double latitude;
    private Double longitude;
    private String address;

    @NotNull
    @Future
    private LocalDateTime startsAt;
    @Future
    private LocalDateTime endsAt;
    @Min(1)
    private Integer maxParticipants;
    private BigDecimal price;
    private String imageUrl;
}
