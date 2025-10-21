package com.mountblue.stackoverflowclone.repositories;

import com.mountblue.stackoverflowclone.models.Upload;
import com.mountblue.stackoverflowclone.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UploadRepository extends JpaRepository<Upload, Long> {

    List<Upload> findByOwner(User owner);

    List<Upload> findByOwnerId(Long ownerId);

    List<Upload> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    Optional<Upload> findByPath(String path);
}
