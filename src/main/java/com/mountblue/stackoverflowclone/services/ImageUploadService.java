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

        Map uploadResult = cloudinary.uploader().upload(
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
}
