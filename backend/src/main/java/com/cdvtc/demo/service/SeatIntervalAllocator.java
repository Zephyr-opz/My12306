package com.cdvtc.demo.service;

public final class SeatIntervalAllocator {

    private SeatIntervalAllocator() {
    }

    public static boolean overlaps(int existingStart, int existingEnd,
                                   int requestedStart, int requestedEnd) {
        validate(existingStart, existingEnd);
        validate(requestedStart, requestedEnd);
        return existingStart < requestedEnd && existingEnd > requestedStart;
    }

    private static void validate(int start, int end) {
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("座位区间无效");
        }
    }
}
