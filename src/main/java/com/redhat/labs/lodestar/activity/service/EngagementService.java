package com.redhat.labs.lodestar.activity.service;

import com.redhat.labs.lodestar.activity.model.Engagement;
import com.redhat.labs.lodestar.activity.model.Hook;
import com.redhat.labs.lodestar.activity.rest.client.EngagementApiRestClient;
import com.redhat.labs.lodestar.activity.rest.client.GitApiRestClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class EngagementService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EngagementService.class);

    @ConfigProperty(name = "engagement.version.one")
    boolean useV1 = false;

    @Inject
    @RestClient
    GitApiRestClient gitApiRestClient;

    @Inject
    @RestClient
    EngagementApiRestClient engagementApiRestClient;

    public Engagement getEngagement(Hook hook) {
        LOGGER.debug("use v1? {}", useV1);
        if(useV1) {
            return gitApiRestClient.getEngagement(hook.getProject().getPathWithNamespace(), false);
        }

        return engagementApiRestClient.getEngagementByProject(hook.getProjectId());
    }

    public List<Engagement> getAllEngagements() {
        if(useV1) {
            return gitApiRestClient.getAllEngagements();
        }

        return engagementApiRestClient.getAllEngagements();
    }
}
