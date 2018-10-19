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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.model.Submission;
import org.dataconservancy.pass.model.SubmissionEvent;
import org.dataconservancy.pass.notification.app.NotificationApp;
import org.dataconservancy.pass.notification.model.Notification;
import org.dataconservancy.pass.notification.model.config.Mode;
import org.dataconservancy.pass.notification.model.config.NotificationConfig;
import org.dataconservancy.pass.notification.model.config.RecipientConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.join;
import static java.nio.charset.Charset.forName;
import static java.util.Collections.singletonList;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.dataconservancy.pass.notification.impl.Composer.getRecipientConfig;
import static org.dataconservancy.pass.notification.model.Notification.Param.CC;
import static org.dataconservancy.pass.notification.model.Notification.Param.EVENT_METADATA;
import static org.dataconservancy.pass.notification.model.Notification.Param.FROM;
import static org.dataconservancy.pass.notification.model.Notification.Param.RESOURCE_METADATA;
import static org.dataconservancy.pass.notification.model.Notification.Param.SUBJECT;
import static org.dataconservancy.pass.notification.model.Notification.Param.TO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = NotificationApp.class)
public class ComposerIT {

    private String submissionId = "http://example.org/submission/1";

    private String eventId = "http://example.org/event/1";

    private String preparer_1 = "mailto:preparer_one@example.org";

    private String preparer_2 = "mailto:preparer_two@example.org";

    private Collection<String> preparers = Arrays.asList(preparer_1, preparer_2);

    private String submitter = "mailto:submitter@example.org";

    private Submission submission;

    private SubmissionEvent submissionEvent;

    @Autowired
    private NotificationConfig config;

    @Autowired
    private Composer composer;

    @Autowired
    private PassClient passClient;

    @Autowired
    @Qualifier("objectMapper")
    private ObjectMapper mapper;

    @Before
    public void setUp() throws Exception {
        assertNotNull(composer.getRecipientAnalyzer());
        assertNotNull(composer.getRecipientConfig());

        submission = mock(Submission.class);
        submissionEvent = mock(SubmissionEvent.class);

        when(submissionEvent.getId()).thenReturn(URI.create(eventId));
        when(submissionEvent.getSubmission()).thenReturn(URI.create(submissionId));
        when(submission.getId()).thenReturn(URI.create(submissionId));
        when(submission.getPreparers()).thenReturn(preparers.stream().map(URI::create).collect(Collectors.toList()));
        when(submission.getSubmitter()).thenReturn(URI.create(submitter));
    }

    /**
     * When composing a Notification with an empty whitelist, every recipient should be present.
     */
    @Test
    @DirtiesContext
    public void testEmptyWhitelist() {
        RecipientConfig config = composer.getRecipientConfig();
        config.setWhitelist(Collections.emptyList());

        when(submissionEvent.getEventType()).thenReturn(SubmissionEvent.EventType.APPROVAL_REQUESTED);

        Notification n = composer.apply(submission, submissionEvent);

        assertEquals(1, n.getRecipients().size());
        assertTrue(n.getRecipients().contains(submitter));
    }

    /**
     * When composing a Notification, the global CC addresses should not be filtered by the whitelist, while the direct recipients are.
     */
    @Test
    @DirtiesContext
    public void testGlobalCCUnaffectedByWhitelist() {
        RecipientConfig config = composer.getRecipientConfig();

        // Configure the whitelist such that the submitter's address will
        // *not* be whitelisted
        String whitelistEmail = "foo@bar.baz";
        config.setWhitelist(singletonList(whitelistEmail));

        String globalCCEmail = "cc@example.org";
        config.setGlobalCc(singletonList(globalCCEmail));

        when(submissionEvent.getEventType()).thenReturn(SubmissionEvent.EventType.APPROVAL_REQUESTED);

        Notification n = composer.apply(submission, submissionEvent);

        // The recipient list doesn't contain the submitter because they aren't whitelisted
        assertEquals(0, n.getRecipients().size());

        // The cc list does contain the expected address, because the global cc is not filtered through the whitelist at all
        assertEquals(1, n.getCc().size());
        assertTrue(n.getCc().contains(globalCCEmail));
    }

