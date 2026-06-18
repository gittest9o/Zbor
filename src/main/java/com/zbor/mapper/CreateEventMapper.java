package com.zbor.mapper;

import com.zbor.data.entity.Event;
import com.zbor.dto.request.CreateEventRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CreateEventMapper {
    Event toEvent(CreateEventRequest event);
}
