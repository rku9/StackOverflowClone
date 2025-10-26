package com.mountblue.stackoverflowclone.services;

import com.mountblue.stackoverflowclone.models.Tag;
import com.mountblue.stackoverflowclone.repositories.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Transactional(readOnly = true)
    public List<Tag> getAllTags() {
        return tagRepository.findAllByOrderByNameAsc();
    }
}
