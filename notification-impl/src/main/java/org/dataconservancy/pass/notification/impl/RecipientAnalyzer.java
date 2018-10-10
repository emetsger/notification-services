/*
 *
 *  * Copyright 2018 Johns Hopkins University
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.dataconservancy.pass.notification.impl;

import org.dataconservancy.pass.model.Submission;
import org.dataconservancy.pass.model.SubmissionEvent;
import org.dataconservancy.pass.notification.model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;

/**
 * Examines the {@link Submission} and {@link SubmissionEvent}, and determines who should receive the notification.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class RecipientAnalyzer implements BiFunction<Submission, SubmissionEvent, Collection<String>> {

    private static final Logger LOG = LoggerFactory.getLogger(RecipientAnalyzer.class);

    private Function<Collection<String>, Collection<String>> whitelist;

    public RecipientAnalyzer(Function<Collection<String>, Collection<String>> whitelist) {
        this.whitelist = whitelist;
    }

    @Override
    public Collection<String> apply(Submission submission, SubmissionEvent event) {
        switch (event.getEventType()) {
            case APPROVAL_REQUESTED_NEWUSER:
            case APPROVAL_REQUESTED:
            {
                // to: submission.getSubmitter() // the AS
                return whitelist.apply(singleton(submission.getSubmitter().toString()));
            }

            case CHANGES_REQUESTED:
            case SUBMITTED:
            {
                // to: submission.preparers
                return whitelist.apply(submission.getPreparers().stream().map(URI::toString).collect(toSet()));
            }

            case CANCELLED: {
                String performedBy = event.getPerformedBy().toString();
                Collection<String> recipients;
                if (submission.getSubmitter().toString().equals(performedBy)) {
                    recipients =
                            whitelist.apply(submission.getPreparers().stream().map(URI::toString).collect(toSet()));
                } else {
                    recipients =
                            whitelist.apply(singleton(submission.getSubmitter().toString()));
                }

                return recipients;
            }

            default: {
                throw new RuntimeException("Unhandled SubmissionEvent type '" + event.getEventType() + "'");
            }
        }
    }

}
