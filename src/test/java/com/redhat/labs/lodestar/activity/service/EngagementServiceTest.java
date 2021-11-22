package com.redhat.labs.lodestar.activity.service;

import com.redhat.labs.lodestar.activity.model.Engagement;
import com.redhat.labs.lodestar.activity.model.GitlabProject;
import com.redhat.labs.lodestar.activity.model.Hook;
import com.redhat.labs.lodestar.activity.model.VersionManifest;
import com.redhat.labs.lodestar.activity.rest.client.GitApiRestClient;
import com.redhat.labs.lodestar.activity.rest.client.LodeStarStatusRestClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class EngagementServiceTest {

    EngagementService service;
    LodeStarStatusRestClient statusRestClient;
    GitApiRestClient gitApiRestClient;

    @BeforeEach
    public void setUp() {
        statusRestClient = Mockito.mock(LodeStarStatusRestClient.class);
        gitApiRestClient = Mockito.mock(GitApiRestClient.class);
        service = new EngagementService();
        service.gitApiRestClient = gitApiRestClient;
        service.statusRestClient = statusRestClient;
        service.component = "lodestar-engagements";
    }

    @Test
    public void testGetEngagement() {
        Mockito.when(statusRestClient.getVersionManifest("lodestar-engagements")).thenReturn(
                VersionManifest.builder().value("v1.0.0").build());

        Mockito.when(gitApiRestClient.getEngagement("iac", false)).thenReturn(Engagement.builder().build());

        GitlabProject p = GitlabProject.builder().pathWithNamespace("iac").build();
        Engagement e = service.getEngagement(Hook.builder().project(p).build());

        assertNotNull(e);
        Mockito.verify(gitApiRestClient).getEngagement("iac", false);
    }

    @Test
    public void testGetAllEngagement() {
        Mockito.when(statusRestClient.getVersionManifest("lodestar-engagements")).thenReturn(
                VersionManifest.builder().value("v1.0.0").build());

        Mockito.when(gitApiRestClient.getAllEngagements()).thenReturn(Collections.singletonList(Engagement.builder().build()));

        List<Engagement> e = service.getAllEngagements();
        Mockito.verify(gitApiRestClient).getAllEngagements();
        assertEquals(1, e.size());
    }

    @Test
    public void testVersionsV1() {
        Mockito.when(statusRestClient.getVersionManifest("lodestar-engagements")).thenReturn(
                VersionManifest.builder().value("v1.0.0").build());

        service.getVersion();
        assertTrue(service.useV1);
    }

    @Test
    public void testVersionsV2() {
        Mockito.when(statusRestClient.getVersionManifest("lodestar-engagements")).thenReturn(
                VersionManifest.builder().value("v2.0.0").build());

        service.getVersion();
        assertFalse(service.useV1);
    }

    @Test
    public void testVersionsMain() {
        Mockito.when(statusRestClient.getVersionManifest("lodestar-engagements")).thenReturn(
                VersionManifest.builder().value("main").build());

        service.getVersion();
        assertFalse(service.useV1);
    }

    public void testVersionsNull() {
        Mockito.when(statusRestClient.getVersionManifest("lodestar-engagements")).thenReturn(
                VersionManifest.builder().value(null).build());

        service.getVersion();
        assertFalse(service.useV1);
    }

}
