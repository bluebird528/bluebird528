package com.bluebird.webeditor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileListResponse {

    private List<FileUploadResponse> files;
    private long totalCount;
    private int page;
    private int size;
    private long totalPages;
}
