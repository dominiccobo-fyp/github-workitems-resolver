package com.dominiccobo.fyp.github;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;


public class SampleResolverTest {

    @Test
    public void givenGitHubUrl_whenChecked_isTrue() {
        String testUrl = "git@github.com:dominiccobo/Scientia-Week-Schedule-Scraper.git";
        boolean result = SampleResolver.isGitHub(testUrl);

        assertThat(result).isTrue();
    }

    @Test
    public void givenNonGitHubUrl_whenChecked_isTrue() {
        String testUrl = "git@gitlab.com:dominiccobo/Scientia-Week-Schedule-Scraper.git";
        boolean result = SampleResolver.isGitHub(testUrl);

        assertThat(result).isFalse();
    }

    @Test
    public void givenGitHubUrl_whenParsed_returnsWebAppUrl() {
        String testUrl = "git@github.com:dominiccobo/Scientia-Week-Schedule-Scraper.git";
        String resultUrl = SampleResolver.resolveSSHUrlToWebView(testUrl);
        String expectedUrl = "https://github.com/dominiccobo/Scientia-Week-Schedule-Scraper";
        assertThat(resultUrl).matches(expectedUrl);
    }
}