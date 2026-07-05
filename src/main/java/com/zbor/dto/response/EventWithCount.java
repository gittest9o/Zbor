package com.zbor.dto.response;

import com.zbor.data.entity.Event;
import lombok.Data;

@Data
public class EventWithCount {
    Event event;
    Integer userCount;
}
