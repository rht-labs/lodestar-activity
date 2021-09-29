package com.redhat.labs.lodestar.activity.rest.client;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.MultivaluedMap;

@ApplicationScoped
public class GitlabTokenFactory implements ClientHeadersFactory {
    
    @ConfigProperty(name = "gitlab.personal.access.token")
    String gitLabToken;

    @Override
    public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {
        
        MultivaluedMap<String, String> result = new MultivaluedMapImpl<>();
        result.add("Private-Token", gitLabToken);

        return result;
    }
}
