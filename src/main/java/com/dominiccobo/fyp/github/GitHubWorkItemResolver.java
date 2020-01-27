package com.dominiccobo.fyp.github;

import com.dominiccobo.fyp.context.api.events.GitRemoteURLRecognisedEvent;
import com.dominiccobo.fyp.context.api.queries.AssociatedWorkItemsQuery;
import com.dominiccobo.fyp.context.listeners.WorkItemQueryListener;
import com.dominiccobo.fyp.context.models.Pagination;
import com.dominiccobo.fyp.context.models.QueryContext;
import com.dominiccobo.fyp.context.models.WorkItem;
import com.dominiccobo.fyp.context.models.git.GitContext;
import com.dominiccobo.fyp.context.models.git.GitRemoteIdentifier;
import com.dominiccobo.fyp.context.models.git.GitRemoteURL;
import com.dominiccobo.fyp.github.provider.GitHubAPI;
import com.dominiccobo.fyp.github.provider.Issue;
import com.dominiccobo.fyp.github.utils.GitRepoDetails;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class GitHubWorkItemResolver implements WorkItemQueryListener {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubWorkItemResolver.class);
    private final GitHubAPI gitHubAPI;

    @Autowired
    public GitHubWorkItemResolver(GitHubAPI gitHubAPI) {
        this.gitHubAPI = gitHubAPI;
    }

    @EventHandler
    public void on(GitRemoteURLRecognisedEvent event) throws ExecutionException {
        GitRepoDetails repoDetails = GitRepoDetails.from(event.getGitRemoteURL().getUrl());
        if(repoDetails != null) {
            LOG.info("No caching implemented. We have received {}... ", event.getGitRemoteURL().getUrl());
        }
    }

    @Override
    @QueryHandler
    public List<WorkItem> on(AssociatedWorkItemsQuery query) {
        LOG.info("Received query for associated work items");
        QueryContext queryContext = query.getContext();
        Pagination pagination = query.getPagination();

        return fetchWorkItemsFromGitHub(queryContext, pagination)
                .collect(Collectors.toList());
    }

    /**
     * cached, avoid direct calling
     * @param key
     * @param pagination
     * @return
     */
    private Stream<WorkItem> fetchWorkItemsForRemote(GitRemoteURL key, Pagination pagination) {
        return fetchGitHubIssuesForRemote(key, pagination).map(this::transformIssueToWorkItem);
    }



    private Stream<WorkItem> fetchWorkItemsFromGitHub(QueryContext qryCtx, Pagination pagination) {
        if(qryCtx.getGitContext().isPresent()) {
            return fetchWorkItemsFromGitContext(qryCtx, pagination);
        }
        return Stream.of();
    }

    private Stream<WorkItem> fetchWorkItemsFromGitContext(QueryContext qryCtx, Pagination pagination) {
        return this.fetchWorkItemsForAllRemotes(qryCtx.getGitContext().get(), pagination);
    }

    private WorkItem transformIssueToWorkItem(Issue issue) {
        WorkItem workItemToAdd = new WorkItem();
        workItemToAdd.setTitle(issue.getTitle());
        workItemToAdd.setBody(issue.getBody());
        return workItemToAdd;
    }

    private Stream<WorkItem> fetchWorkItemsForAllRemotes(GitContext ctx, Pagination pagination) {
        if (ctx.getRemotes().isPresent()) {
            Map<GitRemoteIdentifier, GitRemoteURL> remotes = ctx.getRemotes().get();

            return remotes.values().stream().flatMap(gitRemoteURL -> getRemoteFromCache(gitRemoteURL, pagination));
        }
        return Stream.empty();
    }


    private Stream<Issue> fetchGitHubIssuesForRemote(GitRemoteURL remote, Pagination pagination) {
        String remoteUrl = remote.getUrl();
        GitRepoDetails gitRepoDetails = GitRepoDetails.from(remoteUrl);
        if (gitRepoDetails == null) {
            return Stream.empty();
        }
        LOG.info("Fetching any issues associated with {} ({}:{})", remoteUrl, gitRepoDetails.getRepositoryName(), gitRepoDetails.getUserOrOrganisation());
        return gitHubAPI.getIssuesForRepository(gitRepoDetails, pagination);
    }


    private Stream<? extends WorkItem> getRemoteFromCache(GitRemoteURL gitRemoteURL, Pagination pagination) {
        return this.fetchWorkItemsForRemote(gitRemoteURL, pagination);
    }
}
