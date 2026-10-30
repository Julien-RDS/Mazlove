package com.mazlove.backend.tracking;

public enum TravelMode {
  WALKING(1.35),
  CYCLING(4.2),
  DRIVING(8.3);
  public final double speed;

  TravelMode(double speed) {
    this.speed = speed;
  }
}
