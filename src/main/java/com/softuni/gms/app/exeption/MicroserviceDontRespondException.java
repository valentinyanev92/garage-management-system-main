package com.softuni.gms.app.exeption;

public class MicroserviceDontRespondException extends RuntimeException {

    public MicroserviceDontRespondException(String message) {
        super(message);
    }
}
