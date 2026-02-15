package org.amalitech.repositories;

import org.amalitech.entities.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for Tag CRUD operations.
 * Abstracts the data access layer from business logic.
 */
public interface TagRepository extends JpaRepository<Tag, Integer> {

    List<Tag> findByNameIn(List<String> names);
}

