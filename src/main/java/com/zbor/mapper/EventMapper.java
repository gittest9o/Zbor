package com.zbor.mapper;

import com.zbor.data.entity.Event;
import com.zbor.dto.response.EventResponse;
import com.zbor.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventMapper {

    private final ShortUserMapper shortUserMapper;
    private final EventRepository eventRepository;


    public EventResponse toResponse(Event event) {
        if (event == null) {
            return null;
        }

        EventResponse.EventResponseBuilder eventResponse = EventResponse.builder();

        eventResponse.id(event.getId());
        eventResponse.title(event.getTitle());
        eventResponse.description(event.getDescription());
        eventResponse.category(event.getCategory());
        eventResponse.status(event.getStatus());
        eventResponse.organizer(shortUserMapper.toShortResponse(event.getOrganizer()));
        eventResponse.latitude(event.getLatitude());
        eventResponse.longitude(event.getLongitude());
        eventResponse.address(event.getAddress());
        eventResponse.startsAt(event.getStartsAt());
        eventResponse.endsAt(event.getEndsAt());
        eventResponse.maxParticipants(event.getMaxParticipants());
        eventResponse.price(event.getPrice());
        eventResponse.imageUrl(event.getImageUrl());
        eventResponse.createdAt(event.getCreatedAt());
        eventResponse.participantCount(eventRepository.countParticipants(event.getId()));

        return eventResponse.build();
    }
}