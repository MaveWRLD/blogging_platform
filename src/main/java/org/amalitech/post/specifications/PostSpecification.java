package org.amalitech.post.specifications;


import org.amalitech.post.Post;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Set;

public class PostSpecification {

    public static Specification<Post> byTitle(String title) {
        if (title == null || title.isEmpty())
            return null;
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), "%" + title.toLowerCase() + "%");
    }

    public static Specification<Post> byAuthor(String author) {
        if (author == null || author.isEmpty())
            return null;
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("author")), "%" + author.toLowerCase() + "%");
    }

    public static Specification<Post> byTag(Set<String> tags) {
        if (tags == null || tags.isEmpty())
            return null;
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);
            return root.join("tags").get("name").in(tags);
        };
    }

    public static Specification<Post> byPublicatedAtRange(Instant from, Instant to) {
        if (from == null && to == null)
            return null;
        return (root, query, criteriaBuilder) -> {
            if (from != null && to != null) {
                return criteriaBuilder.between(root.get("publishedAt"), from, to);
            } else if (from != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("publishedAt"), from);
            } else {
                return criteriaBuilder.lessThanOrEqualTo(root.get("publishedAt"), to);
            }
        };
    }
}
