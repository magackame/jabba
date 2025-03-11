package org.einsof.jabba.entities;

public enum DomainState {
  Available,
  Registered,
  Error;

  @Override
  public String toString() {
    switch (this) {
      case Available: {
        return "available";
      }
      case Registered: {
        return "registered";
      }
      case Error: {
        return "error";
      }
      default: {
        return null;
      }
    }
  }
}
