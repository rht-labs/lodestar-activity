package com.redhat.labs.lodestar.activity.service;

import com.redhat.labs.lodestar.activity.model.Activity;
import com.redhat.labs.lodestar.activity.model.Engagement;
import com.redhat.labs.lodestar.activity.model.Hook;
import com.redhat.labs.lodestar.activity.model.pagination.PagedResults;
import com.redhat.labs.lodestar.activity.rest.client.GitlabRestClient;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.Sort.Direction;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.mutiny.core.eventbus.EventBus;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    ActivityRepository activityRepository;

    @Inject
    EngagementService engagementService;

    @Inject
    EventBus bus;

    /**
     * If there is no data in the activity db go get it all.
     *
     */
    @Scheduled(every = "5m")
    void checkDBPopulated() {
        long count = activityRepository.count();
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
        for (Activity commit : hook.getCommits()) {
            Optional<Activity> entity = activityRepository.find("id", commit.getId()).firstResultOptional();
            if (entity.isEmpty()) {
                LOGGER.debug("Pid {} Commit {}", hook.getProjectId(), commit.getId());
                var fullCommit = gitlabRestClient.getCommit(hook.getProjectId(), commit.getId(), false);
                if(commit.didFileChange(committedFilesToWatch) && filterCommit(fullCommit)) {
                    LOGGER.debug("Activity for {} {}", hook.getCustomerName(), hook.getEngagementName());
                    var engagement = engagementService.getEngagement(hook);
                    fullCommit.setEngagementUuid(engagement.getUuid());
                    fullCommit.setProjectId(hook.getProjectId());
                    fullCommit.setRegion(engagement.getRegion());
                    activityRepository.persist(fullCommit);
                }
            }
        }

    }
    
    @Transactional
    public long purge(Hook hook) {
        LOGGER.info("Purging engagement {}", hook.getProjectId());
        return activityRepository.delete("projectId", hook.getProjectId());
    }

    @Transactional
    public long purge() {
        LOGGER.info("Purging activity db");
        return activityRepository.deleteAll();
    }

    public void refresh() {

        List<Engagement> engagements = engagementService.getAllEngagements();

        LOGGER.debug("Engagement count {}", engagements.size());

        engagements.parallelStream().forEach(ev -> bus.publish("refresh.engagement.event", ev));
    }

    @ConsumeEvent(value = "refresh.engagement.event", blocking = true)
    @Transactional
    public void reloadEngagement(Engagement e) {
        LOGGER.debug("Reloading {}", e);
        List<Activity> fullCommit = getCommitLog(String.valueOf(e.getProjectId()));

        for (Activity commit : fullCommit) {
            LOGGER.trace("c {}", commit);
            commit.setProjectId(e.getProjectId());
            commit.setEngagementUuid(e.getUuid());
            commit.setRegion(e.getRegion());
        }

        long deletedRows = activityRepository.delete(engagementUuid, e.getUuid());
        LOGGER.debug("Deleted {} rows for engagement {}. Adding {}", deletedRows, e.getUuid(), fullCommit.size());
        activityRepository.persist(fullCommit);

    }

    public List<RecentCommit> getMostRecentlyUpdateEngagements() {
        return activityRepository.findMostRecentlyUpdatedEngagements();
    }

    public List<String> getMostRecentlyUpdateEngagements(int page, int pageSize) {
        return activityRepository.findMostRecentlyUpdatedEngagements(page, pageSize);
    }

    public List<String> getMostRecentlyUpdateEngagements(int page, int pageSize, Set<String> regions) {
        return activityRepository.findMostRecentlyUpdatedEngagementsRegion(page, pageSize, regions);
    }

    public List<Activity> getActivityByUuid(String uuid) {
        return activityRepository.list(engagementUuid, Sort.by(committedDate, Direction.Descending), uuid);
    }

    public Activity getLastActivity(String uuid) {
        List<Activity> activity = getActivityByUuid(uuid);
        if(activity.isEmpty()) {
            throw new WebApplicationException(404);
        }

        return activity.get(0);
    }

    public long getTotalActivityByUuid(String uuid) {
        return activityRepository.count(engagementUuid, uuid);
    }

    public List<Activity> getPagedActivityByUuid(String uuid, int page, int pageSize) {
        return activityRepository.find(engagementUuid, Sort.by(committedDate, Direction.Descending), uuid)
                .page(Page.of(page, pageSize)).list();
    }

    public List<Activity> getAll(int page, int pageSize) {
        return activityRepository.findAll(Sort.by(committedDate, Direction.Descending).and("id")).page(Page.of(page, pageSize))
                .list();
    }

    public long getActivityCount() {
        return activityRepository.count();
    }

    private List<Activity> getCommitLog(String projectPathOrId) {
        PagedResults<Activity> page = new PagedResults<>(commitPageSize);

        while (page.hasMore()) {
            try {
                var response = gitlabRestClient.getCommitLog(projectPathOrId, commitPageSize, page.getNumber());
                page.update(response, new GenericType<>() {
                });
            } catch (WebApplicationException ex) {
                page.end();
                LOGGER.error("Error retrieving engagement / project for {} page {} Message {}. Was the engagement deleted? Check / refresh engagement service",
                        projectPathOrId, page.getNumber(), ex.getMessage());
            }
        }

        LOGGER.debug("total commits for project {} {}", projectPathOrId, page.size());

        return page.getResults().stream().filter(this::filterCommit).collect(Collectors.toList());
    }

    /**
     * This method will alter the commit message when the message starts with a
     * value in the commit filter message list
     * 
     * @param commit the commit
     * @return true if author email is not in the email filter list nor does the filter
     *         message matches an item in the message filter list
     */
    private boolean filterCommit(Activity commit) {
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
