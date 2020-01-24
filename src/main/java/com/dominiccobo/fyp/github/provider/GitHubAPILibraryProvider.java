package com.dominiccobo.fyp.github.provider;

import com.dominiccobo.fyp.github.utils.GitRepoDetails;
import com.google.common.collect.Streams;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.stream.Stream;

@Component
class GitHubAPILibraryProvider implements GitHubAPI {

    private static final Logger LOG = LoggerFactory.getLogger(GitHubAPILibraryProvider.class);
    public static final int DEFAULT_PAGE_SIZE = 50;

    private final GitHub gitHubApi;

    @Autowired
    public GitHubAPILibraryProvider(GitHub gitHubApi) {
        this.gitHubApi = gitHubApi;
    }

    @Override
    public Stream<Issue> getIssuesForRepository(GitRepoDetails remoteRepoDetails) {
        return fetchIssuesForRemoteRepository(remoteRepoDetails)
            .map(GitHubIssue::new);
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

    private Stream<GHIssue> fetchIssuesForRemoteRepository(GitRepoDetails gitRepoDetails) {
        GHRepository repo = getGitHubRepository(gitRepoDetails);
        if (repo != null) {
            return getGitHubIssuesForRepo(repo);
        }
        else {
            LOG.error("Could not return issues for null repository.");
        }
        return Stream.empty();
    }

    private Stream<GHIssue> getGitHubIssuesForRepo(GHRepository repo)  {
        return Streams.stream(repo.listIssues(GHIssueState.ALL).withPageSize(DEFAULT_PAGE_SIZE).iterator());
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
