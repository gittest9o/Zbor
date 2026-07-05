package com.zbor.mapper;

import com.zbor.data.entity.Event;
import com.zbor.dto.response.EventResponse;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface EventMapper {

    EventResponse toResponse(Event event);
}