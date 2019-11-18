package com.dominiccobo.fyp.github.provider;

import com.dominiccobo.fyp.github.utils.GitRepoDetails;

import java.util.List;

public interface GitHubAPI {

    List<Issue> getIssuesForRepository(GitRepoDetails remoteRepoDetails);
}
