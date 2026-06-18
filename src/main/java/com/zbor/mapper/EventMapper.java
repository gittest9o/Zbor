package com.zbor.mapper;

import com.zbor.data.entity.Event;
import com.zbor.dto.response.EventResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface EventMapper {

    @Mapping(target = "participants", source = "participants")
    EventResponse toResponse(Event event);
}