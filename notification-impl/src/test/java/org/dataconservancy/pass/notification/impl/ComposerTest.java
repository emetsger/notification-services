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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.text.StringEscapeUtils;
import org.dataconservancy.pass.model.Submission;
import org.dataconservancy.pass.model.SubmissionEvent;
import org.dataconservancy.pass.model.SubmissionEvent.EventType;
import org.dataconservancy.pass.notification.model.Notification;
import org.dataconservancy.pass.notification.model.Notification.Param;
import org.dataconservancy.pass.notification.model.config.Mode;
import org.dataconservancy.pass.notification.model.config.NotificationConfig;
import org.dataconservancy.pass.notification.model.config.RecipientConfig;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class ComposerTest {

    private static final String METADATA_JSON_BLOB = "[{\"id\": \"JScholarship\", \"data\": {\"embargo\": \"NON-EXCLUSIVE LICENSE FOR USE OF MATERIALS This non-exclusive license defines the terms for the deposit of Materials in all formats into the digital repository of materials collected, preserved and made available through the Johns Hopkins Digital Repository, JScholarship. The Contributor hereby grants to Johns Hopkins a royalty free, non-exclusive worldwide license to use, re-use, display, distribute, transmit, publish, re-publish or copy the Materials, either digitally or in print, or in any other medium, now or hereafter known, for the purpose of including the Materials hereby licensed in the collection of materials in the Johns Hopkins Digital Repository for educational use worldwide. In some cases, access to content may be restricted according to provisions established in negotiation with the copyright holder. This license shall not authorize the commercial use of the Materials by Johns Hopkins or any other person or organization, but such Materials shall be restricted to non-profit educational use. Persons may apply for commercial use by contacting the copyright holder. Copyright and any other intellectual property right in or to the Materials shall not be transferred by this agreement and shall remain with the Contributor, or the Copyright holder if different from the Contributor. Other than this limited license, the Contributor or Copyright holder retains all rights, title, copyright and other interest in the images licensed. If the submission contains material for which the Contributor does not hold copyright, the Contributor represents that s/he has obtained the permission of the Copyright owner to grant Johns Hopkins the rights required by this license, and that such third-party owned material is clearly identified and acknowledged within the text or content of the submission. If the submission is based upon work that has been sponsored or supported by an agency or organization other than Johns Hopkins, the Contributor represents that s/he has fulfilled any right of review or other obligations required by such contract or agreement. Johns Hopkins will not make any alteration, other than as allowed by this license, to your submission. This agreement embodies the entire agreement of the parties. No modification of this agreement shall be of any effect unless it is made in writing and signed by all of the parties to the agreement.\", \"agreement-to-deposit\": \"true\"}}, {\"id\": \"common\", \"data\": {\"title\": \"Specific protein supplementation using soya, casein or whey differentially affects regional gut growth and luminal growth factor bioactivity in rats; implications for the treatment of gut injury and stimulating repair\", \"journal-title\": \"Food & Function\", \"volume\": \"9\", \"issue\": \"1\", \"ISSN\": \"2042-6496,2042-650X\", \"abstract\": \"Differential enhancement of luminal growth factor bioactivity and targeted regional gut growth occurs dependent on dietary protein supplement.\", \"authors\": [{\"author\": \"Tania Marchbank\", \"orcid\": \"http://orcid.org/0000-0003-2076-9098\"}, {\"author\": \"Nikki Mandir\"}, {\"author\": \"Denis Calnan\"}, {\"author\": \"Robert A. Goodlad\"}, {\"author\": \"Theo Podas\"}, {\"author\": \"Raymond J. Playford\", \"orcid\": \"http://orcid.org/0000-0003-1235-8504\"}], \"Embargo-end-date\": \"2018-06-30\", \"issn-map\": {\"2042-6496\": {\"pub-type\": [\"Print\"]}, \"2042-650X\": {\"pub-type\": [\"Electronic\"]}}}}, {\"id\": \"crossref\", \"data\": {\"doi\": \"10.1039/c7fo01251a\", \"publisher\": \"Royal Society of Chemistry (RSC)\", \"journal-title-short\": \"Food Funct.\"}}, {\"id\": \"pmc\", \"data\": {\"nlmta\": \"Food Funct\"}}]";

    private static final String NOTIFICATION_FROM_ADDRESS = "pass-production-noreply@jhu.edu";

    private static final List<String> NOTIFICATION_GLOBAL_CC_ADDRESS = Arrays.asList("pass@jhu.edu", "pass-prod-cc@jhu.edu");

    private Composer underTest;

    private Function<Collection<String>, Collection<String>> whitelister;

    private NotificationConfig notificationConfig;


    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        whitelister = mock(Function.class);
        notificationConfig = mock(NotificationConfig.class);

        Mode runtimeMode = Mode.PRODUCTION;
        RecipientConfig recipientConfig = new RecipientConfig();
        recipientConfig.setMode(runtimeMode);
        recipientConfig.setFromAddress(NOTIFICATION_FROM_ADDRESS);
        recipientConfig.setGlobalCc(NOTIFICATION_GLOBAL_CC_ADDRESS);

        // all recipients are whitelisted
        when(whitelister.apply(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notificationConfig.getMode()).thenReturn(runtimeMode);
        when(notificationConfig.getRecipientConfigs()).thenReturn(singletonList(recipientConfig));

        underTest = new Composer(notificationConfig, new RecipientAnalyzer(whitelister));
    }

    /**
     * test the notification that is composed by the business layer for requesting approval to submit where the
     * authorized submitter is not a {@code User} in PASS.
     */
    @Test
    public void approvalRequestedNewUser() {
        SubmissionEvent event = new SubmissionEvent();
        URI eventUri = URI.create("uri:" + UUID.randomUUID().toString());
        event.setEventType(EventType.APPROVAL_REQUESTED_NEWUSER);
        event.setId(eventUri);

        Submission submission = new Submission();
        URI submissionUri = URI.create("uri:" + UUID.randomUUID().toString());
        String userUri = "mailto:jane_professor@jhu.edu";
        URI mailtoUri = URI.create(userUri);
        submission.setMetadata(METADATA_JSON_BLOB);
        submission.setId(submissionUri);
        submission.setSubmitter(mailtoUri);
        event.setSubmission(submissionUri);

        Notification notification = underTest.apply(submission, event);

        assertNotNull(notification);

        // verify params
        Map<Param, String> params = notification.getParameters();
        assertNotNull(params);
        // Dispatch Impl will handle the treatment of mailto: uris vs User resource uris
        assertEquals(userUri, params.get(Param.TO));
        assertEquals(NOTIFICATION_FROM_ADDRESS, params.get(Param.FROM));
        assertEquals(String.join(",", NOTIFICATION_GLOBAL_CC_ADDRESS), params.get(Param.CC));
        assertEquals(METADATA_JSON_BLOB, params.get(Param.RESOURCE_METADATA));
        assertEquals(Notification.Type.SUBMISSION_APPROVAL_INVITE, notification.getType());

        // TODO test event metadata?
        // TODO test links
    }

    @Test
    public void approvalRequested() {
        SubmissionEvent event = new SubmissionEvent();
        URI eventUri = URI.create("uri:" + UUID.randomUUID().toString());
        event.setEventType(EventType.APPROVAL_REQUESTED);
        event.setId(eventUri);

        Submission submission = new Submission();
        URI submissionUri = URI.create("uri:" + UUID.randomUUID().toString());
        String userUri = "http://pass.jhu.edu/fcrepo/users/abc123";
        URI mailtoUri = URI.create(userUri);
        submission.setMetadata(METADATA_JSON_BLOB);
        submission.setId(submissionUri);
        submission.setSubmitter(mailtoUri);
        event.setSubmission(submissionUri);

        Notification notification = underTest.apply(submission, event);

        assertNotNull(notification);

        // verify params
        Map<Param, String> params = notification.getParameters();
        assertNotNull(params);
        // Dispatch Impl will handle the treatment of mailto: uris vs User resource uris
        assertEquals(userUri, params.get(Param.TO));
        assertEquals(NOTIFICATION_FROM_ADDRESS, params.get(Param.FROM));
        assertEquals(String.join(",", NOTIFICATION_GLOBAL_CC_ADDRESS), params.get(Param.CC));
        assertEquals(METADATA_JSON_BLOB, params.get(Param.RESOURCE_METADATA));
        assertEquals(Notification.Type.SUBMISSION_APPROVAL_REQUESTED, notification.getType());

        // TODO test event metadata?
        // TODO test links
    }

    @Test
    public void changesRequested() {
        SubmissionEvent event = new SubmissionEvent();
        URI eventUri = URI.create("uri:" + UUID.randomUUID().toString());
        event.setEventType(EventType.CHANGES_REQUESTED);
        event.setId(eventUri);

        Submission submission = new Submission();
        URI submissionUri = URI.create("uri:" + UUID.randomUUID().toString());
        String preparersUri = "http://pass.jhu.edu/fcrepo/users/abc123";
        submission.setMetadata(METADATA_JSON_BLOB);
        submission.setId(submissionUri);
        submission.setPreparers(singletonList(URI.create(preparersUri)));
        event.setSubmission(submissionUri);

        Notification notification = underTest.apply(submission, event);

        assertNotNull(notification);

        // verify params
        Map<Param, String> params = notification.getParameters();
        assertNotNull(params);
        // Dispatch Impl will handle the treatment of mailto: uris vs User resource uris
        assertEquals(preparersUri, params.get(Param.TO));
        assertEquals(NOTIFICATION_FROM_ADDRESS, params.get(Param.FROM));
        assertEquals(String.join(",", NOTIFICATION_GLOBAL_CC_ADDRESS), params.get(Param.CC));
        assertEquals(METADATA_JSON_BLOB, params.get(Param.RESOURCE_METADATA));
        assertEquals(Notification.Type.SUBMISSION_CHANGES_REQUESTED, notification.getType());

        // TODO test event metadata?
        // TODO test links
    }

    @Test
    public void submitted() {
        SubmissionEvent event = new SubmissionEvent();
        URI eventUri = URI.create("uri:" + UUID.randomUUID().toString());
        event.setEventType(EventType.SUBMITTED);
        event.setId(eventUri);

        Submission submission = new Submission();
        URI submissionUri = URI.create("uri:" + UUID.randomUUID().toString());
        String preparersUri = "http://pass.jhu.edu/fcrepo/users/abc123";
        submission.setMetadata(METADATA_JSON_BLOB);
        submission.setId(submissionUri);
        submission.setPreparers(singletonList(URI.create(preparersUri)));
        event.setSubmission(submissionUri);

        Notification notification = underTest.apply(submission, event);

        assertNotNull(notification);

        // verify params
        Map<Param, String> params = notification.getParameters();
        assertNotNull(params);
        // Dispatch Impl will handle the treatment of mailto: uris vs User resource uris
        assertEquals(preparersUri, params.get(Param.TO));
        assertEquals(NOTIFICATION_FROM_ADDRESS, params.get(Param.FROM));
        assertEquals(String.join(",", NOTIFICATION_GLOBAL_CC_ADDRESS), params.get(Param.CC));
        assertEquals(METADATA_JSON_BLOB, params.get(Param.RESOURCE_METADATA));
        assertEquals(Notification.Type.SUBMISSION_SUBMISSION_SUBMITTED, notification.getType());

        // TODO test event metadata?
        // TODO test links
    }

    @Test
    public void cancelledByPreparer() {
        SubmissionEvent event = new SubmissionEvent();
        URI eventUri = URI.create("uri:" + UUID.randomUUID().toString());
        event.setEventType(EventType.CANCELLED);
        event.setId(eventUri);

        Submission submission = new Submission();
        URI submissionUri = URI.create("uri:" + UUID.randomUUID().toString());
        String preparersUri = "http://pass.jhu.edu/fcrepo/users/abc123";
        String submitterUri = "http://pass.jhu.edu/fcrepo/users/xyz789";
        submission.setMetadata(METADATA_JSON_BLOB);
        submission.setId(submissionUri);
        submission.setPreparers(singletonList(URI.create(preparersUri)));
        submission.setSubmitter(URI.create(submitterUri));
        event.setPerformedBy(URI.create(preparersUri));
        event.setSubmission(submissionUri);

        Notification notification = underTest.apply(submission, event);

        assertNotNull(notification);

        // verify params
        Map<Param, String> params = notification.getParameters();
        assertNotNull(params);
        // Dispatch Impl will handle the treatment of mailto: uris vs User resource uris
        assertEquals(submitterUri, params.get(Param.TO));
        assertEquals(NOTIFICATION_FROM_ADDRESS, params.get(Param.FROM));
        assertEquals(String.join(",", NOTIFICATION_GLOBAL_CC_ADDRESS), params.get(Param.CC));
        assertEquals(METADATA_JSON_BLOB, params.get(Param.RESOURCE_METADATA));
        assertEquals(Notification.Type.SUBMISSION_SUBMISSION_CANCELLED, notification.getType());

        // TODO test event metadata?
        // TODO test links
    }

    @Test
    public void cancelledBySubmitter() {
        SubmissionEvent event = new SubmissionEvent();
        URI eventUri = URI.create("uri:" + UUID.randomUUID().toString());
        event.setEventType(EventType.CANCELLED);
        event.setId(eventUri);

        Submission submission = new Submission();
        URI submissionUri = URI.create("uri:" + UUID.randomUUID().toString());
        String preparersUri = "http://pass.jhu.edu/fcrepo/users/abc123";
        String submitterUri = "http://pass.jhu.edu/fcrepo/users/xyz789";
        submission.setMetadata(METADATA_JSON_BLOB);
        submission.setId(submissionUri);
        submission.setPreparers(singletonList(URI.create(preparersUri)));
        submission.setSubmitter(URI.create(submitterUri));
        event.setPerformedBy(URI.create(submitterUri));
        event.setSubmission(submissionUri);

        Notification notification = underTest.apply(submission, event);

        assertNotNull(notification);

        // verify params
        Map<Param, String> params = notification.getParameters();
        assertNotNull(params);
        // Dispatch Impl will handle the treatment of mailto: uris vs User resource uris
        assertEquals(preparersUri, params.get(Param.TO));
        assertEquals(NOTIFICATION_FROM_ADDRESS, params.get(Param.FROM));
        assertEquals(String.join(",", NOTIFICATION_GLOBAL_CC_ADDRESS), params.get(Param.CC));
        assertEquals(METADATA_JSON_BLOB, params.get(Param.RESOURCE_METADATA));
        assertEquals(Notification.Type.SUBMISSION_SUBMISSION_CANCELLED, notification.getType());

        // TODO test event metadata?
        // TODO test links
    }

    @Test
    public void jsonMappingOfParams() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        SubmissionEvent event = new SubmissionEvent();
        URI eventUri = URI.create("uri:" + UUID.randomUUID().toString());
        event.setEventType(EventType.CANCELLED);
        event.setId(eventUri);

        Submission submission = new Submission();
        URI submissionUri = URI.create("uri:" + UUID.randomUUID().toString());
        String preparersUri = "http://pass.jhu.edu/fcrepo/users/abc123";
        String submitterUri = "http://pass.jhu.edu/fcrepo/users/xyz789";
        submission.setMetadata(METADATA_JSON_BLOB);
        submission.setId(submissionUri);
        submission.setPreparers(singletonList(URI.create(preparersUri)));
        submission.setSubmitter(URI.create(submitterUri));
        event.setPerformedBy(URI.create(submitterUri));
        event.setSubmission(submissionUri);

        Notification notification = underTest.apply(submission, event);

        System.err.println(notification.getParameters().get(Param.RESOURCE_METADATA));

        notification.getParameters().put(Param.SUBJECT, "Don\'t try \"this\" at home!");

        String escapedOut = StringEscapeUtils.unescapeJson(
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(notification.getParameters()));

        System.err.println(escapedOut);


    }
}