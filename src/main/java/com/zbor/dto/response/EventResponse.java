package com.zbor.dto.response;

import com.zbor.data.enums.EventCategory;
import com.zbor.data.enums.EventStatus;
import com.zbor.data.enums.UserRole;
import com.zbor.mapper.ShortUserMapper;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private EventCategory category;
    private EventStatus status;
    private ShortUserResponse organizer;
    private UserRole userRole;
    private Double latitude;
    private Double longitude;
    private String address;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Integer maxParticipants;
    //private final ShortUserMapper shortUserMapper;
    private BigDecimal price;
    private String imageUrl;
    private LocalDateTime createdAt;
}
