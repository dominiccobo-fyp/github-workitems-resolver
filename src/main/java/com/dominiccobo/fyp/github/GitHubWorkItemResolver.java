package com.dominiccobo.fyp.github;

import com.dominiccobo.fyp.context.api.events.GitRemoteURLRecognisedEvent;
import com.dominiccobo.fyp.context.api.queries.AssociatedWorkItemsQuery;
import com.dominiccobo.fyp.context.listeners.WorkItemQueryListener;
import com.dominiccobo.fyp.context.models.*;
import com.dominiccobo.fyp.context.models.git.*;
import com.dominiccobo.fyp.github.provider.GitHubAPI;
import com.dominiccobo.fyp.github.provider.Issue;
import com.dominiccobo.fyp.github.utils.GitRepoDetails;
import com.google.common.cache.*;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

@Component
public class GitHubWorkItemResolver implements WorkItemQueryListener {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubWorkItemResolver.class);
    private final GitHubAPI gitHubAPI;
    private final LoadingCache<GitRemoteURL, Stream<WorkItem>> workItemsCache;

    @Autowired
    public GitHubWorkItemResolver(GitHubAPI gitHubAPI) {
        this.gitHubAPI = gitHubAPI;
        this.workItemsCache = newCacheInstance();
    }

    private LoadingCache<GitRemoteURL, Stream<WorkItem>> newCacheInstance() {
        return CacheBuilder.newBuilder()
                .maximumSize(2000)
                .expireAfterAccess(1, TimeUnit.HOURS)
                .refreshAfterWrite(30, TimeUnit.MINUTES)
                .build(getCacheLoader());
    }

    private CacheLoader<GitRemoteURL, Stream<WorkItem>> getCacheLoader() {
        return new CacheLoader<GitRemoteURL, Stream<WorkItem>>() {
            @Override
            public Stream<WorkItem> load(GitRemoteURL key) {
                return fetchWorkItemsForRemote(key);
            }
        };
    }

    @EventHandler
    public void on(GitRemoteURLRecognisedEvent event) throws ExecutionException {
        GitRepoDetails repoDetails = GitRepoDetails.from(event.getGitRemoteURL().getUrl());
        if(repoDetails != null) {
            this.workItemsCache.get(event.getGitRemoteURL());
        }
    }

    @Override
    @QueryHandler
    public List<WorkItem> on(AssociatedWorkItemsQuery query) {
        LOG.info("Received query for associated work items");
        QueryContext queryContext = query.getContext();
        Pagination pagination = query.getPagination();

        return fetchWorkItemsFromGitHub(queryContext)
                .limit(pagination.itemsPerPage)
                .skip(pagination.page * pagination.itemsPerPage)
                .collect(Collectors.toList());
    }

    /**
     * cached, avoid direct calling
     * @param key
     * @return
     */
    private Stream<WorkItem> fetchWorkItemsForRemote(GitRemoteURL key) {
        return fetchGitHubIssuesForRemote(key).map(this::transformIssueToWorkItem);
    }



    private Stream<WorkItem> fetchWorkItemsFromGitHub(QueryContext qryCtx) {
        if(qryCtx.getGitContext().isPresent()) {
            return fetchWorkItemsFromGitContext(qryCtx);
        }
        return Stream.of();
    }

    private Stream<WorkItem> fetchWorkItemsFromGitContext(QueryContext qryCtx) {
        return this.fetchWorkItemsForAllRemotes(qryCtx.getGitContext().get());
    }

    private WorkItem transformIssueToWorkItem(Issue issue) {
        WorkItem workItemToAdd = new WorkItem();
        workItemToAdd.setTitle(issue.getTitle());
        workItemToAdd.setBody(issue.getBody());
        return workItemToAdd;
    }

    private Stream<WorkItem> fetchWorkItemsForAllRemotes(GitContext ctx) {
        if (ctx.getRemotes().isPresent()) {
            Map<GitRemoteIdentifier, GitRemoteURL> remotes = ctx.getRemotes().get();

            return remotes.values().stream().flatMap(this::getRemoteFromCache);
        }
        return Stream.empty();
    }


    private Stream<Issue> fetchGitHubIssuesForRemote(GitRemoteURL remote) {
        String remoteUrl = remote.getUrl();
        GitRepoDetails gitRepoDetails = GitRepoDetails.from(remoteUrl);
        if (gitRepoDetails == null) {
            return Stream.empty();
        }
        LOG.info("Fetching any issues associated with {} ({}:{})", remoteUrl, gitRepoDetails.getRepositoryName(), gitRepoDetails.getUserOrOrganisation());
        return gitHubAPI.getIssuesForRepository(gitRepoDetails);
    }


    private Stream<? extends WorkItem> getRemoteFromCache(GitRemoteURL gitRemoteURL) {
        try {
            return workItemsCache.get(gitRemoteURL);
        } catch (ExecutionException e) {
            LOG.error("Error retrieving remote {} through cache. Returning empty stream. {}", gitRemoteURL.getUrl(), e);
            return Stream.empty();
        }
    }
}
