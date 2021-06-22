package com.redhat.labs.lodestar.activity.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hook {

    private String objectKind;
    private String eventName;
    private Long projectId;
    private List<Commit> commits;
    private GitlabProject project;
    private String groupId;

    public boolean didFileChange(String fileName) {
        for (Commit commit : commits) {
            if (commit.didFileChange(fileName)) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    public String getCustomerName() {
        return project.getCustomerNameFromName();

    }

    @JsonIgnore
    public String getEngagementName() {
        return project.getEngagementNameFromName();
    }

    @JsonIgnore
    public boolean wasProjectDeleted() {
        return "project_deleted".equals(eventName);
    }

    @JsonIgnore
    public boolean containsAnyMessage(List<String> messages) {
        return messages.stream().anyMatch(message -> commits.stream()
                .anyMatch(c -> null != c.getMessage() && c.getMessage().startsWith(message)));
    }

}
