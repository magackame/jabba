package org.einsof.jabba.entities;

import java.util.Date;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Domain {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;
  @ManyToOne(targetEntity = Scrape.class)
  private Scrape scrape;
  private String domain;
  private DomainState state;
  @Column(nullable = true)
  private String price;
  @Column(nullable = true)
  private String renewalPrice;
  @Column(nullable = true)
  private Date latestHistory;

  public Domain() {
  }

  public Domain(String domain, DomainState state, Optional<String> price,
      Optional<String> renewalPrice, Optional<Date> latestHistory) {
    this.domain = domain;
    this.state = state;
    this.price = price.orElse(null);
    this.renewalPrice = renewalPrice.orElse(null);
    this.latestHistory = latestHistory.orElse(null);
  }

  public Scrape getScrape() {
    return this.scrape;
  }

  public String getDomain() {
    return this.domain;
  }

  public DomainState getState() {
    return this.state;
  }

  public Optional<String> getPrice() {
    if (this.price != null) {
      return Optional.of(this.price);
    } else {
      return Optional.empty();
    }
  }

  public Optional<String> getRenewalPrice() {
    if (this.renewalPrice != null) {
      return Optional.of(this.renewalPrice);
    } else {
      return Optional.empty();
    }
  }

  public Optional<Date> getLatestHistory() {
    if (this.latestHistory != null) {
      return Optional.of(this.latestHistory);
    } else {
      return Optional.empty();
    }
  }

  public void setScrape(Scrape scrape) {
    this.scrape = scrape;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public void setState(DomainState state) {
    this.state = state;
  }

  public void setPrice(Optional<String> price) {
    this.price = price.orElse(null);
  }

  public void setRenewalPrice(Optional<String> renewalPrice) {
    this.renewalPrice = renewalPrice.orElse(null);
  }

  public void setLatestHistory(Optional<Date> latestHistory) {
    this.latestHistory = latestHistory.orElse(null);
  }
}
