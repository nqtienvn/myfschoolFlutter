package vn.edu.fpt.myfschool.common.dto;

public record AttachmentDto(Long id, String fileUrl, String fileName, Long fileSize, String mimeType) {}
