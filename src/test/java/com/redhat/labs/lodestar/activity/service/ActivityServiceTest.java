package com.redhat.labs.lodestar.activity.service;

import com.redhat.labs.lodestar.activity.mock.ExternalApiWireMock;
import com.redhat.labs.lodestar.activity.model.Activity;
import com.redhat.labs.lodestar.activity.model.Engagement;
import com.redhat.labs.lodestar.activity.model.GitlabProject;
import com.redhat.labs.lodestar.activity.model.Hook;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.WebApplicationException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(ExternalApiWireMock.class)
class ActivityServiceTest {

    @Inject
    ActivityService service;

    @Inject
    ActivityRepository repo;

    @BeforeEach
    void init() {
        service.purge();
        Engagement e = Engagement.builder().uuid("cb570945-a209-40ba-9e42-63a7993baf4d").projectId(13065).build();
        service.reloadEngagement(e);
    }

    @Test
    void testPurge() {

        long deletes = service.purge();

        assertEquals(3, deletes);
    }
    
    @Test
    void testPurgeProject() {
        Hook hook = Hook.builder().projectId(13065L).build();
        long deletes = service.purge(hook);
        assertEquals(3, deletes);
    }

    @Test
    void testActivityCount() {

        long activity = service.getActivityCount();

        assertEquals(3L, activity);
    }

    @Test
    void testHook() {

        List<Activity> queryResp = new ArrayList<>();
        GitlabProject glp = GitlabProject.builder().pathWithNamespace("main/store/Hats/Cap/iac").build();
        queryResp.add(Activity.builder().id("1").projectId(1L).engagementUuid("abc").build());
        queryResp.add(Activity.builder().id("2").modified(Collections.singletonList("engagement.json")).projectId(1L)
                .engagementUuid("abc").build());

        Hook hook = Hook.builder().projectId(1L).project(glp).commits(queryResp).build();
        service.addNewCommits(hook);

        assertEquals(4, service.getActivityCount());
    }

    @Test
    void testActivityCountForUuid() {
        assertEquals(3, service.getTotalActivityByUuid("cb570945-a209-40ba-9e42-63a7993baf4d"));
    }

    @Test
    void testPagedActivity() {
        assertEquals(2, service.getPagedActivityByUuid("cb570945-a209-40ba-9e42-63a7993baf4d", 0, 2).size());
    }

    @Test
    void testLastActivity() {
        Activity activity = service.getLastActivity("cb570945-a209-40ba-9e42-63a7993baf4d");

        assertNotNull(activity);
        assertEquals("2020-04-01T17:42:42-07:00", activity.getCommittedDate().toString());
    }

    @Test
    void testLastActivityNoData() {
         WebApplicationException ex = assertThrows(WebApplicationException.class, () -> service.getLastActivity("blah"));
         assertEquals(404, ex.getResponse().getStatus());
    }

    @Test
    void testAllCommitsForUuidSuccess() {

        List<Activity> activity = service.getActivityByUuid("cb570945-a209-40ba-9e42-63a7993baf4d");
        assertEquals(3, activity.size());
    }

    @Test
    @Transactional
    void testMostRecentPerEngagement() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Activity> activity = new ArrayList<>(8);
        activity.add(Activity.builder().id("id1").committedDate(now.minus(1, ChronoUnit.DAYS)).engagementUuid("e1").build());
        activity.add(Activity.builder().id("id2").committedDate(now.minus(2, ChronoUnit.DAYS)).engagementUuid("e1").build());
        activity.add(Activity.builder().id("id3").committedDate(now.minus(3, ChronoUnit.DAYS)).engagementUuid("e2").build());
        activity.add(Activity.builder().id("id4").committedDate(now.minus(4, ChronoUnit.DAYS)).engagementUuid("e2").build());
        activity.add(Activity.builder().id("id5").committedDate(now.minus(5, ChronoUnit.DAYS)).engagementUuid("e3").build());
        activity.add(Activity.builder().id("id6").committedDate(now.minus(6, ChronoUnit.DAYS)).engagementUuid("e3").build());
        activity.add(Activity.builder().id("id7").committedDate(now.minus(7, ChronoUnit.DAYS)).engagementUuid("e3").build());
        activity.add(Activity.builder().id("id8").committedDate(now.minus(8, ChronoUnit.DAYS)).engagementUuid("e3").build());
        repo.persist(activity);

        activity.forEach(System.out::println);

        List<String> result = service.getMostRecentlyUpdateEngagements(0, 100);
        assertEquals(4, result.size());
        assertEquals("id1", activity.get(0).getId());
        assertEquals("id2", activity.get(1).getId());
        assertEquals("id3", activity.get(2).getId());
    }

    @Test
    @Transactional
    void testMostRecentPerEngagementRegion() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Activity> activity = new ArrayList<>(8);
        activity.add(Activity.builder().id("id1").region("emea").committedDate(now.minus(1, ChronoUnit.DAYS)).engagementUuid("e1").build());
        activity.add(Activity.builder().id("id2").region("emea").committedDate(now.minus(2, ChronoUnit.DAYS)).engagementUuid("e1").build());
        activity.add(Activity.builder().id("id3").region("emea").committedDate(now.minus(3, ChronoUnit.DAYS)).engagementUuid("e2").build());
        activity.add(Activity.builder().id("id4").region("emea").committedDate(now.minus(4, ChronoUnit.DAYS)).engagementUuid("e2").build());
        activity.add(Activity.builder().id("id5").region("apac").committedDate(now.minus(5, ChronoUnit.DAYS)).engagementUuid("e3").build());
        activity.add(Activity.builder().id("id6").region("apac").committedDate(now.minus(6, ChronoUnit.DAYS)).engagementUuid("e3").build());
        activity.add(Activity.builder().id("id7").region("apac").committedDate(now.minus(7, ChronoUnit.DAYS)).engagementUuid("e3").build());
        activity.add(Activity.builder().id("id8").region("apac").committedDate(now.minus(8, ChronoUnit.DAYS)).engagementUuid("e3").build());
        repo.persist(activity);

        List<String> result = service.getMostRecentlyUpdateEngagements(0, 100, Set.of("emea", "apac"));
        assertEquals(3, result.size());
        assertEquals("e1", result.get(0));
        assertEquals("e2", result.get(1));
        assertEquals("e3", result.get(2));

        result = service.getMostRecentlyUpdateEngagements(0, 100, Set.of("apac"));
        assertEquals(1, result.size());
        assertEquals("e3", result.get(0));

        result = service.getMostRecentlyUpdateEngagements(0, 100, Set.of("emea"));
        assertEquals(2, result.size());
        assertEquals("e1", result.get(0));
        assertEquals("e2", result.get(1));
    }

    @Test
    void testGetAllPaged() {
        long activity = service.getActivityCount();
        assertEquals(3L, activity);

        //0 based index
        assertEquals(1, service.getAll(1, 2).size());
    }

}
