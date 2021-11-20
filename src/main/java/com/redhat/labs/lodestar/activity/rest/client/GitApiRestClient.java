package com.redhat.labs.lodestar.activity.rest.client;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.lodestar.activity.model.Engagement;

import javax.ws.rs.*;

@RegisterRestClient(configKey = "git-api-v1.api")
@Produces("application/json")
@Consumes("application/json")
@Path("/api/v1/engagements")
public interface GitApiRestClient {

    @GET
    @Path("/projects")
    List<Engagement> getAllEngagements();

    @GET
    @Path("/namespace/{namespace}")
    Engagement getEngagement(@Encoded @PathParam("namespace") String namespace, @QueryParam("includeStatus") boolean includeStatus);
}
