package com.mountblue.stackoverflowclone.dtos;

import com.mountblue.stackoverflowclone.models.FilterType;
import com.mountblue.stackoverflowclone.models.SortType;
import java.util.List;

public record QuestionFilterRequestDto(
        List<FilterType> filterTypes,
        Integer daysOld,
        SortType sortBy,
        List<String> tags,
        Boolean useWatchedTags
) { }
