package com.zbor.mapper;

import com.zbor.data.entity.User;
import com.zbor.dto.response.MyProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {ShortEventMapper.class})
public interface ProfileMapper {
    MyProfile toProfile(User user);
}
