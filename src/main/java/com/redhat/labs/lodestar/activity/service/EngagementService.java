package com.redhat.labs.lodestar.activity.service;

import com.redhat.labs.lodestar.activity.model.Engagement;
import com.redhat.labs.lodestar.activity.model.Hook;
import com.redhat.labs.lodestar.activity.model.VersionManifest;
import com.redhat.labs.lodestar.activity.rest.client.EngagementApiRestClient;
import com.redhat.labs.lodestar.activity.rest.client.GitApiRestClient;
import com.redhat.labs.lodestar.activity.rest.client.LodeStarStatusRestClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class EngagementService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EngagementService.class);

    @ConfigProperty(name = "engagement.component")
    String component;

    boolean useV1 = true;

    @Inject
    @RestClient
    GitApiRestClient gitApiRestClient;

    @Inject
    @RestClient
    EngagementApiRestClient engagementApiRestClient;

    @Inject
    @RestClient
    LodeStarStatusRestClient statusRestClient;

    @PostConstruct
    void getVersion() {
        LOGGER.debug("component {}", component);
        VersionManifest vm = statusRestClient.getVersionManifest(component);

        if(vm.value == null || !vm.value.startsWith("v")) {
            useV1 = false; //Use v2 if not in an env with a true version set
        } else {
            String[] version = vm.value.split("\\.");
            int majorVersion = Integer.parseInt(version[0].substring(1));
            LOGGER.debug("major version {} full version {}", majorVersion, vm.value);
            useV1 = majorVersion < 2;
        }

        LOGGER.debug("using v1 {}", useV1);
    }

    public Engagement getEngagement(Hook hook) {
        LOGGER.debug("use {}", useV1);
        if(useV1) {
            return gitApiRestClient.getEngagement(hook.getProject().getPathWithNamespace(), false);
        }

        return engagementApiRestClient.getEngagement(hook.getProjectId());
    }

    public List<Engagement> getAllEngagements() {
        if(useV1) {
            return gitApiRestClient.getAllEngagements();
        }

        return engagementApiRestClient.getAllEngagements(false, false, false);
    }
}
