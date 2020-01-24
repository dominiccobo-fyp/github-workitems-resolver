package com.dominiccobo.fyp.github.provider;

import com.dominiccobo.fyp.github.utils.GitRepoDetails;

import java.util.stream.Stream;

public interface GitHubAPI {

    Stream<Issue> getIssuesForRepository(GitRepoDetails remoteRepoDetails);
}
