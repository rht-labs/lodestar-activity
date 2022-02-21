package com.redhat.labs.lodestar.activity.service;

import com.redhat.labs.lodestar.activity.model.Engagement;
import com.redhat.labs.lodestar.activity.model.GitlabProject;
import com.redhat.labs.lodestar.activity.model.Hook;
import com.redhat.labs.lodestar.activity.model.VersionManifest;
import com.redhat.labs.lodestar.activity.rest.client.EngagementApiRestClient;
import com.redhat.labs.lodestar.activity.rest.client.GitApiRestClient;
import com.redhat.labs.lodestar.activity.rest.client.LodeStarStatusRestClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.junit.mockito.InjectSpy;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


import javax.inject.Inject;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class EngagementServiceTest {

    @Inject
    EngagementService service;

    @Test
    void testGetEngagement() {

        GitlabProject p = GitlabProject.builder().pathWithNamespace("iac").build();
        Engagement e = service.getEngagement(Hook.builder().project(p).projectId(1L).build());

        assertNotNull(e);
    }

    @Test
    void testGetAllEngagement() {

        List<Engagement> e = service.getAllEngagements();
        assertEquals(1, e.size());
    }
}
