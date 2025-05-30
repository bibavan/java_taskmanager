package com.max.taskmanager.dto;

public class CreateNotificationRequest {
    private String message;

    public CreateNotificationRequest() {

    }

    public CreateNotificationRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}