package com.zbor.mapper;

import com.zbor.data.entity.User;
import com.zbor.dto.response.ShortUserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ShortUserMapper {
    ShortUserResponse toShortResponse(User user);
}
