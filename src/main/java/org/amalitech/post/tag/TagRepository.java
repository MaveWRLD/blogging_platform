package org.amalitech.post.tag;

import org.amalitech.post.tag.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * Repository interface for Tag CRUD operations.
 * Abstracts the data access layer from business logic.
 */
@Repository
@RepositoryRestResource(exported = false)
public interface TagRepository extends JpaRepository<Tag, Integer> {

    Set<Tag> findByNameIn(Set<String> names);

}

