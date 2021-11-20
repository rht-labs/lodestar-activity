package com.redhat.labs.lodestar.activity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionManifest {

    public String name;
    public String value;
}
