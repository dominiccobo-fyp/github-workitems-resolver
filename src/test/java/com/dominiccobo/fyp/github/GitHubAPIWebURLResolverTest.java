package com.dominiccobo.fyp.github;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;


public class GitHubAPIWebURLResolverTest {

    @Test
    public void givenGitHubUrl_whenChecked_isNotNull() {
        String testUrl = "git@github.com:dominiccobo/Scientia-Week-Schedule-Scraper.git";
        GitRepoDetails result = GitRepoDetails.from(testUrl);
        assertThat(result).isNotNull();
    }

    @Test
    public void givenNonGitHubUrl_whenChecked_isNull() {
        String testUrl = "git@gitlab.com:dominiccobo/Scientia-Week-Schedule-Scraper.git";
        GitRepoDetails result = GitRepoDetails.from(testUrl);
        assertThat(result).isNull();
    }

    @Test
    public void givenGitHubUrl_whenParsed_returnsAppropriateUrl() {
        String testUrl = "git@github.com:dominiccobo/Scientia-Week-Schedule-Scraper.git";
        GitRepoDetails result = GitRepoDetails.from(testUrl);
        String expectedOrg = "dominiccobo";
        String expectedRepo = "Scientia-Week-Schedule-Scraper";
        assertThat(result.getUserOrOrganisation()).matches(expectedOrg);
        assertThat(result.getRepositoryName()).matches(expectedRepo);
    }
}