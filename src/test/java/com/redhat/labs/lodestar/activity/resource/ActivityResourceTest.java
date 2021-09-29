package com.redhat.labs.lodestar.activity.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.redhat.labs.lodestar.activity.mock.ResourceLoader;
import com.redhat.labs.lodestar.activity.model.Activity;
import com.redhat.labs.lodestar.activity.model.Hook;
import com.redhat.labs.lodestar.activity.service.ActivityService;
import com.redhat.labs.lodestar.activity.service.RecentCommit;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@TestHTTPEndpoint(ActivityResource.class)
class ActivityResourceTest {

    @InjectMock
    ActivityService service;

    static ObjectMapper om;

    @BeforeAll
    static void config() {

        om = new ObjectMapper();
        om.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    }

    @Test
    void testAllActivityPaged() {

        List<Activity> queryResp = new ArrayList<>();
        queryResp.add(Activity.builder().id("1").engagementUuid("abc").build());
        queryResp.add(Activity.builder().id("2").engagementUuid("abc").build());
        Mockito.when(service.getAll(0, 2)).thenReturn(queryResp);

        JsonPath path = given().queryParam("page", "0").queryParam("pageSize", 2).when().get().then().statusCode(200)
                .extract().jsonPath();

        Assertions.assertEquals(2, path.getList(".").size());
        Assertions.assertEquals("1", path.get("[0].id"));

        Assertions.assertEquals("abc", path.get("[0].engagement_uuid"));
    }

    @Test
    void getLastActivityForUuid() {

        OffsetDateTime now = OffsetDateTime.now();
        String engagementUuid = "abc";
        Activity activity = Activity.builder().id("1").engagementUuid("abc").committedDate(now).build();
        Mockito.when(service.getLastActivity("abc")).thenReturn(activity);

        given().pathParam("uuid", engagementUuid).head("{uuid}").then().statusCode(200)
                .header("last-update", equalTo(now.toInstant().toString()));
    }

    @Test
    void getLatestActivityPerEngagement() {
        List<String> activity = List.of("1", "2", "3");

        Mockito.when(service.getMostRecentlyUpdateEngagements(0, 100)).thenReturn(activity);

        given().when().get("latest").then().statusCode(200).body("size()", equalTo(3));
    }

    @Test
    void getLatestActivityPerEngagementRegion() {
        List<String> activity = List.of("1", "2", "3");

        Mockito.when(service.getMostRecentlyUpdateEngagements(0, 100, Set.of("na"))).thenReturn(activity);

        given().queryParam("regions", "na").when().get("latest").then().statusCode(200).body("size()", equalTo(3));
    }

    @Test
    void getLatestWithLastUpdateTimestamp() {
        OffsetDateTime time = OffsetDateTime.parse("2021-09-20T01:38:03Z");
        List<RecentCommit> recent = new ArrayList<>();
        recent.add(new RecentCommit("euuid1", time));
        recent.add(new RecentCommit("euuid2", time.minusDays(1)));
        recent.add(new RecentCommit("euuid3", time.minusDays(2)));

        Mockito.when(service.getMostRecentlyUpdateEngagements()).thenReturn(recent);
        given().when().get("latestWithTimestamp").then().statusCode(200).body("size()", equalTo(3))
                .body(equalTo("{\"euuid1\":\"2021-09-20T01:38:03Z\",\"euuid2\":\"2021-09-19T01:38:03Z\",\"euuid3\":\"2021-09-18T01:38:03Z\"}"));
    }

    @Test
    void testEngagementActivity() {
        when().get("/uuid/abc").then().statusCode(200);

        given().queryParam("pageSize", "10").queryParam("page", "-1").when().get("/uuid/abc/").then().statusCode(200);
        given().queryParam("pageSize", "10").queryParam("page", "2").when().get("/uuid/abc/").then().statusCode(200);
    }

    @Test
    void testRefresh() {
        given().contentType(ContentType.JSON).when().put("refresh").then().statusCode(202);
    }

    @Test
    void testHookNoAuth() {
        given().body("{\"customer_name\":\"jello\"}").contentType(ContentType.JSON).when().post("hook").then()
                .statusCode(401);
    }

    @Test
    void testHook() {
        String body = ResourceLoader.load("hook-push.json");
        
        given().body(body).contentType(ContentType.JSON).header("x-gitlab-token", "t").when().post("hook").then()
                .statusCode(202);
    }

    @Test
    void testHookNoChanges() {
        Hook hook = Hook.builder().commits(Collections.singletonList(new Activity())).build();
        given().body(hook).contentType(ContentType.JSON).header("x-gitlab-token", "t").when().post("hook").then()
                .statusCode(200);
    }

    @Test
    void testHookProjectDeleted() throws JsonProcessingException {
        Hook hook = Hook.builder().groupId("1").projectId(1L).eventName("project_deleted")
                .commits(Collections.singletonList(new Activity())).build();

        given().body(om.writeValueAsString(hook)).contentType(ContentType.JSON).header("x-gitlab-token", "t").when()
                .post("hook").then().statusCode(204);
    }

}
