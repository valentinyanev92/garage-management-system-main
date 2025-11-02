package com.softuni.gms.app.web.mapper;

import com.softuni.gms.app.user.model.User;
import com.softuni.gms.app.web.dto.UserEditRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DtoMapper {

    public static UserEditRequest mapUserToUserEditRequest(User user) {

        String phoneNumber = "0" + user.getPhoneNumber().substring(3);

        return UserEditRequest.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(phoneNumber)
                .build();
    }
}
