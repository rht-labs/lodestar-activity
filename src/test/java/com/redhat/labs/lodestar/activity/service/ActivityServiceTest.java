package com.redhat.labs.lodestar.activity.service;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.redhat.labs.lodestar.activity.mock.EngagementWireMock;
import com.redhat.labs.lodestar.activity.model.Commit;

import io.quarkus.panache.common.Sort;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
@QuarkusTestResource(EngagementWireMock.class)
public class ActivityServiceTest {

    @Inject
    ActivityService service;
    
    @InjectMock
    CommitRepository commitRepository;
    
//    @InjectMock
//    GitlabRestClient gitlabRestClient;
    
    @BeforeEach
    public void mockOut() {
        
        
        
    }
    
    @Test
    public void getActivityCountForUuid() {
        Mockito.when(commitRepository.count("engagementUuid", "abc")).thenReturn(4L);
        Assertions.assertEquals(4L, service.getTotalActivityByUuid("abc"));
    }
    
    
    @Test
    public void getAllCommitsForUuidSuccess() {

        List<Commit> commits = new ArrayList<>();
        commits.add(Commit.builder().id("1").engagementUuid("abc").build());
        commits.add(Commit.builder().id("2").engagementUuid("abc").build());
        commits.add(Commit.builder().id("3").engagementUuid("abc").build());
        
        //Mockito.when(commitRepository.list(Mockito.eq("engagementUuid"), Mockito.any(Sort.class), Mockito.eq("abc"))).thenReturn(commits);
        Mockito.when(commitRepository.list(Mockito.eq("engagementUuid"), Mockito.any(Sort.class), ArgumentMatchers.<String>any())).thenReturn(commits);
        
        List<Commit> activity = service.getActivityByUuid("abc");
        Assertions.assertEquals(3, activity.size());

    }
    
    
}
