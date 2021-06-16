package com.redhat.labs.lodestar.activity.rest.client;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.lodestar.activity.model.Engagement;

@RegisterRestClient(configKey = "engagement.api")
@Produces("application/json")
public interface EngagementApiRestClient {

    @GET
    @Path("/api/v1/engagements")
    List<Engagement> getAllEngagements(@QueryParam("includeCommits") boolean includeCommits, @QueryParam("includeStatus") boolean includeStatus, @QueryParam("pagination") boolean pagination);
    
    @GET
    @Path("/api/v1/engagements/customer/{customer}/{engagement}")
    Engagement getEngagement(@PathParam("customer") String customer, @PathParam("engagement") String engagement, @QueryParam("includeStatus") boolean includeStatus);
}
