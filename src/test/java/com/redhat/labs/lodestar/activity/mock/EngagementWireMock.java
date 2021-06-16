package com.redhat.labs.lodestar.activity.mock;

import java.util.Collections;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class EngagementWireMock implements QuarkusTestResourceLifecycleManager {

    private WireMockServer wireMoockServer; 
    @Override
    public Map<String, String> start() {
        wireMoockServer = new WireMockServer();
        wireMoockServer.start();
        
        stubFor(get(urlEqualTo("/api/v1/engagements?includeCommits=false&includeStatus=false&pagination=false")).willReturn(aResponse()
                .withHeader("Content-Type",  "application/json")
                .withBody("[]")
                
                ));
                
        
        return Collections.singletonMap("engagement.api/mp-rest/url", wireMoockServer.baseUrl());
    }

    @Override
    public void stop() {
        if(null != wireMoockServer) {
           wireMoockServer.stop();
        }
        
    }

}
