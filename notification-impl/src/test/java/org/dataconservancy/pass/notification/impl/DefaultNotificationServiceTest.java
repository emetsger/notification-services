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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
        String eventId = "http://example.org/event/1";
        URI eventUri = URI.create(eventId);
        String submissionId = "http://example.org/submission/1";
        URI submissionUri = URI.create(submissionId);

        SubmissionEvent event = mock(SubmissionEvent.class);
        when(event.getId()).thenReturn(eventUri);
        when(event.getSubmission()).thenReturn(submissionUri);
        Submission submission = mock(Submission.class);
        when(submission.getId()).thenReturn(submissionUri);
        when(passClient.readResource(eventUri, SubmissionEvent.class)).thenReturn(event);
        when(passClient.readResource(submissionUri, Submission.class)).thenReturn(submission);

        Notification n = mock(Notification.class);
        when(composer.apply(submission, event)).thenReturn(n);

        underTest.notify(eventId);

        verify(passClient).readResource(eventUri, SubmissionEvent.class);
        verify(passClient).readResource(submissionUri, Submission.class);
        verify(composer).apply(submission, event);
        verify(dispatchService).dispatch(n);
    }
}