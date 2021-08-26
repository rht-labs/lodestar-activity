package com.redhat.labs.lodestar.activity.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

public class CommitTest {
    
    @Test
    void testWatchedFiles() {
        List<String> expected = new ArrayList<>();
        Collections.addAll(expected, "engagement.json", "artifacts.json", "participants.json");
        List<String> none = new ArrayList<>();
        Collections.addAll(none, "file1", "file2", "file3", "file4");
        
        
        List<String> modified = new ArrayList<>();
        Collections.addAll(modified, "engagement.json");
        
        Commit nothing = Commit.builder().build();
        assertFalse(nothing.didFileChange(expected));
        
        nothing.setRemoved(none);
        nothing.setAdded(none);
        nothing.setModified(none);
        
        assertFalse(nothing.didFileChange(expected));
        
        List<String> one = new ArrayList<>();
        one.add("participants.json");
        
        Commit something = Commit.builder().added(one).build();
        
        assertTrue(something.didFileChange(expected));
        
        one.clear();
        one.add("artifacts.json");
        something.setAdded(Collections.emptyList());
        something.setRemoved(one);
        
        assertTrue(something.didFileChange(expected));
        
        one.clear();
        one.add("engagement.json");
        something.setRemoved(Collections.emptyList());
        something.setModified(one);
        
        assertTrue(something.didFileChange(expected));
        
    }
}
