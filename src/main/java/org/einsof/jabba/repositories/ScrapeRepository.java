package org.einsof.jabba.repositories;

import java.util.List;

import org.einsof.jabba.entities.Scrape;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScrapeRepository extends CrudRepository<Scrape, Long> {
  public List<Scrape> findAllByOrderByStartedDesc();
}
