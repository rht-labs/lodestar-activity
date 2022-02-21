package com.redhat.labs.lodestar.activity.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@JsonIgnoreProperties(value = { "added", "modified", "removed" }, allowSetters = true)
public class Activity {
    
    @Builder.Default @Transient private List<String> added = new ArrayList<>();
    @Builder.Default @Transient private List<String> modified = new ArrayList<>();
    @Builder.Default @Transient private List<String> removed = new ArrayList<>();

    @Id
    private String id;
    private String shortId;
    private String title;
    private String authorName;
    private String authorEmail;
    private String committerName;
    private String committerEmail;
    private OffsetDateTime authoredDate;
    private OffsetDateTime committedDate;
    @Column(columnDefinition="TEXT")
    private String message;
    @JsonProperty(value = "web_url")
    private String url;
    
    private Long projectId;
    private String engagementUuid;
    private String region;
    
    public boolean didFileChange(List<String> fileName) {
        Set<String> changedFiles = new HashSet<>(added);
        changedFiles.addAll(modified);
        changedFiles.addAll(removed);
        
        return changedFiles.stream().anyMatch(fileName::contains);
        
    }

}
