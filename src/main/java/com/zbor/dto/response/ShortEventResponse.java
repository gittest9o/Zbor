package com.zbor.dto.response;

import com.zbor.data.enums.EventStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ShortEventResponse {
    private Long id;
    private String title;
    private String imageUrl;
    private LocalDateTime startsAt;
    private Integer maxParticipants;
    private Integer participantCount;
    private BigDecimal price;
    private EventStatus status;

}
