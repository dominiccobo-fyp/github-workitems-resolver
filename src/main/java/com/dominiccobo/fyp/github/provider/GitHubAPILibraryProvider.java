package com.dominiccobo.fyp.github.provider;

import com.dominiccobo.fyp.github.utils.GitRepoDetails;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
class GitHubAPILibraryProvider implements GitHubAPI {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubAPILibraryProvider.class);

    private final GitHub gitHubApi;

    @Autowired
    public GitHubAPILibraryProvider(GitHub gitHubApi) {
        this.gitHubApi = gitHubApi;
    }

    @Override
    public List<Issue> getIssuesForRepository(GitRepoDetails remoteRepoDetails) {
        ArrayList<GHIssue> ghIssues = fetchIssuesForRemoteRepository(remoteRepoDetails);
        return ghIssues
                .stream()
                .map(GitHubIssue::new)
                .collect(Collectors.toList());
    }

    private static class GitHubIssue implements Issue{

        private final GHIssue issue;

        GitHubIssue(GHIssue issue) {
            this.issue = issue;
        }

        @Override
        public String getTitle() {
            return issue.getTitle();
        }

        @Override
        public String getBody() {
            return issue.getBody();
        }
    }

    private ArrayList<GHIssue> fetchIssuesForRemoteRepository(GitRepoDetails gitRepoDetails) {
        ArrayList<GHIssue> issuesForThisRemote = new ArrayList<>();
        GHRepository repo = getGitHubRepository(gitRepoDetails);
        if (repo != null) {
            issuesForThisRemote.addAll(getGitHubIssuesForRepo(repo));
        }
        else {
            LOG.error("Could not return issues for null repository.");
        }
        return issuesForThisRemote;
    }

    private List<GHIssue> getGitHubIssuesForRepo(GHRepository repo)  {
        List<GHIssue> issues = new ArrayList<>();
        try {
            List<GHIssue> fetchedIssues = repo.getIssues(GHIssueState.ALL);
            issues.addAll(fetchedIssues);
        } catch (IOException e) {
            LOG.error("Could not fetch issues from repository.", e);
        }
        return issues;
    }

    private GHRepository getGitHubRepository(GitRepoDetails gitRepoDetails) {
        GHRepository repository = getGitHubUserRepo(gitRepoDetails);
        if (repository == null) {
            repository = fetchGitHubOrganisationRepo(gitRepoDetails);
        }
        return repository;
    }

    private GHRepository getGitHubUserRepo(GitRepoDetails repo) {
        LOG.debug("Fetching user {}", repo.getUserOrOrganisation());
        GHRepository repository = null;
        try {
            repository = gitHubApi
                    .getUser(repo.getUserOrOrganisation())
                    .getRepository(repo.getRepositoryName());

        } catch (IOException ignored) {
            LOG.debug("User {} does not have repo {}", repo.getUserOrOrganisation(), repo.getRepositoryName());
        }
        return repository;
    }


    private GHRepository fetchGitHubOrganisationRepo(GitRepoDetails repo) {
        LOG.debug("Fetching organisation {}", repo.getUserOrOrganisation());
        GHRepository repository;
        try {
            repository = gitHubApi
                    .getOrganization(repo.getUserOrOrganisation())
                    .getRepository(repo.getRepositoryName());

        } catch (IOException ex) {
            LOG.debug("Organisation {} does not have repo {}", repo.getUserOrOrganisation(), repo.getRepositoryName());
            return null;
        }
        return repository;
    }
}
