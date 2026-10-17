package com.park.parkease.api.constant;

import java.time.format.DateTimeFormatter;

public class ISOConstant {
    public static final String ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss";
    public static final DateTimeFormatter ISO_8601_FORMATTER = DateTimeFormatter.ofPattern(ISOConstant.ISO_8601);
}
