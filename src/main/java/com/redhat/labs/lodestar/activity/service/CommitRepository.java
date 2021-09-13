package com.redhat.labs.lodestar.activity.service;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.labs.lodestar.activity.model.Commit;

import io.quarkus.hibernate.orm.panache.PanacheRepository;

import java.util.*;

@ApplicationScoped
public class CommitRepository implements PanacheRepository<Commit> {

    private static final String LAST_PER_ENGAGEMENT = "SELECT act1.* FROM Commit act1 JOIN " +
            "(SELECT engagementUuid, MAX(committedDate) AS committedDate FROM Commit act2 GROUP BY engagementUuid) AS act2 " +
            "ON act1.engagementUuid = act2.engagementUuid AND act1.committedDate = act2.committedDate " +
            "ORDER BY act1.committedDate desc";

    /**
     *
     * @param page page number
     * @param pageSize page size
     * @return A list of commits where each commit is the latest per engagement (i.e. only 1 per engagement)
     */
    @SuppressWarnings("unchecked")
    public List<Commit> findRecentPerEngagement(int page, int pageSize) {
        return getEntityManager().createNativeQuery(LAST_PER_ENGAGEMENT, Commit.class)
                .setFirstResult(page).setMaxResults(pageSize).getResultList();
    }
}
