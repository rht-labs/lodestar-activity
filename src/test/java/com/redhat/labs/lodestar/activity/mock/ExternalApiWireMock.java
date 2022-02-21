package com.redhat.labs.lodestar.activity.mock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import java.util.HashMap;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class ExternalApiWireMock implements QuarkusTestResourceLifecycleManager {

    private WireMockServer wireMockServer; 
    @Override
    public Map<String, String> start() {
        wireMockServer = new WireMockServer();
        wireMockServer.start();
        
        String body = ResourceLoader.load("seed-activity.json");
        
        stubFor(get(urlPathMatching("/api/v4/projects/([0-9]*)/repository/commits")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));
        
        body = ResourceLoader.load("seed-engagement-v2.json");
        
        stubFor(get(urlEqualTo("/api/v2/engagements")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));

        body = ResourceLoader.load("seed-engagement.json"); //Get all engagements v2

        stubFor(get(urlEqualTo("/api/v1/engagements/projects")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
        )); //Get all engagements v2
        
        body = ResourceLoader.load("hook-commit-1.json");
        
        stubFor(get(urlEqualTo("/api/v4/projects/1/repository/commits/1?stats=false")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));
        
        body = ResourceLoader.load("hook-commit-2.json");
        
        stubFor(get(urlEqualTo("/api/v4/projects/1/repository/commits/2?stats=false")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));
        
        body = ResourceLoader.load("engagement.json");

        //v2 get engagement
        stubFor(get(urlEqualTo("/api/v2/engagements/1")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));

        stubFor(get(urlEqualTo("/api/v2/engagements/project/1")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
        ));

        //v1 get engagement
        stubFor(get(urlEqualTo("/api/v1/engagements/namespace/main%2Fstore%2FHats%2FCap%2Fiac?includeStatus=false")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
        ));


        
        Map<String, String> config = new HashMap<>();
        config.put("gitlab.api/mp-rest/url", wireMockServer.baseUrl());
        config.put("engagement.api/mp-rest/url", wireMockServer.baseUrl());
        //config.put("git-api-v1.api/mp-rest/url", wireMockServer.baseUrl());
        
        return config;
    }

    @Override
    public void stop() {
        if(null != wireMockServer) {
           wireMockServer.stop();
        }
        
    }


}
