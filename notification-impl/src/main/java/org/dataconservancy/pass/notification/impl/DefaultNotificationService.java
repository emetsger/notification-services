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

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class DefaultNotificationService implements NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultNotificationService.class);

    @Autowired
    private PassClient passClient;

    @Autowired
    private DispatchService dispatchService;

    @Autowired
    private Composer composer;

    @Override
    public void notify(String eventUri) {

        // Retrieve SubmissionEvent
        SubmissionEvent event = passClient.readResource(URI.create(eventUri), SubmissionEvent.class);

        // Retrieve Submission
        Submission submission = passClient.readResource(event.getSubmission(), Submission.class);

        // Compose Notification
        Notification notification = composer.apply(submission, event);

        // Invoke Dispatch
        dispatchService.dispatch(notification);

    }

}
