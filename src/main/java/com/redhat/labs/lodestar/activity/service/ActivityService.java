package com.redhat.labs.lodestar.activity.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.labs.lodestar.activity.model.Commit;
import com.redhat.labs.lodestar.activity.model.Engagement;
import com.redhat.labs.lodestar.activity.model.Hook;
import com.redhat.labs.lodestar.activity.model.pagination.PagedResults;
import com.redhat.labs.lodestar.activity.rest.client.EngagementApiRestClient;
import com.redhat.labs.lodestar.activity.rest.client.GitlabRestClient;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.Sort.Direction;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class ActivityService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityService.class);

    @ConfigProperty(name = "commit.page.size")
    int commitPageSize;

    @ConfigProperty(name = "commit.msg.filter.list")
    List<String> commitFilteredMessages;

    @ConfigProperty(name = "commit.filter.list")
    List<String> commitFilteredEmails;

    @Inject
    @RestClient
    GitlabRestClient gitlabRestClient;

    @Inject
    @RestClient
    EngagementApiRestClient engagementRestClient;

    @Inject
    CommitRepository commitRepository;

    /**
     * If there is no data in the activity db go get it all.
     * 
     * @param ev
     */
    void onStart(@Observes StartupEvent ev) {
        long count = commitRepository.count();
        LOGGER.debug("There are {} commits in the activity db.", count);

        if (count == 0) {
            refresh();
        }
    }

    /**
     * Service implementation for webhook updates to gitlab. Takes all listed
     * commits and adds them to the db if the commit is not already there (does not
     * do updates - if update is needed refresh the engagement).
     * 
     * @param hook
     */
    @Transactional
    public void addNewCommits(Hook hook) {
        for (Commit commit : hook.getCommits()) {
            Optional<Commit> entity = commitRepository.find("id", commit.getId()).firstResultOptional();
            if (entity.isEmpty()) {
                LOGGER.debug("Pid {} Commit {}", hook.getProjectId(), commit.getId());
                Commit fullCommit = gitlabRestClient.getCommit(hook.getProjectId(), commit.getId(), false); // TODO 404?
                Engagement engagement = engagementRestClient.getEngagement(hook.getCustomerName(),
                        hook.getEngagementName(), false);
                fullCommit.setEngagementUuid(engagement.getUuid());
                fullCommit.setProjectId(hook.getProjectId());
                commitRepository.persist(fullCommit);
            }
        }

    }

    @Transactional
    public long purge() {
        LOGGER.info("Purging db");
        return commitRepository.deleteAll();
    }

    /**
     * Refreshes entire database. TODO this only deletes at the engagement level
     * (via reload) - not entire db.
     */
    @Transactional
    public void refresh() {

        List<Engagement> engagements = engagementRestClient.getAllEngagements(false, false, false);

        LOGGER.debug("Engagement count {}", engagements.size());

        engagements.parallelStream().forEach(e -> reloadEngagement(e));
    }

    @Transactional
    public void reloadEngagement(Engagement e) {
        LOGGER.debug("Reloading {}", e);

        List<Commit> fullCommit = getCommitLog(String.valueOf(e.getProjectId()));

        for (Commit commit : fullCommit) {
            LOGGER.trace("c {}", commit);
            commit.setProjectId(e.getProjectId());
            commit.setEngagementUuid(e.getUuid());
        }

        long deletedRows = commitRepository.delete("engagementUuid", e.getUuid());
        LOGGER.debug("Deleted {} rows for engagement {}", deletedRows, e.getUuid());
        commitRepository.persist(fullCommit);

    }

    public List<Commit> getActivityByUuid(String uuid) {
        return commitRepository.list("engagementUuid", Sort.by("committedDate", Direction.Descending), uuid);
    }
    
    public long getTotalActivityByUuid(String uuid) {
        return commitRepository.count("engagementUuid", uuid);
    }
    
    public List<Commit> getPagedActivityByUuid(String uuid, int page, int pageSize) {
        return commitRepository.find("engagementUuid", Sort.by("committedDate", Direction.Descending), uuid).page(Page.of(page, pageSize)).list();
    }

    public List<Commit> getAll(int page, int pageSize) {
        return commitRepository.findAll(Sort.by("committedDate", Direction.Descending)).page(Page.of(page, pageSize))
                .list();
    }

    public long getActivityCount() {
        return commitRepository.count();
    }

    private List<Commit> getCommitLog(String projectPathOrId) {
        PagedResults<Commit> page = new PagedResults<>(commitPageSize);

        while (page.hasMore()) {
            Response response = gitlabRestClient.getCommitLog(projectPathOrId, commitPageSize, page.getNumber());
            page.update(response, new GenericType<List<Commit>>() {
            });
        }

        LOGGER.debug("total commits for project {} {}", projectPathOrId, page.size());

        return page.getResults().stream().filter(e -> !commitFilteredEmails.contains(e.getAuthorEmail())).map(e -> {

            Optional<String> match = commitFilteredMessages.stream().filter(m -> e.getMessage().startsWith(m))
                    .findFirst();
            if (match.isPresent()) {
                String updated = e.getMessage().replaceFirst(match.get(), "").trim();
                e.setMessage(updated);
            }
            return e;

        }).filter(e -> !e.getMessage().isBlank()).collect(Collectors.toList());
    }

}
