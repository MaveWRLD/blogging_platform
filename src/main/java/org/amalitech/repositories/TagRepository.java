package org.amalitech.repositories;

import org.amalitech.entities.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

/**
 * Repository interface for Tag CRUD operations.
 * Abstracts the data access layer from business logic.
 */
public interface TagRepository extends JpaRepository<Tag, Integer> {

    Set<Tag> findByNameIn(Set<String> names);

}

