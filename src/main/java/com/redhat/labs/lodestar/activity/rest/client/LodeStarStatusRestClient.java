package com.redhat.labs.lodestar.activity.rest.client;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.*;

import com.redhat.labs.lodestar.activity.model.VersionManifest;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;


@ApplicationScoped
@RegisterRestClient(configKey = "lodestar.status.api")

@Produces("application/json")
@Consumes("application/json")
public interface LodeStarStatusRestClient {

    @GET
    @Produces("application/json")
    @Path("/api/v1/version/manifest/{component}")
    VersionManifest getVersionManifest(@PathParam("component") String component);

}