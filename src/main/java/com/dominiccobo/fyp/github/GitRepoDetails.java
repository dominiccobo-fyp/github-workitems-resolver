package com.dominiccobo.fyp.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitRepoDetails {
    private static final Logger LOG = LoggerFactory.getLogger(GitRepoDetails.class);
    private final String userOrOrganisation;
    private final String repositoryName;

    private GitRepoDetails(String userOrOrganisation, String repositoryName) {
        this.userOrOrganisation = userOrOrganisation;
        this.repositoryName = repositoryName;
    }

    public static GitRepoDetails from(String remoteUrl) {
        LOG.trace("Resolving URL {}", remoteUrl);

        if(isAGitHubUpstream(remoteUrl)) {
            return resolveSSHUrlToRemoteURL((remoteUrl));
        }
        else return null;
    }

    private static boolean isAGitHubUpstream(String testString) {
        return testString.contains("git@github.com");
    }

    private static GitRepoDetails resolveSSHUrlToRemoteURL(String url) {
        String pathDir = url.split("git@github.com:")[1];
        pathDir = pathDir.replaceAll(".git", "");
        String[] orgToRepo = pathDir.split("/");
        return new GitRepoDetails(orgToRepo[0], orgToRepo[1]);
    }

    public String getUserOrOrganisation() {
        return userOrOrganisation;
    }

    public String getRepositoryName() {
        return repositoryName;
    }
}
