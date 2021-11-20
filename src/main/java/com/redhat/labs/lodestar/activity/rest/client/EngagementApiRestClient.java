package com.redhat.labs.lodestar.activity.rest.client;

import java.util.List;

import javax.ws.rs.*;

import org.apache.http.*;
import org.eclipse.microprofile.faulttolerance.*;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.lodestar.activity.model.Engagement;

@Retry(maxRetries = 5, delay = 1200, retryOn = NoHttpResponseException.class, abortOn = WebApplicationException.class)
@RegisterRestClient(configKey = "engagement.api")
@RegisterProvider(value = RestClientResponseMapper.class, priority = 50)
@Produces("application/json")
@Consumes("application/json")
@Path("/api/v2/engagements")
public interface EngagementApiRestClient {

    @GET
    List<Engagement> getAllEngagements(@QueryParam("includeCommits") boolean includeCommits, @QueryParam("includeStatus") boolean includeStatus, @QueryParam("pagination") boolean pagination);
    
    @GET
    @Path("/{id}")
    Engagement getEngagement(@PathParam("id") long projectId);
}
