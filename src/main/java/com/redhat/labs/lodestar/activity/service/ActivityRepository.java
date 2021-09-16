package com.redhat.labs.lodestar.activity.service;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.labs.lodestar.activity.model.Activity;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

import java.time.OffsetDateTime;
import java.util.*;

@ApplicationScoped
public class ActivityRepository implements PanacheRepository<Activity> {

    private static final String LAST_PER_ENGAGEMENT = "SELECT NEW com.redhat.labs.lodestar.activity.service.RecentCommit( " +
            "engagementUuid, MAX(committedDate) as committedDate) " +
            "FROM Activity Group by engagementUuid ORDER BY committedDate desc";

    private static final String LAST_PER_ENGAGEMENT_IN_REGION = "SELECT NEW com.redhat.labs.lodestar.activity.service.RecentCommit( " +
            "engagementUuid, MAX(committedDate) as committedDate) " +
            "FROM Activity WHERE region in (:region) Group by engagementUuid ORDER BY committedDate desc";

    /**
     *
     * @param page page number
     * @param pageSize page size
     * @return A list of engagement uuids that were most recently updated in order
     */

    @SuppressWarnings("unchecked")
    public List<String> findMostRecentlyUpdatedEngagements(int page, int pageSize) {
        List<String> results = new ArrayList<>();
        getEntityManager().createQuery(LAST_PER_ENGAGEMENT, RecentCommit.class)
                .setFirstResult(page).setMaxResults(pageSize).getResultStream().forEach(r -> results.add(r.engagementUuid));

        return results;
    }

    @SuppressWarnings("unchecked")
    public List<String> findMostRecentlyUpdatedEngagementsRegion(int page, int pageSize, Set<String> regions) {
        List<String> results = new ArrayList<>();
        getEntityManager().createQuery(LAST_PER_ENGAGEMENT_IN_REGION, RecentCommit.class).setParameter("region", regions)
                .setFirstResult(page).setMaxResults(pageSize).getResultStream().forEach(r -> results.add(r.engagementUuid));

        return results;
    }
}
