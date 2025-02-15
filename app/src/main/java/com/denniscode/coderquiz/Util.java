package com.denniscode.coderquiz;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class Util {
    public static String formatTimestamp(String timestampStr) {
        // Define the input format (yyyyMMddHHmmss)
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        // Parse the input timestamp to a LocalDateTime object
        LocalDateTime dateTime = LocalDateTime.parse(timestampStr, inputFormatter);

        // Define the desired output format (e.g., "Tuesday Jan 2 2025 4:30 PM")
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("EEEE MMM d yyyy h:mm a");

        // Format the LocalDateTime to the desired string
        return dateTime.format(outputFormatter);
    }

    public static String generateStatId(String timeStamp) {
        String uuid = UUID.randomUUID().toString();
        return uuid + "_" + timeStamp;
    }


}
