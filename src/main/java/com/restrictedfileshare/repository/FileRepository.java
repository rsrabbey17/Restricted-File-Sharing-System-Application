package com.restrictedfileshare.repository;

import com.restrictedfileshare.model.FileEntity;
import com.restrictedfileshare.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    Optional<FileEntity> findByDownloadToken(String token);
    List<FileEntity> findByUploadedBy(User user);

    @Query("SELECT f FROM FileEntity f JOIN FileAccess fa ON f.id = fa.file.id WHERE fa.user.id = :userId AND f.active = true")
    List<FileEntity> findFilesAccessibleByUser(@Param("userId") Long userId);
}