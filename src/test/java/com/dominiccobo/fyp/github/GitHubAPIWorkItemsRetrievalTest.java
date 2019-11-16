package com.dominiccobo.fyp.github;

import com.dominiccobo.fyp.context.api.queries.AssociatedWorkItemsQuery;
import com.dominiccobo.fyp.context.models.QueryContext;
import com.dominiccobo.fyp.context.models.WorkItem;
import com.dominiccobo.fyp.context.models.git.GitContext;
import com.dominiccobo.fyp.context.models.git.GitRemoteIdentifier;
import com.dominiccobo.fyp.context.models.git.GitRemoteURL;
import com.dominiccobo.fyp.github.provider.GitHubAPI;
import com.dominiccobo.fyp.github.provider.Issue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GitHubAPIWorkItemsRetrievalTest {

    private GitHubWorkItemResolver fixture;

    @Mock
    private GitHubAPI gitHubApi;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        fixture = new GitHubWorkItemResolver(gitHubApi);
    }

    @Test
    public void testNoResultsOnNonGitHubUrl() {
        Map<GitRemoteIdentifier, GitRemoteURL> remotes = new HashMap<>();
        remotes.put(new GitRemoteIdentifier("upstream"), new GitRemoteURL("git@gitlab.com:dominiccobo/cs3004-assignment.git"));
        GitContext gitContext = new GitContext(remotes, null);
        QueryContext queryContext = new QueryContext(gitContext, null);
        AssociatedWorkItemsQuery qry = new AssociatedWorkItemsQuery(queryContext);
        List<WorkItem> result = fixture.on(qry);
        assertThat(result).isEmpty();
    }

    @Test
    public void givenUserRepoExists_whenQueryMadeForRemoteWorkItems_singleWorkItemRetrievedFromUserRepo() {
        final String TITLE = "test";
        final String BODY = "test";

        when(gitHubApi.getIssuesForRepository(any()))
                .thenReturn(Collections.singletonList(new MockableIssue(TITLE, BODY)));

        Map<GitRemoteIdentifier, GitRemoteURL> remotes = new HashMap<>();
        remotes.put(new GitRemoteIdentifier("upstream"), new GitRemoteURL("git@github.com:dominiccobo/cs3004-assignment.git"));
        GitContext gitContext = new GitContext(remotes, null);
        QueryContext queryContext = new QueryContext(gitContext, null);
        AssociatedWorkItemsQuery qry = new AssociatedWorkItemsQuery(queryContext);
        List<WorkItem> result = fixture.on(qry);

        assertThat(result).contains(new WorkItem().setBody(TITLE).setTitle(BODY));
        assertThat(result).hasSize(1);

    }

    private static class MockableIssue implements Issue {

        private final String title;
        private final String body;

        private MockableIssue(String title, String body) {
            this.title = title;
            this.body = body;
        }

        @Override
        public String getTitle() {
            return this.title;
        }

        @Override
        public String getBody() {
            return this.body;
        }
    }
}