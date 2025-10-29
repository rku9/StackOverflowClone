package com.mountblue.stackoverflowclone.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class ImageUploadService {

    private final Cloudinary cloudinary;

    @Autowired
    public ImageUploadService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String uploadImage(MultipartFile image) throws IOException {
        if (image.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }

        if (image.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Image size exceeds 5MB limit");
        }

        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        Map<String, Object> uploadResult = cloudinary.uploader().upload(
                image.getBytes(),
                ObjectUtils.asMap(
                        "folder", "stackoverflow-clone",
                        "resource_type", "image",
                        "use_filename", true,
                        "unique_filename", true
                )
        );

        return (String) uploadResult.get("secure_url");
    }

    public String uploadImage(MultipartFile image, String folder) throws IOException {
        if (image.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }

        if (image.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Image size exceeds 5MB limit");
        }

        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        Map<String, Object> uploadResult = cloudinary.uploader().upload(
                image.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder != null && !folder.isBlank() ? folder : "stackoverflow-clone",
                        "resource_type", "image",
                        "use_filename", true,
                        "unique_filename", true
                )
        );

        return (String) uploadResult.get("secure_url");
    }

    /**
     * Upload and return full Cloudinary response (secure_url, public_id, etc.).
     */
    public Map<String, Object> uploadImageWithResult(MultipartFile image, String folder) throws IOException {
        if (image.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }

        if (image.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Image size exceeds 5MB limit");
        }

        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        return cloudinary.uploader().upload(
                image.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder != null && !folder.isBlank() ? folder : "stackoverflow-clone",
                        "resource_type", "image",
                        "use_filename", true,
                        "unique_filename", true
                )
        );
    }

    /**
     * Delete an image by Cloudinary public_id (include folder path in public_id).
     */
    public void deleteByPublicId(String publicId) throws IOException {
        if (publicId == null || publicId.isBlank()) return;
        cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("invalidate", true));
    }
}
