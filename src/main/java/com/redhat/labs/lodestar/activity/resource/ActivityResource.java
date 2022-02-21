package com.redhat.labs.lodestar.activity.resource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.redhat.labs.lodestar.activity.service.RecentCommit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.activity.model.Activity;
import com.redhat.labs.lodestar.activity.model.Hook;
import com.redhat.labs.lodestar.activity.service.ActivityService;

@Path("/api/activity")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ActivityResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityResource.class);
    private static final String ACCESS_CONTROL_EXPOSE_HEADER = "Access-Control-Expose-Headers";
    private static final String LAST_UPDATE_HEADER = "last-update";

    @Inject
    ActivityService activityService;

    @ConfigProperty(name = "gitlab.webhook.token")
    String webhookToken;
    
    @ConfigProperty(name = "commit.watch.files")
    List<String> committedFilesToWatch;

    @PostConstruct
    void trimTokenAndWatch() {
        webhookToken = webhookToken.trim();
        LOGGER.info("Watched files {}", committedFilesToWatch);
    }

    @GET
    @Path("/uuid/{uuid}")
    public Response getCommitsForEngagement(@PathParam(value = "uuid") String uuid, @QueryParam("page") int page, @QueryParam("pageSize") int pageSize) {
        List<Activity> activity;
        Response response;
        
        if (pageSize < 1 || page < 0) {
            activity = activityService.getActivityByUuid(uuid);
            response = Response.ok(activity).build();
        } else {
            long totalActivity = activityService.getTotalActivityByUuid(uuid);
            activity = activityService.getPagedActivityByUuid(uuid, page, pageSize);
            response = Response.ok(activity).header("x-page", page).header("x-per-page", pageSize)
            .header("x-total-activity", totalActivity).header("x-total-pages", (totalActivity / pageSize) + 1)
            .build();
        }
        
        return response;
    }

    @HEAD
    @Path("{uuid}")
    public Response getLastUpdate(@PathParam("uuid") String uuid) {
        Activity activity = activityService.getLastActivity(uuid);

        return Response.ok().header(LAST_UPDATE_HEADER, activity.getCommittedDate().toInstant())
                .header(ACCESS_CONTROL_EXPOSE_HEADER, LAST_UPDATE_HEADER).build();
    }

    @GET
    public Response getAllActivity(@DefaultValue("0") @QueryParam("page") int page, @DefaultValue("100") @QueryParam("pageSize") int pageSize) {

        List<Activity> activity = activityService.getAll(page, pageSize);
        long totalActivity = activityService.getActivityCount();

        return Response.ok(activity).header("x-page", page).header("x-per-page", pageSize)
                .header("x-total-activity", totalActivity).header("x-total-pages", (totalActivity / pageSize) + 1)
                .build();
    }

    @GET
    @Path("latest")
    public Response getActivityPerEngagement(@DefaultValue("0") @QueryParam("page") int page,
                                             @DefaultValue("100") @QueryParam("pageSize") int pageSize,
                                             @QueryParam("regions") Set<String> regions) {
        if(regions.isEmpty()) {
            return Response.ok(activityService.getMostRecentlyUpdateEngagements(page, pageSize)).build();
        }

        return Response.ok(activityService.getMostRecentlyUpdateEngagements(page, pageSize, regions)).build();
    }

    @GET
    @Path("latestWithTimestamp")
    @Operation(summary = "Gets a map of engagement id and the last activity date")
    public Map<String, OffsetDateTime> getActivityPerEngagement() {
        return activityService.getMostRecentlyUpdateEngagements().stream().collect(Collectors.toMap(
                RecentCommit::getEngagementUuid, RecentCommit::getCommittedDate
        ));
    }

    @POST
    @PermitAll
    @Path("/hook")
    @APIResponses(value = { @APIResponse(responseCode = "401", description = "Invalid Gitlab Token"),
            @APIResponse(responseCode = "200", description = "Returns the hook given.") })
    @Operation(summary = "Entry point for update notifications")
    public Response postWebhook(@HeaderParam(value = "x-gitlab-token") String gitLabToken, Hook hook) {

        if (!webhookToken.equals(gitLabToken)) {
            LOGGER.error("Invalid gitlab token used");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        LOGGER.debug("Hook for {}", hook);
        
        if(hook.wasProjectDeleted()) {
            LOGGER.debug("Hook Project Deleted {}", hook.getProjectId());
            activityService.purge(hook);
            return Response.status(Status.NO_CONTENT).build();
        }

        if(hook.didFileChange(committedFilesToWatch)) {
            LOGGER.debug("Activity spotted for {}", hook.getEngagementName());
            activityService.addNewCommits(hook);
            return Response.accepted().build();
        }

        LOGGER.debug("hook file change? {}", hook.didFileChange(committedFilesToWatch));
        return Response.ok(hook).build();
    }

    @PUT
    @Path("/refresh")
    @APIResponses(value = { @APIResponse(responseCode = "202", description = "The request was accepted and will be processed.") })
    @Operation(summary = "Refreshes database with data in git, purging first")
    public Response refreshActivity() {

        activityService.purge();
        activityService.refresh();

        return Response.accepted().build();
    }

}
