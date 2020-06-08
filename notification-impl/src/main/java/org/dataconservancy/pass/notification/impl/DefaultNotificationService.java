/*
 * Copyright 2018 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataconservancy.pass.notification.impl;

import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.model.Submission;
import org.dataconservancy.pass.model.SubmissionEvent;
import org.dataconservancy.pass.notification.dispatch.DispatchService;
import org.dataconservancy.pass.notification.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.Collections;

import static java.lang.String.join;
import static java.util.Optional.ofNullable;

/**
 * Default implementation of {@link NotificationService} which processes {@link SubmissionEvent}s relating to proxy
 * submissions.  Self-submitted {@link Submission}s (identified by the lack of a preparer on the {@code Submission}) are
 * not processed by this implementation.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class DefaultNotificationService implements NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultNotificationService.class);

    private PassClient passClient;

    private DispatchService dispatchService;

    private Composer composer;

    @Autowired
    public DefaultNotificationService(PassClient passClient, DispatchService dispatchService, Composer composer) {
        this.passClient = passClient;
        this.dispatchService = dispatchService;
        this.composer = composer;
    }

    @Override
    public void notify(String eventUri) {

        // Retrieve SubmissionEvent
        SubmissionEvent event = null;
        try {
            event = passClient.readResource(URI.create(eventUri), SubmissionEvent.class);
        } catch (Exception e) {
            LOG.error("Unable to retrieve SubmissionEvent '{}': {}", eventUri, e);
            return;
        }

        // Retrieve Submission
        Submission submission = null;
        try {
            submission = passClient.readResource(event.getSubmission(), Submission.class);
        } catch (Exception e) {
            LOG.error("Unable to retrieve Submission '{}' for SubmissionEvent '{}': {}",
                    event.getSubmission(), eventUri, e);
            return;
        }

        // TODO abstract into a policy of some kind
        if ((submission.getPreparers() == null || submission.getPreparers().isEmpty()) ||
                (submission.getPreparers().contains(submission.getSubmitter()) && submission.getPreparers().size() == 1)) {
            // then we are not dealing with proxy submission, we're dealing with self-submission.
            // in the case of self-submission, notifications are not produced, so short-circuit here
            LOG.debug("Dropping self-submission SubmissionEvent (Event URI: {}, Resource URI: {})",
                    event.getId(),
                    submission.getId());
            return;
        }

        // Compose Notification
        Notification notification = composer.apply(submission, event);

        // Invoke Dispatch
        dispatchService.dispatch(notification);

    }

}
