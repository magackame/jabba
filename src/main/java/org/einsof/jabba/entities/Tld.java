package org.einsof.jabba.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Tld {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String tld;

  public Tld() {
  }

  public Tld(String tld) {
    this.tld = tld;
  }

  public String getTld() {
    return this.tld;
  }

  public void setTld(String tld) {
    this.tld = tld;
  }
}
