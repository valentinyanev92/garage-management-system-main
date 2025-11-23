package com.softuni.gms.app.exeption;

public class MicroserviceDontRespondExceptionMessages {

    private MicroserviceDontRespondExceptionMessages() {
    }

    public static final String INVOICE_SERVICE_UNAVAILABLE = "Invoice service is unavailable";
    public static final String INVOICE_SERVICE_NOT_AVAILABLE_TRY_AGAIN = "Invoice service is temporarily unavailable. Please try again later.";
    public static final String INVOICE_SERVICE_NOT_AVAILABLE_CANNOT_DOWNLOAD = "Unable to download invoice. Please try again later.";
    public static final String NOTIFICATION_SERVICE_UNAVAILABLE = "Notification service is unavailable";
    public static final String NOTIFICATION_SERVICE_TRY_AGAIN = "Notification service is temporarily unavailable. Completion message was not sent.";
}
