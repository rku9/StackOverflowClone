package com.mountblue.stackoverflowclone.repositories;

import com.mountblue.stackoverflowclone.models.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

//    Optional<Tag> findByNormalized(String normalized);

    List<Tag> findByNameIn(List<String> names);

    boolean existsByName(String name);

    List<Tag> findAllByOrderByNameAsc();
}
