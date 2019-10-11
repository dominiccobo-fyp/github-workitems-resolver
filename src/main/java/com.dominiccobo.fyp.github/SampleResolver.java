package com.dominiccobo.fyp.github;

import com.dominiccobo.fyp.api.queries.GetWebAppForUpstreamUrl;
import org.axonframework.queryhandling.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SampleResolver {

    private static final Logger LOG = LoggerFactory.getLogger(SampleResolver.class);

    @QueryHandler()
    public GetWebAppForUpstreamUrl.Result on(GetWebAppForUpstreamUrl query) {
        LOG.info("Received query for {}", query.upstreamUrl);

        if(isGitHub(query.upstreamUrl)) {
            return new GetWebAppForUpstreamUrl.Result(resolveSSHUrlToWebView(query.upstreamUrl));
        }
        else return null;
    }

    static boolean isGitHub(String testString) {
        return testString.contains("git@github.com");
    }

    static String resolveSSHUrlToWebView(String url) {
        String pathDir = url.split("git@github.com:")[1];
        pathDir = pathDir.replaceAll(".git", "");
        return "https://github.com/" + pathDir;
    }
}
