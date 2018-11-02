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
package org.dataconservancy.pass.notification;

import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.client.PassClientDefault;
import org.dataconservancy.pass.model.Submission;
import org.dataconservancy.pass.model.SubmissionEvent;
import org.dataconservancy.pass.model.User;
import org.dataconservancy.pass.notification.impl.ComposerIT;
import org.dataconservancy.pass.notification.model.Link;
import org.dataconservancy.pass.notification.util.PathUtil;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;

import static java.nio.charset.Charset.forName;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.dataconservancy.pass.notification.model.Link.Rels.SUBMISSION_REVIEW_INVITE;
import static org.junit.Assert.assertTrue;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class NotificationSmokeIT {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationSmokeIT.class);

    private static final String SENDER = "staffWithGrants@jhu.edu";

    private static final String RECIPIENT = "staffWithNoGrants@jhu.edu";

    private static final String CC = "facultyWithGrants@jhu.edu";

    private PassClient passClient;

    @Before
    public void setUp() throws Exception {
        passClient = passClient();
    }

    @Test
    public void postNewEvent() throws IOException, InterruptedException {
        // This User prepares the submission on behalf of the Submission.submitter
        // Confusingly, this User has the ability to submit to PASS.  The authorization-related role of
        // User.Role.SUBMITTER should not be confused with the the logical role as a preparer of a submission.
        User preparer = new User();
        preparer.setEmail("emetsger@gmail.com");
        preparer.setDisplayName("Submission Preparer");
        preparer.setFirstName("Pre");
        preparer.setLastName("Parer");
        preparer.setRoles(Collections.singletonList(User.Role.SUBMITTER));

        preparer = passClient.createAndReadResource(preparer, User.class);

        // The Submission as prepared by the preparer.
        // The preparer did not find the authorized submitter in PASS, so they filled in the email address of the authorized submitter
        // Therefore, the Submission.submitter field will be null (because that *must* be a URI to a User resource, and the User does not exist)
        // The Submission.submitterEmail will be set to the email address of the authorized submitter
        Submission submission = new Submission();
        submission.setMetadata(resourceToString("/" + PathUtil.packageAsPath(ComposerIT.class) + "/submission-metadata.json", forName("UTF-8")));
        submission.setPreparers(Collections.singletonList(preparer.getId()));
        submission.setSource(Submission.Source.PASS);
        submission.setSubmitter(null);
        submission.setSubmitterEmail(URI.create("mailto:" + SENDER));

        submission = passClient.createAndReadResource(submission, Submission.class);

        // When this event is processed, the authorized submitter will recieve an email notification with a link that
        // will invite them to use PASS, and link the Submission to their newly created User (created when they login to PASS for the first time)
        SubmissionEvent event = new SubmissionEvent();
        event.setSubmission(submission.getId());
        event.setPerformerRole(SubmissionEvent.PerformerRole.PREPARER);
        event.setPerformedBy(preparer.getId());
        event.setComment("How does this submission look?");
        event.setEventType(SubmissionEvent.EventType.APPROVAL_REQUESTED_NEWUSER);
        event.setPerformedDate(DateTime.now());
        Link link = new Link(URI.create("https://pass.local/email/dispatch/myLink"), SUBMISSION_REVIEW_INVITE);
        event.setLink(link.getHref());

        event = passClient.createAndReadResource(event, SubmissionEvent.class);

        Thread.sleep(60000);

    }

    private PassClientDefault passClient() {
        assertTrue(System.getProperties().containsKey("pass.fedora.user"));
        assertTrue(System.getProperties().containsKey("pass.fedora.password"));
        assertTrue(System.getProperties().containsKey("pass.fedora.baseurl"));
        assertTrue(System.getProperties().containsKey("pass.elasticsearch.url"));
        assertTrue(System.getProperties().containsKey("pass.elasticsearch.limit"));
        assertTrue(System.getProperties().containsKey("http.agent"));

        return new PassClientDefault();
    }
}
