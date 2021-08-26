package com.redhat.labs.lodestar.activity.service;

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
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.core.GenericType;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class ActivityService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityService.class);
    
    private static final String engagementUuid = "engagementUuid";
    private static final String committedDate = "committedDate";

    @ConfigProperty(name = "commit.page.size")
    int commitPageSize;

    @ConfigProperty(name = "commit.msg.filter.list")
    List<String> commitFilteredMessages;

    @ConfigProperty(name = "commit.filter.list")
    List<String> commitFilteredEmails;
    
    @ConfigProperty(name = "commit.watch.files")
    List<String> committedFilesToWatch;

    @Inject
    @RestClient
    GitlabRestClient gitlabRestClient;

    @Inject
    @RestClient
    EngagementApiRestClient engagementRestClient;

    @Inject
    CommitRepository commitRepository;

    @Inject
    EventBus bus;

    /**
     * If there is no data in the activity db go get it all.
     * 
     * @param ev start
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
     * @param hook webhook
     */
    @Transactional
    public void addNewCommits(Hook hook) {
        for (Commit commit : hook.getCommits()) {
            Optional<Commit> entity = commitRepository.find("id", commit.getId()).firstResultOptional();
            if (entity.isEmpty()) {
                LOGGER.debug("Pid {} Commit {}", hook.getProjectId(), commit.getId());
                var fullCommit = gitlabRestClient.getCommit(hook.getProjectId(), commit.getId(), false);
                if(commit.didFileChange(committedFilesToWatch) && filterCommit(fullCommit)) {
                    var engagement = engagementRestClient.getEngagement(hook.getCustomerName(),
                            hook.getEngagementName(), false);
                    fullCommit.setEngagementUuid(engagement.getUuid());
                    fullCommit.setProjectId(hook.getProjectId());
                    commitRepository.persist(fullCommit);
                }
            }
        }

    }
    
    @Transactional
    public long purge(Hook hook) {
        LOGGER.info("Purging engagement {}", hook.getProjectId());
        
        return commitRepository.delete("projectId", hook.getProjectId());
        
    }

    @Transactional
    public long purge() {
        LOGGER.info("Purging activity db");
        return commitRepository.deleteAll();
    }

    public void refresh() {

        List<Engagement> engagements = engagementRestClient.getAllEngagements(false, false, false);

        LOGGER.debug("Engagement count {}", engagements.size());

        engagements.parallelStream().forEach(ev -> bus.publish("refresh.engagement.event", ev));
    }

    @ConsumeEvent(value = "refresh.engagement.event", blocking = true)
    @Transactional
    public void reloadEngagement(Engagement e) {
        LOGGER.debug("Reloading {}", e);
        List<Commit> fullCommit = getCommitLog(String.valueOf(e.getProjectId()));

        for (Commit commit : fullCommit) {
            LOGGER.trace("c {}", commit);
            commit.setProjectId(e.getProjectId());
            commit.setEngagementUuid(e.getUuid());
        }

        long deletedRows = commitRepository.delete(engagementUuid, e.getUuid());
        LOGGER.debug("Deleted {} rows for engagement {}", deletedRows, e.getUuid());
        commitRepository.persist(fullCommit);

    }

    public List<Commit> getActivityByUuid(String uuid) {
        return commitRepository.list(engagementUuid, Sort.by(committedDate, Direction.Descending), uuid);
    }

    public long getTotalActivityByUuid(String uuid) {
        return commitRepository.count(engagementUuid, uuid);
    }

    public List<Commit> getPagedActivityByUuid(String uuid, int page, int pageSize) {
        return commitRepository.find(engagementUuid, Sort.by(committedDate, Direction.Descending), uuid)
                .page(Page.of(page, pageSize)).list();
    }

    public List<Commit> getAll(int page, int pageSize) {
        return commitRepository.findAll(Sort.by(committedDate, Direction.Descending).and("id")).page(Page.of(page, pageSize))
                .list();
    }

    public long getActivityCount() {
        return commitRepository.count();
    }

    private List<Commit> getCommitLog(String projectPathOrId) {
        PagedResults<Commit> page = new PagedResults<>(commitPageSize);

        while (page.hasMore()) {
            var response = gitlabRestClient.getCommitLog(projectPathOrId, commitPageSize, page.getNumber());
            page.update(response, new GenericType<List<Commit>>() {
            });
        }

        LOGGER.debug("total commits for project {} {}", projectPathOrId, page.size());

        return page.getResults().stream().filter(this::filterCommit).collect(Collectors.toList());
    }

    /**
     * This method will alter the commit message when the message starts with a
     * value in the commit filter message list
     * 
     * @param commit
     * @return true if author email is not in the email filter list nor does the filter
     *         message matches an item in the message filter list
     */
    private boolean filterCommit(Commit commit) {
        if (commitFilteredEmails.contains(commit.getAuthorEmail())) {
            return false;
        }

        Optional<String> match = commitFilteredMessages.stream().filter(m -> commit.getMessage().startsWith(m))
                .findFirst();
        if (match.isPresent()) {
            String updated = commit.getMessage().replaceFirst(match.get(), "").trim();
            commit.setMessage(updated);
        }
        
        return !commit.getMessage().isBlank();
    }

}
