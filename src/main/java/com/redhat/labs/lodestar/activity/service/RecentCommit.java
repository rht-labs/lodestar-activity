package com.redhat.labs.lodestar.activity.service;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.OffsetDateTime;

@Getter
@EqualsAndHashCode
@ToString
public class RecentCommit {
    public String engagementUuid;
    public OffsetDateTime committedDate;

    public RecentCommit(String engagementUuid, OffsetDateTime committedDate) {
        this.engagementUuid = engagementUuid;
        this.committedDate = committedDate;
    }

}
