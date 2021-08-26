package com.redhat.labs.lodestar.activity.service;

import com.redhat.labs.lodestar.activity.mock.ExternalApiWireMock;
import com.redhat.labs.lodestar.activity.model.Commit;
import com.redhat.labs.lodestar.activity.model.Engagement;
import com.redhat.labs.lodestar.activity.model.GitlabProject;
import com.redhat.labs.lodestar.activity.model.Hook;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@QuarkusTest
@QuarkusTestResource(ExternalApiWireMock.class)
class ActivityServiceTest {

    @Inject
    ActivityService service;

    @BeforeEach
    void init() {
        Engagement e = Engagement.builder().uuid("cb570945-a209-40ba-9e42-63a7993baf4d").projectId(13065).build();
        service.reloadEngagement(e);
    }

    @Test
    void testPurge() {

        long deletes = service.purge();

        Assertions.assertEquals(3, deletes);
    }
    
    @Test
    void testPurgeProject() {
        Hook hook = Hook.builder().projectId(13065L).build();
        long deletes = service.purge(hook);
        Assertions.assertEquals(3, deletes);
    }

    @Test
    void testActivityCount() {

        long activity = service.getActivityCount();

        Assertions.assertEquals(3L, activity);
    }

    @Test
    void testHook() {

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
    void testActivityCountForUuid() {
        Assertions.assertEquals(3, service.getTotalActivityByUuid("cb570945-a209-40ba-9e42-63a7993baf4d"));
    }

    @Test
    void testPagedActivity() {
        Assertions.assertEquals(2, service.getPagedActivityByUuid("cb570945-a209-40ba-9e42-63a7993baf4d", 0, 2).size());
    }

    @Test
    void testAllCommitsForUuidSuccess() {

        List<Commit> activity = service.getActivityByUuid("cb570945-a209-40ba-9e42-63a7993baf4d");
        Assertions.assertEquals(3, activity.size());
    }

    @Test
    void testGetAllPaged() {
        long activity = service.getActivityCount();
        Assertions.assertEquals(3L, activity);

        //0 based index
        Assertions.assertEquals(1, service.getAll(1, 2).size());
    }

}
