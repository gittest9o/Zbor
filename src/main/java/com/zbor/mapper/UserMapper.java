package com.zbor.mapper;

import com.zbor.data.entity.User;
import com.zbor.dto.response.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}