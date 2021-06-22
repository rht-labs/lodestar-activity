package com.redhat.labs.lodestar.activity.resource;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.activity.model.Commit;
import com.redhat.labs.lodestar.activity.model.Hook;
import com.redhat.labs.lodestar.activity.service.ActivityService;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapper;
import io.restassured.path.json.JsonPath;


@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestHTTPEndpoint(ActivityResource.class)
public class ActivityResourceTest {
    
    @InjectMock
    ActivityService service;
    
    static ObjectMapper om;
    
    @Test
    public void testActivityForEngagement() {
        
    }

    @Test
    public void testAllActivityPaged() {
        
        List<Commit> queryResp = new ArrayList<>();
        queryResp.add(Commit.builder().id("1").engagementUuid("abc").build());
        queryResp.add(Commit.builder().id("2").engagementUuid("abc").build());
        Mockito.when(service.getAll(0, 2)).thenReturn(queryResp);
        
        JsonPath path =  given().queryParam("page", "0").queryParam("pageSize", 2)
            .when().get().then().statusCode(200).extract().jsonPath();

        Assertions.assertEquals(2, path.getList(".").size());
        Assertions.assertEquals("1", path.get("[0].id"));
        
        Assertions.assertEquals("abc", path.get("[0].engagement_uuid"));
    }
    
    @Test
    public void testAllActivityNoPage() {
        when().get().then().statusCode(400);
        
        given().queryParam("pageSize", 1).queryParam("page", "-1").when().get().then().statusCode(400);
    }
    
    @Test void testEngagementActivity() {
        when().get("/uuid/abc").then().statusCode(200);
        
        
        given().queryParam("pageSize", "10").queryParam("page", "-1").when().get("/uuid/abc/").then().statusCode(200);
        given().queryParam("pageSize", "10").queryParam("page", "2").when().get("/uuid/abc/").then().statusCode(200);
    }
    
    @Test
    public void testRefresh() {
        given().contentType(ContentType.JSON).when().put("refresh").then().statusCode(202);
    }
    
    @Test
    public void testHookNoAuth() {        
        given().body("{\"customer_name\":\"jello\"}").contentType(ContentType.JSON).when().post("hook").then().statusCode(401);
    }
    
    @Test
    public void testHook() {
        Hook hook = Hook.builder().build();
        given().body(hook).contentType(ContentType.JSON).header("x-gitlab-token", "t").when().post("hook").then().statusCode(200);
    }
    
}
