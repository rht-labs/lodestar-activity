package com.redhat.labs.lodestar.activity.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.labs.lodestar.activity.mock.ExternalApiWireMock;
import com.redhat.labs.lodestar.activity.model.Commit;
import com.redhat.labs.lodestar.activity.model.GitlabProject;
import com.redhat.labs.lodestar.activity.model.Hook;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
@QuarkusTestResource(ExternalApiWireMock.class)
public class ActivityServiceTest {

    @Inject
    ActivityService service;

    @BeforeEach
    public void init() {
        service.refresh();
    }

    @Test
    public void testPurge() {

        long deletes = service.purge();

        Assertions.assertEquals(3, deletes);
    }
    
    @Test
    public void testPurgeProject() {
        Hook hook = Hook.builder().projectId(13065L).build();
        long deletes = service.purge(hook);
        Assertions.assertEquals(3, deletes);
    }

    @Test
    public void testActivityCount() {

        long activity = service.getActivityCount();

        Assertions.assertEquals(3L, activity);
    }

    @Test
    public void testHook() {

        List<Commit> queryResp = new ArrayList<>();
        GitlabProject glp = GitlabProject.builder().pathWithNamespace("main/store/Hats/Cap/iac").build();
        queryResp.add(Commit.builder().id("1").projectId(1L).engagementUuid("abc").build());
        queryResp.add(Commit.builder().id("2").modified(Collections.singletonList("engagement.json")).projectId(1L)
                .engagementUuid("abc").build());

        Hook hook = Hook.builder().projectId(1L).project(glp).commits(queryResp).build();
        service.addNewCommits(hook);

        Assertions.assertEquals(4, service.getActivityCount());
    }

    @Test
    public void testActivityCountForUuid() {
        Assertions.assertEquals(3, service.getTotalActivityByUuid("cb570945-a209-40ba-9e42-63a7993baf4d"));
    }

    @Test
    public void testPagedActivity() {
        Assertions.assertEquals(2, service.getPagedActivityByUuid("cb570945-a209-40ba-9e42-63a7993baf4d", 0, 2).size());
    }

    @Test
    public void testAllCommitsForUuidSuccess() {

        List<Commit> activity = service.getActivityByUuid("cb570945-a209-40ba-9e42-63a7993baf4d");
        Assertions.assertEquals(3, activity.size());

    }

    @Test
    public void testGetAllPaged() {
        Assertions.assertEquals(1, service.getAll(1, 2).size());
    }

}