    /**
     * When composing a Notification with a non-empty whitelist, only those whitelisted recipients should be present.
     */
    @Test
    @DirtiesContext
    public void testWhitelistFilter() {
        Collection<String> allPreparers = preparers;
        Collection<String> onePreparer = singletonList(preparer_1);

        // Configure the whitelist to contain all the preparers
        RecipientConfig config = composer.getRecipientConfig();
        config.setWhitelist(allPreparers);

        when(submissionEvent.getEventType()).thenReturn(SubmissionEvent.EventType.CHANGES_REQUESTED);

        Notification n = composer.apply(submission, submissionEvent);

        // all the preparers should be present
        assertEquals(2, n.getRecipients().size());
        assertTrue(n.getRecipients().contains(preparer_1));
        assertTrue(n.getRecipients().contains(preparer_2));

        // Configure the whitelist to contain only one of the preparers
        config = composer.getRecipientConfig();
        config.setWhitelist(onePreparer);

        n = composer.apply(submission, submissionEvent);

        // Only one of the preparers should be present
        assertEquals(1, n.getRecipients().size());
        assertTrue(n.getRecipients().contains(preparer_1));
    }

    /**
     * Insure the mode for the RecipientConfig matches the NotificationConfig mode
     */
    @Test
    public void testRecipientConfigForMode() {
        assertEquals(config.getMode(), composer.getRecipientConfig().getMode());
    }

    /**
     * Insure that the proper whitelist is used for the specified mode
     */
    @Test
    @DirtiesContext
    public void testRecipientConfigForEachMode() {
        // make a unique whitelist and recipient config for each possible mode
        HashMap<Mode, RecipientConfig> rcs = new HashMap<>();
        Arrays.stream(Mode.values()).forEach(m -> {
            RecipientConfig rc = new RecipientConfig();
            rc.setMode(m);
            rc.setWhitelist(new ArrayList<>(1));
            rcs.put(m, rc);
        });

        config.setRecipientConfigs(rcs.values());

        Arrays.stream(Mode.values()).forEach(mode -> {
            config.setMode(mode);
            assertEquals(mode, new Composer(config, mapper).getRecipientConfig().getMode());
        });
    }

    /**
     * Insure the proper from address is used for the specified mode
     */
    @Test
    public void testFromForMode() {
        RecipientConfig expectedRc = config.getRecipientConfigs().stream()
                .filter(Composer.RecipientConfigFilter.modeFilter(config)).findFirst()
                .orElseThrow(() -> new RuntimeException("Missing RecipientConfig for mode '" + config.getMode() + "'"));
        String expectedFromAddress = expectedRc.getFromAddress();

        assertEquals(expectedFromAddress, composer.getRecipientConfig().getFromAddress());
    }

    @Test
    @DirtiesContext
    public void testFromForEachMode() {
        // make a unique from address and recipient config for each possible mode
        HashMap<Mode, RecipientConfig> rcs = new HashMap<>();
        Arrays.stream(Mode.values()).forEach(m -> {
            RecipientConfig rc = new RecipientConfig();
            rc.setMode(m);
            rc.setFromAddress(UUID.randomUUID().toString());
            rcs.put(m, rc);
        });

        config.setRecipientConfigs(rcs.values());

        Arrays.stream(Mode.values()).forEach(mode -> {
            config.setMode(mode);
            assertEquals(mode, new Composer(config, mapper).getRecipientConfig().getMode());
            assertEquals(rcs.get(mode).getFromAddress(), new Composer(config, mapper).getRecipientConfig().getFromAddress());
        });
    }

    /**
     * Insure the proper global CC addresses are used for the specified mode
     */
    @Test
    @DirtiesContext
    public void testGlobalCCForEachMode() {
        // make a unique global address and recipient config for each possible mode
        HashMap<Mode, RecipientConfig> rcs = new HashMap<>();
        Arrays.stream(Mode.values()).forEach(m -> {
            RecipientConfig rc = new RecipientConfig();
            rc.setMode(m);
            rc.setGlobalCc(Collections.singleton(UUID.randomUUID().toString()));
            rcs.put(m, rc);
        });

        config.setRecipientConfigs(rcs.values());

        Arrays.stream(Mode.values()).forEach(mode -> {
            config.setMode(mode);
            assertEquals(mode, new Composer(config, mapper).getRecipientConfig().getMode());
            assertEquals(rcs.get(mode).getGlobalCc(), new Composer(config, mapper).getRecipientConfig().getGlobalCc());
        });
    }

