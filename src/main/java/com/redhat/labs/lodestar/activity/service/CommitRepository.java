package com.redhat.labs.lodestar.activity.service;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.labs.lodestar.activity.model.Commit;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class CommitRepository implements PanacheRepository<Commit> {

}
