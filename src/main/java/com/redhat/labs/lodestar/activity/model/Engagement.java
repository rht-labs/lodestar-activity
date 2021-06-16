package com.redhat.labs.lodestar.activity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Engagement {

    private String uuid;
    private long projectId;
    
}
