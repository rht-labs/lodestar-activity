package com.redhat.labs.lodestar.activity.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.redhat.labs.lodestar.activity.mock.ResourceLoader;
import com.redhat.labs.lodestar.activity.model.Commit;
import com.redhat.labs.lodestar.activity.model.Hook;
import com.redhat.labs.lodestar.activity.service.ActivityService;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;

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

        List<Commit> queryResp = new ArrayList<>();
        queryResp.add(Commit.builder().id("1").engagementUuid("abc").build());
        queryResp.add(Commit.builder().id("2").engagementUuid("abc").build());
        Mockito.when(service.getAll(0, 2)).thenReturn(queryResp);

        JsonPath path = given().queryParam("page", "0").queryParam("pageSize", 2).when().get().then().statusCode(200)
                .extract().jsonPath();

        Assertions.assertEquals(2, path.getList(".").size());
        Assertions.assertEquals("1", path.get("[0].id"));

        Assertions.assertEquals("abc", path.get("[0].engagement_uuid"));
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
    void testHook() throws JsonProcessingException {
        String body = ResourceLoader.load("hook-push.json");
        
        given().body(body).contentType(ContentType.JSON).header("x-gitlab-token", "t").when().post("hook").then()
                .statusCode(202);
    }

    @Test
    void testHookNoChanges() {
        Hook hook = Hook.builder().commits(Collections.singletonList(new Commit())).build();
        given().body(hook).contentType(ContentType.JSON).header("x-gitlab-token", "t").when().post("hook").then()
                .statusCode(200);
    }

    @Test
    void testHookProjectDeleted() throws JsonProcessingException {
        Hook hook = Hook.builder().groupId("1").projectId(1L).eventName("project_deleted")
                .commits(Collections.singletonList(new Commit())).build();

        given().body(om.writeValueAsString(hook)).contentType(ContentType.JSON).header("x-gitlab-token", "t").when()
                .post("hook").then().statusCode(204);
    }

}
