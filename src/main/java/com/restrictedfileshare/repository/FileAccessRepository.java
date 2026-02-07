package com.restrictedfileshare.repository;

import com.restrictedfileshare.model.FileAccess;
import com.restrictedfileshare.model.FileEntity;
import com.restrictedfileshare.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileAccessRepository extends JpaRepository<FileAccess, Long> {
    boolean existsByFileAndUser(FileEntity file, User user);

    @Query("SELECT COUNT(fa) > 0 FROM FileAccess fa WHERE fa.file.id = :fileId AND fa.user.id = :userId")
    boolean hasAccess(@Param("fileId") Long fileId, @Param("userId") Long userId);

    @Query("SELECT u.email FROM FileAccess fa JOIN fa.user u WHERE fa.file.id = :fileId")
    List<String> findUserEmailsWithAccess(@Param("fileId") Long fileId);

    void deleteByFileAndUser(FileEntity file, User user);
}