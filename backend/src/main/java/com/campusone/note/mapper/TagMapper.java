package com.campusone.note.mapper;

import com.campusone.note.dto.response.TagResponse;
import com.campusone.note.entity.Tag;
import org.springframework.stereotype.Component;

@Component
public class TagMapper {

    public TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
