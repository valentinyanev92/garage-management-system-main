package com.softuni.gms.app.exeption;

public class CarAlreadyExistsException extends RuntimeException {

    public CarAlreadyExistsException(String message) {
        super(message);
    }
}
