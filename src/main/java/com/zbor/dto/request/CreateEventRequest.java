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

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double longitude;

    @Size(max = 150)
    private String address;

    @NotNull
    @Future
    private LocalDateTime startsAt;

    @Future
    private LocalDateTime endsAt;

    @Min(1)
    @Max(100)
    private Integer maxParticipants;

    @Max(1000)
    private BigDecimal price;

    @Size(max = 2000)
    private String imageUrl;
}
