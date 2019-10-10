package com.dominiccobo.fyp.github;

import com.dominiccobo.fyp.api.events.GitHubUpstreamResolved;
import org.axonframework.eventhandling.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SampleResolver {

    public static final Logger LOG = LoggerFactory.getLogger(SampleResolver.class);

    @EventHandler
    public void onEvent(GitHubUpstreamResolved resolved) {
        LOG.info("Received event {}", resolved.url);
    }
}
