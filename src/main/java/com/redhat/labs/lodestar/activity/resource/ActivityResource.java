package com.redhat.labs.lodestar.activity.resource;

import java.util.List;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.activity.model.Commit;
import com.redhat.labs.lodestar.activity.model.Hook;
import com.redhat.labs.lodestar.activity.service.ActivityService;

@Path("/api/activity")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ActivityResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityResource.class);

    @Inject
    ActivityService activityService;

    @ConfigProperty(name = "gitlab.webhook.token")
    String webhookToken;
    
    @ConfigProperty(name = "commit.watch.files")
    List<String> commitedFilesToWatch;

    @GET
    @Path("/uuid/{uuid}")
    public Response getCommitsForEngagement(@PathParam(value = "uuid") String uuid, @QueryParam("page") int page, @QueryParam("pageSize") int pageSize) {
        List<Commit> activity;
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

    @GET
    public Response getAllActivity(@QueryParam("page") int page, @QueryParam("pageSize") int pageSize) {

        if (pageSize < 1 || page < 0) {
            return Response.status(Status.BAD_REQUEST).entity("Invalid Page Request").build();
        }

        List<Commit> activity = activityService.getAll(page, pageSize);
        long totalActivity = activityService.getActivityCount();

        return Response.ok(activity).header("x-page", page).header("x-per-page", pageSize)
                .header("x-total-activity", totalActivity).header("x-total-pages", (totalActivity / pageSize) + 1)
                .build();
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
        
        if(hook.didFileChange(commitedFilesToWatch)) {
            LOGGER.debug("Activity spotted for {}", hook.getEngagementName());
            activityService.addNewCommits(hook);
            return Response.accepted().build();
        }

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
