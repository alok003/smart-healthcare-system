package com.project.notificationService.Model;

import lombok.Getter;

@Getter
public enum Subjects {
    WELCOME("Welcome to Smart Healthcare System");

    private final String subject;

    Subjects(String subject) {
        this.subject = subject;
    }

}
