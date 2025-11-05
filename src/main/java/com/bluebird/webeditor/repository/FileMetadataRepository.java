package com.bluebird.webeditor.repository;

import com.bluebird.webeditor.entity.FileMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    Optional<FileMetadata> findByStoredFilename(String storedFilename);

    @Query("SELECT f FROM FileMetadata f WHERE f.deleted = false")
    Page<FileMetadata> findAllActive(Pageable pageable);

    @Query("SELECT f FROM FileMetadata f WHERE f.deleted = false AND f.fileType = :fileType")
    Page<FileMetadata> findByFileType(@Param("fileType") FileMetadata.FileType fileType, Pageable pageable);

    @Query("SELECT f FROM FileMetadata f WHERE f.deleted = false AND f.originalFilename LIKE %:filename%")
    List<FileMetadata> searchByFilename(@Param("filename") String filename);

    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileMetadata f WHERE f.deleted = false")
    Long getTotalStorageUsed();
}
