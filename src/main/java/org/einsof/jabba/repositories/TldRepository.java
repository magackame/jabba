package org.einsof.jabba.repositories;

import org.einsof.jabba.entities.Tld;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TldRepository extends CrudRepository<Tld, Long> {
}
