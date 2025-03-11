package org.einsof.jabba.entities;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Scrape {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;
  private String query;
  private ScrapeStatus status;
  private Date started;
  @Column(nullable = true)
  private Date finished;
  @OneToMany(targetEntity = Domain.class, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  private List<Domain> domains;

  public Scrape() {
  }

  public Scrape(String query, ScrapeStatus status, Date started) {
    this.query = query;
    this.status = ScrapeStatus.InProgress;
    this.started = new Date();
    this.finished = null;
  }

  public Long getId() {
    return this.id;
  }

  public String getQuery() {
    return this.query;
  }

  public ScrapeStatus getStatus() {
    return this.status;
  }

  public Date getStarted() {
    return this.started;
  }

  public Optional<Date> getFinished() {
    if (this.finished != null) {
      return Optional.of(this.finished);
    } else {
      return Optional.empty();
    }
  }

  public List<Domain> getDomains() {
    return this.domains;
  }

  public void setStatus(ScrapeStatus status) {
    this.status = status;
  }

  public void setFinished(Optional<Date> finished) {
    this.finished = finished.orElse(null);
  }

  public void setDomains(List<Domain> domains) {
    this.domains = domains;
  }
}
