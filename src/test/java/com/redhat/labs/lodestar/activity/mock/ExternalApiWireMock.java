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
        
        body = ResourceLoader.load("seed-engagement.json");
        
        stubFor(get(urlEqualTo("/api/v2/engagements?includeCommits=false&includeStatus=false&pagination=false")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));
        
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
        
        stubFor(get(urlEqualTo("/api/v2/engagements/customer/Hats/Cap?includeStatus=false")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody(body)
                ));
        
        Map<String, String> config = new HashMap<>();
        config.put("gitlab.api/mp-rest/url", wireMockServer.baseUrl());
        config.put("engagement.api/mp-rest/url", wireMockServer.baseUrl());
        
        return config;
    }

    @Override
    public void stop() {
        if(null != wireMockServer) {
           wireMockServer.stop();
        }
        
    }


}
