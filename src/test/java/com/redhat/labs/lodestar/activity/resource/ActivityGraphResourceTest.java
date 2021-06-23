package com.redhat.labs.lodestar.activity.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.activity.mock.ResourceLoader;
import com.redhat.labs.lodestar.activity.service.ActivityService;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
class ActivityGraphResourceTest {

    @InjectMock
    ActivityService service;

    @Test
    void testTotalCommits() {
        Mockito.when(service.getActivityCount()).thenReturn(3L);

        String body = ResourceLoader.load("graphql-activity.json");
        given().contentType(ContentType.JSON).body(body).when().post("/graphql").then().statusCode(200).assertThat()
                .body("data.totalCommits", equalTo(3));
    }
}
