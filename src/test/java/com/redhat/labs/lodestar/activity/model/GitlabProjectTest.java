package com.redhat.labs.lodestar.activity.model;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class GitlabProjectTest {

    static String nameWithNamespace = "a / b / c / customer / project / iac";

    @Test
    void testEngagementName() {
        GitlabProject p = GitlabProject.builder().nameWithNamespace(nameWithNamespace).build();
        assertEquals("project", p.getEngagementNameFromName());
    }

    @Test
    void testCustomerName() {
        GitlabProject p = GitlabProject.builder().nameWithNamespace(nameWithNamespace).build();
        assertEquals("customer", p.getCustomerNameFromName());
    }
}
