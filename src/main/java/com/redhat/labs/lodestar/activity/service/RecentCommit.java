package com.redhat.labs.lodestar.activity.service;

import java.time.OffsetDateTime;

public class RecentCommit {
    public String engagementUuid;
    public OffsetDateTime committedDate;

    public RecentCommit(String engagementUuid, OffsetDateTime committedDate) {
        this.engagementUuid = engagementUuid;
        this.committedDate = committedDate;
    }
}
