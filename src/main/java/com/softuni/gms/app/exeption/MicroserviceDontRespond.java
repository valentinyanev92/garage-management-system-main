package com.softuni.gms.app.exeption;

public class MicroserviceDontRespond extends RuntimeException {

    public MicroserviceDontRespond(String message) {
        super(message);
    }
}
