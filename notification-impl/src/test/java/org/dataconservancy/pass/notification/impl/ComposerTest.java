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
import org.apache.commons.text.StringEscapeUtils;

import org.dataconservancy.pass.authz.usertoken.Key;
import org.dataconservancy.pass.model.Submission;
import org.dataconservancy.pass.model.SubmissionEvent;
import org.dataconservancy.pass.model.SubmissionEvent.EventType;
import org.dataconservancy.pass.notification.model.Link;
import org.dataconservancy.pass.notification.model.Notification;
import org.dataconservancy.pass.notification.model.Notification.Param;
import org.dataconservancy.pass.notification.model.config.Mode;
import org.dataconservancy.pass.notification.model.config.NotificationConfig;
import org.dataconservancy.pass.notification.model.config.RecipientConfig;
import org.dataconservancy.pass.notification.model.config.UserTokenGeneratorConfig;

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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.dataconservancy.pass.notification.impl.Links.deserialize;
import static org.dataconservancy.pass.notification.impl.LinksTest.randomUri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class ComposerTest {

    private static final String RESOURCE_METADATA = "{\"title\":\"Specific protein supplementation using soya, casein or whey differentially affects regional gut growth and luminal growth factor bioactivity in rats; implications for the treatment of gut injury and stimulating repair\",\"journal-title\":\"Food & Function\",\"volume\":\"9\",\"issue\":\"1\",\"abstract\":\"Differential enhancement of luminal growth factor bioactivity and targeted regional gut growth occurs dependent on dietary protein supplement.\",\"doi\":\"10.1039/c7fo01251a\",\"publisher\":\"Royal Society of Chemistry (RSC)\",\"authors\":\"[{\\\"author\\\":\\\"Tania Marchbank\\\",\\\"orcid\\\":\\\"http://orcid.org/0000-0003-2076-9098\\\"},{\\\"author\\\":\\\"Nikki Mandir\\\"},{\\\"author\\\":\\\"Denis Calnan\\\"},{\\\"author\\\":\\\"Robert A. Goodlad\\\"},{\\\"author\\\":\\\"Theo Podas\\\"},{\\\"author\\\":\\\"Raymond J. Playford\\\",\\\"orcid\\\":\\\"http://orcid.org/0000-0003-1235-8504\\\"}]\"}";

    private static final String NOTIFICATION_FROM_ADDRESS = "pass-production-noreply@jhu.edu";

    private static final List<String> NOTIFICATION_GLOBAL_CC_ADDRESS = Arrays.asList("pass@jhu.edu", "pass-prod-cc@jhu.edu");

    private static final List<String> NOTIFICATION_GLOBAL_BCC_ADDRESS = singletonList("pass-prod-bcc@jhu.edu");

    private Composer underTest;

    private Function<Collection<String>, Collection<String>> whitelister;

    private NotificationConfig notificationConfig;
    
    private SubmissionLinkAnalyzer submissionLinkAnalyzer;
    
    private static final List<Link> generatedSubmissionLinks = asList(
            new Link(randomUri(), "rel1"),
            new Link(randomUri(), "rel2"));


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
        recipientConfig.setGlobalBcc(NOTIFICATION_GLOBAL_BCC_ADDRESS);

        // all recipients are whitelisted
        when(whitelister.apply(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notificationConfig.getMode()).thenReturn(runtimeMode);
        when(notificationConfig.getRecipientConfigs()).thenReturn(singletonList(recipientConfig));
        
        UserTokenGeneratorConfig userTokenConfig = new UserTokenGeneratorConfig();
        userTokenConfig.setKey(Key.generate().toString());
        when(notificationConfig.getUserTokenGeneratorConfig()).thenReturn(userTokenConfig);

        ObjectMapper mapper = new ObjectMapper();
        
        submissionLinkAnalyzer = mock(SubmissionLinkAnalyzer.class);
        when(submissionLinkAnalyzer.apply(any(), any())).thenReturn(generatedSubmissionLinks.stream());
        
        LinkValidator linkValidator = mock(LinkValidator.class);
        when(linkValidator.test(any())).thenReturn(true);

        underTest = new Composer(notificationConfig, new RecipientAnalyzer(), submissionLinkAnalyzer, linkValidator, mapper);
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
        submission.setMetadata(RESOURCE_METADATA);
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
        assertEquals(String.join(",", NOTIFICATION_GLOBAL_BCC_ADDRESS), params.get(Param.BCC));
        assertEquals(RESOURCE_METADATA, params.get(Param.RESOURCE_METADATA));
        assertEquals(Notification.Type.SUBMISSION_APPROVAL_INVITE, notification.getType());

        // TODO test event metadata?

        assertLinksPresent(notification, submission, event);
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
        submission.setMetadata(RESOURCE_METADATA);
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
        assertEquals(String.join(",", NOTIFICATION_GLOBAL_BCC_ADDRESS), params.get(Param.BCC));
        assertEquals(RESOURCE_METADATA, params.get(Param.RESOURCE_METADATA));
        assertEquals(Notification.Type.SUBMISSION_APPROVAL_REQUESTED, notification.getType());

        // TODO test event metadata?
        
        assertLinksPresent(notification, submission, event);
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
        submission.setMetadata(RESOURCE_METADATA);
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
        assertEquals(String.join(",", NOTIFICATION_GLOBAL_BCC_ADDRESS), params.get(Param.BCC));
        assertEquals(RESOURCE_METADATA, params.get(Param.RESOURCE_METADATA));
        assertEquals(Notification.Type.SUBMISSION_CHANGES_REQUESTED, notification.getType());

        // TODO test event metadata?
        
        assertLinksPresent(notification, submission, event);
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
        submission.setMetadata(RESOURCE_METADATA);
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
        assertEquals(String.join(",", NOTIFICATION_GLOBAL_BCC_ADDRESS), params.get(Param.BCC));
        assertEquals(RESOURCE_METADATA, params.get(Param.RESOURCE_METADATA));
        assertEquals(Notification.Type.SUBMISSION_SUBMISSION_SUBMITTED, notification.getType());

        // TODO test event metadata?
        
        assertLinksPresent(notification, submission, event);
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
        submission.setMetadata(RESOURCE_METADATA);
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
        assertEquals(String.join(",", NOTIFICATION_GLOBAL_BCC_ADDRESS), params.get(Param.BCC));
        assertEquals(RESOURCE_METADATA, params.get(Param.RESOURCE_METADATA));
        assertEquals(Notification.Type.SUBMISSION_SUBMISSION_CANCELLED, notification.getType());

        // TODO test event metadata?
        
        assertLinksPresent(notification, submission, event);
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
        submission.setMetadata(RESOURCE_METADATA);
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
        assertEquals(String.join(",", NOTIFICATION_GLOBAL_BCC_ADDRESS), params.get(Param.BCC));
        assertEquals(RESOURCE_METADATA, params.get(Param.RESOURCE_METADATA));
        assertEquals(Notification.Type.SUBMISSION_SUBMISSION_CANCELLED, notification.getType());

        // TODO test event metadata?
        
        assertLinksPresent(notification, submission, event);
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
        submission.setMetadata(RESOURCE_METADATA);
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

    @Test
    public void testModeFilter() {
        RecipientConfig prod = new RecipientConfig();
        prod.setMode(Mode.PRODUCTION);

        RecipientConfig demo = new RecipientConfig();
        demo.setMode(Mode.DEMO);

        RecipientConfig disabled = new RecipientConfig();
        disabled.setMode(Mode.DISABLED);

        NotificationConfig config = new NotificationConfig();
        config.setRecipientConfigs(Arrays.asList(prod, demo, disabled));

        config.setMode(Mode.PRODUCTION);
        assertTrue(Composer.RecipientConfigFilter.modeFilter(config).test(prod));

        config.setMode(Mode.DEMO);
        assertTrue(Composer.RecipientConfigFilter.modeFilter(config).test(demo));

        config.setMode(Mode.DISABLED);
        assertTrue(Composer.RecipientConfigFilter.modeFilter(config).test(disabled));

        assertFalse(Composer.RecipientConfigFilter.modeFilter(config).test(demo));
        assertFalse(Composer.RecipientConfigFilter.modeFilter(config).test(prod));

        config.setMode(Mode.PRODUCTION);
        config.setRecipientConfigs(Arrays.asList(prod, demo));

        assertFalse(Composer.RecipientConfigFilter.modeFilter(config).test(disabled));
    }
    
    void assertLinksPresent(Notification notification, Submission submission, SubmissionEvent event) {
        
        // Make sure the submission link analyzer was called with the appropriate arguments
        verify(submissionLinkAnalyzer).apply(eq(submission), eq(event));
        
        // Make sure the generated links are attached.
        String serializedLinks = notification.getParameters().get(Notification.Param.LINKS);
        assertNotNull(serializedLinks);
        
        Collection<Link> deserializedLinks = deserialize(serializedLinks);
        assertEquals(generatedSubmissionLinks.size(), deserializedLinks.size());
        assertTrue(deserializedLinks.containsAll(generatedSubmissionLinks));
        
    }
}