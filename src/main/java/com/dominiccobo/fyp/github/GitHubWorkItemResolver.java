package com.dominiccobo.fyp.github;

import com.dominiccobo.fyp.context.api.queries.AssociatedWorkItemsQuery;
import com.dominiccobo.fyp.context.listeners.WorkItemQueryListener;
import com.dominiccobo.fyp.context.models.QueryContext;
import com.dominiccobo.fyp.context.models.WorkItem;
import com.dominiccobo.fyp.context.models.git.GitContext;
import com.dominiccobo.fyp.context.models.git.GitRemoteIdentifier;
import com.dominiccobo.fyp.context.models.git.GitRemoteURL;
import com.dominiccobo.fyp.github.provider.GitHubAPI;
import com.dominiccobo.fyp.github.provider.Issue;
import com.dominiccobo.fyp.github.utils.GitRepoDetails;
import org.axonframework.queryhandling.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GitHubWorkItemResolver implements WorkItemQueryListener {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubWorkItemResolver.class);
    private final GitHubAPI gitHubAPI;

    @Autowired
    public GitHubWorkItemResolver(GitHubAPI gitHubAPI) {
        this.gitHubAPI = gitHubAPI;
    }

    @QueryHandler
    @Override
    public List<WorkItem> on(AssociatedWorkItemsQuery query) {
        LOG.info("Received query for associated work items");
        QueryContext queryContext = query.getContext();
        return fetchWorkItemsFromGitHub(queryContext);
    }



    private List<WorkItem> fetchWorkItemsFromGitHub(QueryContext qryCtx) {
        if(qryCtx.getGitContext().isPresent()) {
            List<Issue> ghIssues = this.fetchGitHubIssuesForGitContext(qryCtx.getGitContext().get());
            return transformIssuesToWorkItems(ghIssues);
        }
        return new ArrayList<>();
    }


    private List<WorkItem> transformIssuesToWorkItems(List<Issue> issues) {
        List<WorkItem> formattedWorkItems = new ArrayList<>();
        for (Issue issue : issues) {
            formattedWorkItems.add(transformIssueToWorkItem(issue));
        }
        return formattedWorkItems;
    }

    private WorkItem transformIssueToWorkItem(Issue issue) {
        WorkItem workItemToAdd = new WorkItem();
        workItemToAdd.setTitle(issue.getTitle());
        workItemToAdd.setBody(issue.getBody());
        return workItemToAdd;
    }

    private List<Issue> fetchGitHubIssuesForGitContext(GitContext ctx) {
        List<Issue> retrievedIssues = new ArrayList<>();
        if (ctx.getRemotes().isPresent()) {
            Map<GitRemoteIdentifier, GitRemoteURL> remotes = ctx.getRemotes().get();
            for (Map.Entry<GitRemoteIdentifier, GitRemoteURL> remote : remotes.entrySet()) {
                retrievedIssues.addAll(fetchGitHubIssuesForRemote(remote));
            }
        }
        return retrievedIssues;
    }


    private List<Issue> fetchGitHubIssuesForRemote(Map.Entry<GitRemoteIdentifier, GitRemoteURL> remote) {
        ArrayList<Issue> issuesForThisRemote = new ArrayList<>();
        String remoteUrl = remote.getValue().getUrl();
        GitRepoDetails gitRepoDetails = GitRepoDetails.from(remoteUrl);
        if (gitRepoDetails == null) {
            return new ArrayList<>();
        }
        LOG.info("Fetching any issues associated with {} ({}:{})", remoteUrl, gitRepoDetails.getRepositoryName(), gitRepoDetails.getUserOrOrganisation());
        issuesForThisRemote.addAll(gitHubAPI.getIssuesForRepository(gitRepoDetails));
        return issuesForThisRemote;
    }


}
