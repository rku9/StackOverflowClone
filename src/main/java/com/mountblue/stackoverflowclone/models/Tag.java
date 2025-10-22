package com.mountblue.stackoverflowclone.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Entity
@Table(name = "tags")
@Getter
@Setter
public class Tag extends BaseModel {

    private String name;

    private String normalized;

    @ManyToMany(mappedBy = "tags")
    private List<Question> questions;
}
