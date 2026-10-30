package com.mazlove.backend.tracking;

public record PositionDTO(
    double lat, double lng, Double accuracy, Double heading, Double speed, long ts, long seq) {}
