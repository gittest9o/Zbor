package com.zbor.mapper;

import com.zbor.data.entity.Event;
import com.zbor.dto.response.ShortEventResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShortEventMapper {

    ShortEventResponse toShortResponse(Event event);
}