    /**
     * Insure that event types are properly mapped to notification types
     *  APPROVAL_REQUESTED_NEWUSER -> SUBMISSION_APPROVAL_INVITE
     *  APPROVAL_REQUESTED -> SUBMISSION_APPROVAL_REQUESTED
     *  CHANGES_REQUESTED -> SUBMISSION_CHANGES_REQUESTED
     *  SUBMITTED -> SUBMISSION_SUBMISSION_SUBMITTED
     *  CANCELLED -> SUBMISSION_SUBMISSION_CANCELLED
     */
    @Test
    public void testEventMappingToNotificationType() {
        HashMap<SubmissionEvent.EventType, Notification.Type> expectedMapping =
                new HashMap<SubmissionEvent.EventType, Notification.Type>() {
                    {
                        put(SubmissionEvent.EventType.APPROVAL_REQUESTED_NEWUSER,
                                Notification.Type.SUBMISSION_APPROVAL_INVITE);
                        put(SubmissionEvent.EventType.APPROVAL_REQUESTED,
                                Notification.Type.SUBMISSION_APPROVAL_REQUESTED);
                        put(SubmissionEvent.EventType.CHANGES_REQUESTED,
                                Notification.Type.SUBMISSION_CHANGES_REQUESTED);
                        put(SubmissionEvent.EventType.SUBMITTED,
                                Notification.Type.SUBMISSION_SUBMISSION_SUBMITTED);
                        put(SubmissionEvent.EventType.CANCELLED,
                                Notification.Type.SUBMISSION_SUBMISSION_CANCELLED);
                    }
                };

        Arrays.stream(SubmissionEvent.EventType.values()).forEach(eventType -> {
            SubmissionEvent event = new SubmissionEvent();
            event.setEventType(eventType);
            event.setId(URI.create(eventId));
            event.setSubmission(URI.create(submissionId));
            event.setPerformedBy(URI.create(submitter));

            Submission submission = new Submission();
            submission.setId(URI.create(submissionId));
            submission.setPreparers(preparers.stream().map(URI::create).collect(Collectors.toList()));
            submission.setSubmitter(URI.create(submitter));

            assertEquals(expectedMapping.get(eventType), composer.apply(submission, event).getType());
        });
    }

    /**
     * Insure that the {@link Notification#getParameters() parameters} are properly populated
     */
    @Test
    public void testNotificationParametersModel() throws IOException {
        String to = "mailto:emetsger@mail.local.domain";
        String from = "mailto:preparer@mail.local.domain";

        Submission submission = new Submission();
        submission.setSubmitter(URI.create(to));
        URI preparerUri = URI.create(from);
        submission.setPreparers(singletonList(preparerUri));
        String metadata = resourceToString(join("/","", packageAsPath(), "submission-metadata.json"), forName("UTF-8"));
        submission.setMetadata(metadata);
        submission = passClient.createAndReadResource(submission, Submission.class);

        SubmissionEvent event = new SubmissionEvent();
        event.setSubmission(submission.getId());
        event.setEventType(SubmissionEvent.EventType.APPROVAL_REQUESTED_NEWUSER);
        event.setPerformedBy(preparerUri);
        event.setComment("Please see if this submission meets your approval.");
        event = passClient.createAndReadResource(event, SubmissionEvent.class);

        Notification n = composer.apply(submission, event);
        Map<Notification.Param, String> params = n.getParameters();

        assertEquals(Composer.resourceMetadata(submission, mapper), params.get(RESOURCE_METADATA));
        // TODO Params map contains URIs of recipients at this point, they've not been resolved to email addresses
        // TODO Recipient URIs aren't resolved until Dispatch
        assertEquals(to, params.get(TO));
        assertEquals(getRecipientConfig(config).getFromAddress(), params.get(FROM));
        assertEquals(join(",", getRecipientConfig(config).getGlobalCc()), params.get(CC));
        assertEquals(Composer.eventMetadata(event, mapper), params.get(EVENT_METADATA));
        JsonNode eventMdNode = mapper.readTree(params.get(EVENT_METADATA));
        assertEquals(event.getId().toString(), Composer.field("id", eventMdNode).get());
        // TODO remove Subject?  Unset at this point, since templates haven't been resolved or parameterized
        assertNull(params.get(SUBJECT));
    }

    private static String packageAsPath() {
        return ComposerIT.class.getPackage().getName().replace('.', '/');
    }
}
