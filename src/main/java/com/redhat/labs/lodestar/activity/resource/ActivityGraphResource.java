package com.redhat.labs.lodestar.activity.resource;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

import com.redhat.labs.lodestar.activity.model.Commit;
import com.redhat.labs.lodestar.activity.service.ActivityService;

@GraphQLApi
public class ActivityGraphResource {
    
    @Inject
    ActivityService activityService;
    
    @Query("totalCommits")
    @Description("a count of all commits")
    public long getCommitCount() {
        return activityService.getActivityCount();
    }
    
    @Query("allCommits")
    @Description("Get all commits from all projects")
    public List<Commit> getAllActivity(@Name("page") int page, @Name("pageSize") int pageSize) {
        return activityService.getAll(page, pageSize);
    }

    @Query("totalCommitsForUuid")
    @Description("a count of all commits for uuid")
    public long getCommitCountForUuid(@Name("uuid") String engagementUuid) {
        return activityService.getTotalActivityByUuid(engagementUuid);
    }
    
    @Query
    @Description("Get a list of activity from a single project by uuid")
    public List<Commit> getActivityForEngagement(@Name("uuid") String engagementUuid) {
        return activityService.getActivityByUuid(engagementUuid);
    }

}
