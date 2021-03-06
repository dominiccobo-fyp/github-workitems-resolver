package com.dominiccobo.fyp.github.provider;

import com.dominiccobo.fyp.context.models.Pagination;
import com.dominiccobo.fyp.github.utils.GitRepoDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Stream;

@Component
class GitHubAPILibraryProvider implements GitHubAPI {

    HashMap<String, ArrayList<GitIssue>> notReallyACacheButYeh = new HashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(GitHubAPILibraryProvider.class);

    private final RestTemplate restTemplate;
    @Autowired
    public GitHubAPILibraryProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public Stream<Issue> getIssuesForRepository(GitRepoDetails remoteRepoDetails, Pagination pagination) {
        return fetchIssuesForRemoteRepository(remoteRepoDetails, pagination).stream().map(GitHubIssue::new);
    }

    private static class GitHubIssue implements Issue {

        private final GitIssue issue;

        GitHubIssue(GitIssue issue) {
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

    static class GitIssue {
        private String id;
        private String body;
        private String title;

        public GitIssue(String id, String body, String title) {
            this.id = id;
            this.body = body;
            this.title = title;
        }

        public GitIssue() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return "GitIssue{" +
                    "id='" + id + '\'' +
                    ", body='" + body + '\'' +
                    ", title='" + title + '\'' +
                    '}';
        }
    }

    private List<GitIssue> fetchIssuesForRemoteRepository(GitRepoDetails gitRepoDetails, Pagination pagination) {
        String url = "/repos/{owner}/{repo}/issues?page={page}&per_page={size}";
        Map<String, String> uriVars = new HashMap<>();
        uriVars.put("page", String.valueOf(pagination.page));
        uriVars.put("size", String.valueOf(pagination.itemsPerPage));
        uriVars.put("owner", gitRepoDetails.getUserOrOrganisation());
        uriVars.put("repo", gitRepoDetails.getRepositoryName());
        ResponseEntity<GitIssue[]> forEntity = this.restTemplate.getForEntity(url, GitIssue[].class, uriVars);

        List<GitIssue> gitIssues = Arrays.asList(forEntity.getBody());
        addToCache(gitRepoDetails, gitIssues);

        if(forEntity.getStatusCode().is3xxRedirection()) {
            LOG.info("Fetching from short term cache");
            return notReallyACacheButYeh.get(getCacheKey(gitRepoDetails));
        }
        return gitIssues;
    }

    private void addToCache(GitRepoDetails gitRepoDetails, List<GitIssue> gitIssues) {
        String key = getCacheKey(gitRepoDetails);
        ArrayList<GitIssue> orDefault = notReallyACacheButYeh.getOrDefault(key, new ArrayList<>());
        orDefault.addAll(gitIssues);
        notReallyACacheButYeh.put(key, orDefault);
    }

    private String getCacheKey(GitRepoDetails gitRepoDetails) {
        return gitRepoDetails.getUserOrOrganisation() + gitRepoDetails.getRepositoryName();
    }
}
