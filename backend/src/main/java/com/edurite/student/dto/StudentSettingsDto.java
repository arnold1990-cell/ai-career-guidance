package com.edurite.student.dto; // declares the package path for this Java file

public record StudentSettingsDto( // supports the surrounding application logic
        boolean inAppNotificationsEnabled, // supports the surrounding application logic
        boolean emailNotificationsEnabled, // supports the surrounding application logic
        boolean smsNotificationsEnabled // supports the surrounding application logic
) {} // supports the surrounding application logic
