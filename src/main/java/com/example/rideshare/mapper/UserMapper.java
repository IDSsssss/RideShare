package com.example.rideshare.mapper;

import com.example.rideshare.dto.UserDto;
import com.example.rideshare.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    UserDto toDto(User user);

    User toEntity(UserDto userDTO);

    List<UserDto> toDtoList(List<User> users);
}