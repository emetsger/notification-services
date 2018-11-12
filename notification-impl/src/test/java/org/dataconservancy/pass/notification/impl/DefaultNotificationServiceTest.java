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
import org.dataconservancy.pass.model.Submission;
import org.dataconservancy.pass.model.SubmissionEvent;
import org.dataconservancy.pass.notification.dispatch.DispatchService;
import org.dataconservancy.pass.notification.model.Notification;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class DefaultNotificationServiceTest {

    private PassClient passClient;

    private DispatchService dispatchService;

    private Composer composer;

    private DefaultNotificationService underTest;

    @Before
    public void setUp() throws Exception {
        passClient = mock(PassClient.class);
        dispatchService = mock(DispatchService.class);
        composer = mock(Composer.class);

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
    }
}