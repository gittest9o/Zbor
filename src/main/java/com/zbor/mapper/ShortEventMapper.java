package com.zbor.mapper;

import com.zbor.data.entity.Event;
import com.zbor.dto.response.ShortEventResponse;
import com.zbor.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public  class ShortEventMapper {

    private final EventRepository eventRepository;

    public ShortEventResponse toShortResponse(Event event){
        ShortEventResponse shortEventResponse = new ShortEventResponse();
        shortEventResponse.setId(event.getId());
        shortEventResponse.setTitle(event.getTitle());
        shortEventResponse.setImageUrl(event.getImageUrl());
        shortEventResponse.setStartsAt(event.getStartsAt());
        shortEventResponse.setMaxParticipants(event.getMaxParticipants());
        shortEventResponse.setParticipantCount(eventRepository.countParticipants(event.getId()));
        shortEventResponse.setPrice(event.getPrice());
        shortEventResponse.setStatus(event.getStatus());
        return shortEventResponse;
    };
}