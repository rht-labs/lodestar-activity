package com.redhat.labs.lodestar.activity.rest.client;

import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
//import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.labs.lodestar.activity.model.Commit;


//import com.redhat.labs.lodestar.exception.mapper.GitLabServiceResponseMapper;

//@Retry(maxRetries = 5, delay = 2000)
@Path("/api/v4")
@RegisterRestClient(configKey = "gitlab.api")
//@RegisterProvider(value = GitLabServiceResponseMapper.class, priority = 50)
@RegisterClientHeaders(GitlabTokenFactory.class)
@Produces("application/json")
public interface GitlabRestClient {
    
    @GET
    @Path("/projects/{id}/repository/commits")
    @Produces("application/json")
    Response getCommitLog(@PathParam("id") @Encoded String projectPathOrId, @QueryParam("per_page") int perPage,
            @QueryParam("page") int page);
    
    @GET
    @Path("/projects/{id}/repository/commits/{sha}")
    Commit getCommit(@PathParam("id") long projectPathOrId, @PathParam("sha") String sha, @QueryParam("stats") boolean stats);

}
