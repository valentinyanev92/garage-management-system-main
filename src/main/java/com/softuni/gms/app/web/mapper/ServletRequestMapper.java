package com.softuni.gms.app.web.mapper;

import com.softuni.gms.app.web.dto.CarEditRequest;
import com.softuni.gms.app.web.dto.CarRegisterRequest;
import com.softuni.gms.app.web.dto.RegisterRequest;
import com.softuni.gms.app.web.dto.UserEditRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ServletRequestMapper {

    public static RegisterRequest extractRegisterRequest(HttpServletRequest request) {

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(request.getParameter("username"));
        registerRequest.setFirstName(request.getParameter("firstName"));
        registerRequest.setLastName(request.getParameter("lastName"));
        registerRequest.setEmail(request.getParameter("email"));
        registerRequest.setPhoneNumber(request.getParameter("phoneNumber"));
        registerRequest.setPassword(request.getParameter("password"));
        return registerRequest;
    }

    public static CarRegisterRequest extractCarRegisterRequest(HttpServletRequest request) {

        CarRegisterRequest carRegisterRequest = new CarRegisterRequest();
        carRegisterRequest.setBrand(request.getParameter("brand"));
        carRegisterRequest.setModel(request.getParameter("model"));
        carRegisterRequest.setVin(request.getParameter("vin"));
        carRegisterRequest.setPlateNumber(request.getParameter("plateNumber"));
        return carRegisterRequest;
    }

    public static CarEditRequest extractCarEditRequest(HttpServletRequest request) {

        CarEditRequest carEditRequest = new CarEditRequest();
        carEditRequest.setBrand(request.getParameter("brand"));
        carEditRequest.setModel(request.getParameter("model"));
        carEditRequest.setVin(request.getParameter("vin"));
        carEditRequest.setPlateNumber(request.getParameter("plateNumber"));
        carEditRequest.setPictureUrl(request.getParameter("pictureUrl"));
        return carEditRequest;
    }

    public static UserEditRequest extractUserEditRequest(HttpServletRequest request) {

        UserEditRequest userEditRequest = new UserEditRequest();
        userEditRequest.setFirstName(request.getParameter("firstName"));
        userEditRequest.setLastName(request.getParameter("lastName"));
        userEditRequest.setEmail(request.getParameter("email"));
        userEditRequest.setPhoneNumber(request.getParameter("phoneNumber"));
        return userEditRequest;
    }
}
