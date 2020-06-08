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

import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.client.PassJsonAdapter;
import org.dataconservancy.pass.client.adapter.PassJsonAdapterBasic;
import org.dataconservancy.pass.model.Submission;
import org.dataconservancy.pass.model.SubmissionEvent;
import org.dataconservancy.pass.notification.dispatch.DispatchService;
import org.dataconservancy.pass.notification.model.Notification;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class DefaultNotificationServiceTest {

    private PassClient passClient;

    private DispatchService dispatchService;

    private Composer composer;

    private DefaultNotificationService underTest;

    private PassJsonAdapter adapter;

    @Before
    public void setUp() throws Exception {
        passClient = mock(PassClient.class);
        dispatchService = mock(DispatchService.class);
        composer = mock(Composer.class);
        adapter = new PassJsonAdapterBasic();

        underTest = new DefaultNotificationService(passClient, dispatchService, composer);
    }

    @Test
    public void success() {
        SubmissionPreparer sp = new SubmissionPreparer().invoke(passClient);

        Notification n = mock(Notification.class);
        when(composer.apply(sp.submission, sp.event)).thenReturn(n);

        // The preparers and the submitter must differ (i.e. must *not* be a self-submission) for the submissionevent to
        // be processed by defaultnotificationservice
        when(sp.submission.getSubmitter()).thenReturn(URI.create(randomUUID().toString()));
        when(sp.submission.getPreparers()).thenReturn(singletonList(URI.create(randomUUID().toString())));

        underTest.notify(sp.eventId);

        verify(passClient).readResource(sp.eventUri, SubmissionEvent.class);
        verify(passClient).readResource(sp.submissionUri, Submission.class);
        verify(composer).apply(sp.submission, sp.event);
        verify(dispatchService).dispatch(n);
    }

    /**
     * A self-submission is where the authorized submitter prepares and submits their own submission (i.e.
     * self-submission).  Notification services should not respond to self-submission SubmissionEvents
     */
    @Test
    public void selfSubmissionPreparerIsNull() {

        // mock a self submission where Submission.preparer is null

        SubmissionPreparer sp = new SubmissionPreparer().invoke(passClient);

        when(sp.getSubmission().getPreparers()).thenReturn(null);

        underTest.notify(sp.eventId);

        verify(passClient).readResource(sp.eventUri, SubmissionEvent.class);
        verify(passClient).readResource(sp.submissionUri, Submission.class);

        verifyZeroInteractions(composer);
        verifyZeroInteractions(dispatchService);

    }

    /**
     * A self-submission is where the authorized submitter prepares and submits their own submission (i.e.
     * self-submission).  Notification services should not respond to self-submission SubmissionEvents
     */
    @Test
    public void selfSubmissionPreparerIsEmpty() {

        // mock a self submission where Submission.preparer is empty

        SubmissionPreparer sp = new SubmissionPreparer().invoke(passClient);

        when(sp.getSubmission().getPreparers()).thenReturn(Collections.emptyList());

        underTest.notify(sp.eventId);

        verify(passClient).readResource(sp.eventUri, SubmissionEvent.class);
        verify(passClient).readResource(sp.submissionUri, Submission.class);

        verifyZeroInteractions(composer);
        verifyZeroInteractions(dispatchService);

    }

    /**
     * A self-submission is where the authorized submitter prepares and submits their own submission (i.e.
     * self-submission).  Notification services should not respond to self-submission SubmissionEvents
     */
    @Test
    public void selfSubmissionPreparerIsSubmitter() {

        // mock a self submission where Submission.preparer contains exactly one URI, the URI of the submitter

        SubmissionPreparer sp = new SubmissionPreparer().invoke(passClient);

        URI submitterUri = URI.create(randomUUID().toString());

        when(sp.getSubmission().getPreparers()).thenReturn(singletonList(submitterUri));
        when(sp.getSubmission().getSubmitter()).thenReturn(submitterUri);

        underTest.notify(sp.eventId);

        verify(passClient).readResource(sp.eventUri, SubmissionEvent.class);
        verify(passClient).readResource(sp.submissionUri, Submission.class);

        verifyZeroInteractions(composer);
        verifyZeroInteractions(dispatchService);
    }

    /**
     * A self-submission is where the authorized submitter prepares and submits their own submission (i.e.
     * self-submission).  Notification services should not respond to self-submission SubmissionEvents.
     *
     * In this case there are multiple preparers, and the submitter is one of them.  in this case, process the
     * submissionevent.
     */
    @Test
    public void selfSubmissionPreparerContainsSubmitter() {

        // mock a self submission where Submission.preparer contains multiple URIs, one of them is the URI of the submitter

        SubmissionPreparer sp = new SubmissionPreparer().invoke(passClient);

        URI submitterUri = URI.create(randomUUID().toString());
        URI anotherPreparerUri = URI.create(randomUUID().toString());

        when(sp.getSubmission().getPreparers()).thenReturn(Arrays.asList(submitterUri, anotherPreparerUri));
        when(sp.getSubmission().getSubmitter()).thenReturn(submitterUri);

        Notification n = mock(Notification.class);
        when(composer.apply(sp.submission, sp.event)).thenReturn(n);

        underTest.notify(sp.eventId);

        verify(passClient).readResource(sp.eventUri, SubmissionEvent.class);
        verify(passClient).readResource(sp.submissionUri, Submission.class);
        verify(composer).apply(sp.submission, sp.event);
        verify(dispatchService).dispatch(n);
    }

    /**
     * Insures a Submission based on version 3.4 of the context (e.g. using the submissionStatus field introduced in
     * version 3.4) can be deserialized by the PassJsonAdapter used by this version of Notification Services.
     */
    @Test
    public void context34SubmissionApprovalRequested() {
        String submissionJson = "" +
                "{\n" +
                "  \"@id\" : \"https://pass.jhu.edu/fcrepo/rest/submissions/3d/24/71/8a/3d24718a-5c79-4d1f-a5ab-85333fd12967\",\n" +
                "  \"@type\" : \"Submission\",\n" +
                "  \"aggregatedDepositStatus\" : \"not-started\",\n" +
                "  \"effectivePolicies\" : [ \"https://pass.jhu.edu/fcrepo/rest/policies/5e/2e/16/92/5e2e1692-c128-4fb4-b1a0-95c0e355defd\" ],\n" +
                "  \"metadata\" : \"{\\\"hints\\\":{\\\"collection-tags\\\":[\\\"covid\\\"]},\\\"publisher\\\":\\\"American Medical Association (AMA)\\\",\\\"title\\\":\\\"The Urgency and Challenge of Opening K-12 Schools in the Fall of 2020\\\",\\\"journal-title\\\":\\\"JAMA\\\",\\\"issns\\\":[{\\\"issn\\\":\\\"0098-7484\\\",\\\"pubType\\\":\\\"Print\\\"},{\\\"issn\\\":\\\"1538-3598\\\",\\\"pubType\\\":\\\"Online\\\"}],\\\"authors\\\":[{\\\"author\\\":\\\"Joshua M. Sharfstein\\\"},{\\\"author\\\":\\\"Christopher C. Morphew\\\"}],\\\"journal-NLMTA-ID\\\":\\\"JAMA\\\",\\\"publicationDate\\\":\\\"2020-6-1\\\",\\\"doi\\\":\\\"10.1001/jama.2020.10175\\\",\\\"$schema\\\":\\\"https://oa-pass.github.com/metadata-schemas/jhu/global.json\\\",\\\"agent_information\\\":{\\\"name\\\":\\\"Chrome\\\",\\\"version\\\":\\\"81\\\"}}\",\n" +
                "  \"preparers\" : [ \"https://pass.jhu.edu/fcrepo/rest/users/00019042\" ],\n" +
                "  \"publication\" : \"https://pass.jhu.edu/fcrepo/rest/publications/76/da/16/27/76da1627-a5a8-4a45-8841-c40fa357a4a3\",\n" +
                "  \"repositories\" : [ \"https://pass.jhu.edu/fcrepo/rest/repositories/41/96/0a/92/41960a92-d3f8-4616-86a6-9e9cadc1a269\" ],\n" +
                "  \"source\" : \"pass\",\n" +
                "  \"submissionStatus\" : \"approval-requested\",\n" +
                "  \"submitted\" : false,\n" +
                "  \"submitter\" : \"https://pass.jhu.edu/fcrepo/rest/users/1c/eb/55/22/1ceb5522-99de-4f6d-844f-f9e97cae3686\",\n" +
                "  \"submitterEmail\" : \"https://pass.jhu.edu/fcrepo/rest/submissions/3d/24/71/8a/3d24718a-5c79-4d1f-a5ab-85333fd12967\",\n" +
                "  \"submitterName\" : \"\",\n" +
                "  \"@context\" : \"https://oa-pass.github.io/pass-data-model/src/main/resources/context-3.4.jsonld\"\n" +
                "}";

        Submission context34submission = adapter.toModel(submissionJson.getBytes(UTF_8), Submission.class);

        // The adapter specifically removes the context after deserializing the JSON-LD, so we can't test this assertion
        // In fact, the adapter ignores @context all together.  For example, the value can be present, or absent, or set
        // to a non-sensical value, and the adapter will happily process the json.
        // assertTrue(context34submission.getContext().contains("context-3.4.jsonld"));

        assertEquals(Submission.SubmissionStatus.APPROVAL_REQUESTED, context34submission.getSubmissionStatus());

        // Prepare the, Submission, SubmissionEvent, and PassClient to use a concrete Submission deserialized from the
        // the test JSON above
        SubmissionPreparer sp = new SubmissionPreparer().invoke(passClient, context34submission);


        Notification n = mock(Notification.class);
        when(composer.apply(sp.submission, sp.event)).thenReturn(n);

        underTest.notify(sp.eventId);

        verify(passClient).readResource(sp.eventUri, SubmissionEvent.class);
        verify(passClient).readResource(sp.submissionUri, Submission.class);
        verify(composer).apply(sp.submission, sp.event);
        verify(dispatchService).dispatch(n);
    }

    private static class SubmissionPreparer {
        private String eventId;
        private URI eventUri;
        private URI submissionUri;
        private SubmissionEvent event;
        private Submission submission;

        private String getEventId() {
            return eventId;
        }

        private URI getEventUri() {
            return eventUri;
        }

        private URI getSubmissionUri() {
            return submissionUri;
        }

        private SubmissionEvent getEvent() {
            return event;
        }

        private Submission getSubmission() {
            return submission;
        }

        private SubmissionPreparer invoke(PassClient passClient) {
            eventId = "http://example.org/event/1";
            eventUri = URI.create(eventId);
            String submissionId = "http://example.org/submission/1";
            submissionUri = URI.create(submissionId);

            event = mock(SubmissionEvent.class);
            when(event.getId()).thenReturn(eventUri);
            when(event.getSubmission()).thenReturn(submissionUri);
            submission = mock(Submission.class);
            when(submission.getId()).thenReturn(submissionUri);
            when(passClient.readResource(eventUri, SubmissionEvent.class)).thenReturn(event);
            when(passClient.readResource(submissionUri, Submission.class)).thenReturn(submission);
            return this;
        }

        private SubmissionPreparer invoke(PassClient passClient, Submission submission) {
            this.submission = submission;

            eventId = "http://example.org/event/1";
            eventUri = URI.create(eventId);
            event = mock(SubmissionEvent.class);
            submissionUri = submission.getId();

            when(event.getId()).thenReturn(eventUri);
            when(event.getSubmission()).thenReturn(submissionUri);

            when(passClient.readResource(eventUri, SubmissionEvent.class)).thenReturn(event);
            when(passClient.readResource(submissionUri, Submission.class)).thenReturn(submission);

            return this;
        }
    }
}