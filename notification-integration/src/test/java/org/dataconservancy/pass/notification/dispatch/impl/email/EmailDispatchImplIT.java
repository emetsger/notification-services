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
package org.dataconservancy.pass.notification.dispatch.impl.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.model.Submission;
import org.dataconservancy.pass.model.SubmissionEvent;
import org.dataconservancy.pass.model.User;
import org.dataconservancy.pass.notification.SimpleImapClientFactory;
import org.dataconservancy.pass.notification.SpringBootIntegrationConfig;
import org.dataconservancy.pass.notification.app.NotificationApp;
import org.dataconservancy.pass.notification.dispatch.DispatchException;
import org.dataconservancy.pass.notification.impl.Composer;
import org.dataconservancy.pass.notification.impl.ComposerIT;
import org.dataconservancy.pass.notification.model.Link;
import org.dataconservancy.pass.notification.model.Notification;
import org.dataconservancy.pass.notification.model.SimpleNotification;
import org.dataconservancy.pass.notification.model.config.NotificationConfig;
import org.dataconservancy.pass.notification.model.config.template.NotificationTemplate;
import org.dataconservancy.pass.notification.util.async.Condition;
import org.dataconservancy.pass.notification.util.mail.SimpleImapClient;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import javax.mail.Message;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Callable;

import static java.nio.charset.Charset.forName;
import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.dataconservancy.pass.notification.impl.Links.serialized;
import static org.dataconservancy.pass.notification.model.Link.Rels.SUBMISSION_REVIEW_INVITE;
import static org.dataconservancy.pass.notification.model.Notification.Param.EVENT_METADATA;
import static org.dataconservancy.pass.notification.model.Notification.Param.FROM;
import static org.dataconservancy.pass.notification.model.Notification.Param.LINKS;
import static org.dataconservancy.pass.notification.model.Notification.Param.RESOURCE_METADATA;
import static org.dataconservancy.pass.notification.model.Notification.Param.TO;
import static org.dataconservancy.pass.notification.model.Notification.Type.SUBMISSION_APPROVAL_INVITE;
import static org.dataconservancy.pass.notification.util.mail.SimpleImapClient.getBodyAsText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { NotificationApp.class, SpringBootIntegrationConfig.class })
public class EmailDispatchImplIT {

    private static final Logger LOG = LoggerFactory.getLogger(EmailDispatchImplIT.class);

    private static final String SENDER = "staffWithGrants@jhu.edu";

    private static final String RECIPIENT = "staffWithNoGrants@jhu.edu";

    private static final String CC = "facultyWithGrants@jhu.edu";

    @Autowired
    private EmailDispatchImpl underTest;

    @Autowired
    private PassClient passClient;

    @Autowired
    private SimpleImapClientFactory imapClientFactory;

    @Autowired
    private NotificationConfig config;

    @Autowired
    private ObjectMapper objectMapper;

    private SimpleImapClient imapClient;

    @Before
    public void setUp() throws Exception {
        imapClientFactory.setImapUser(RECIPIENT);
        imapClientFactory.setImapPass("moo");
        imapClient = imapClientFactory.getObject();
    }

    /**
     * Simple test insuring the basic parts of the dispatch email are where they belong.
     */
    @Test
    public void simpleSuccess() throws Exception {
        String expectedBody = "Approval Invite Body\r\n\r\nApproval Invite Footer";

        SimpleNotification n = new SimpleNotification();
        n.setType(SUBMISSION_APPROVAL_INVITE);
        n.setSender(SENDER);
        n.setCc(Collections.singleton(CC));
        n.setRecipient(Collections.singleton("mailto:" + RECIPIENT));

        String messageId = underTest.dispatch(n);
        assertNotNull(messageId);

        newMessageCondition(messageId, imapClient).await();
        Message message = getMessage(messageId, imapClient).call();
        assertNotNull(message);

        assertEquals("Approval Invite Subject", message.getSubject());
        assertEquals(expectedBody, getBodyAsText(message));
        assertEquals(SENDER, message.getFrom()[0].toString());
        assertEquals(CC, message.getRecipients(Message.RecipientType.CC)[0].toString());
        assertEquals(RECIPIENT, message.getRecipients(Message.RecipientType.TO)[0].toString());
    }

    /**
     * Dispatching a notification with a PASS User URI as a recipient should result in the proper resolution of the {@code to} recipient.
     */
    @Test
    public void dispatchResolveUserUri() throws Exception {
        User recipientUser = new User();
        recipientUser.setEmail(RECIPIENT);
        URI recipientUri = passClient.createResource(recipientUser);

        SimpleNotification n = new SimpleNotification();
        n.setType(SUBMISSION_APPROVAL_INVITE);
        n.setSender(SENDER);
        n.setRecipient(Collections.singleton(recipientUri.toString()));

        String messageId = underTest.dispatch(n);

        newMessageCondition(messageId, imapClient).await();
        Message message = getMessage(messageId, imapClient).call();

        assertEquals(RECIPIENT, message.getRecipients(Message.RecipientType.TO)[0].toString());
    }

    /**
     * References to subject/body/footer templates should be resolved
     */
    @Test
    @DirtiesContext
    public void notificationConfigWithTemplateRefs() throws Exception {

        // Override the NotificationTemplate for approval invites, subbing in Spring URIs as references
        // to template bodies
        NotificationTemplate template = config.getTemplates().stream()
                .filter(templatePrototype ->
                        templatePrototype.getNotificationType() == SUBMISSION_APPROVAL_INVITE)
                .findAny()
                .orElseThrow(() -> new RuntimeException("Missing expected template for SUBMISSION_APPROVAL_INVITE"));

        template.setTemplates(new HashMap<NotificationTemplate.Name, String>() {
            {
                put(NotificationTemplate.Name.SUBJECT, "classpath:" + packageAsPath() + "/subject.hbr");
                put(NotificationTemplate.Name.BODY, "classpath:" + packageAsPath() + "/body.hbr");
                put(NotificationTemplate.Name.FOOTER, "classpath:" + packageAsPath() + "/footer.hbr");
            }
        });

        config.setTemplates(Collections.singleton(template));

        SimpleNotification n = new SimpleNotification();
        n.setType(SUBMISSION_APPROVAL_INVITE);
        n.setSender(SENDER);
        n.setRecipient(Collections.singleton("mailto:" + RECIPIENT));

        String messageId = underTest.dispatch(n);
        assertNotNull(messageId);

        newMessageCondition(messageId, imapClient).await();
        Message message = getMessage(messageId, imapClient).call();
        assertNotNull(message);

        assertEquals("Handlebars Subject", message.getSubject());
        assertEquals("Handlebars Body\r\n\r\nHandlebars Footer", getBodyAsText(message));
    }

    @Test
    public void subjectTemplateParameterization() throws Exception {
        Submission submission = new Submission();
        submission.setMetadata(resourceToString("/" + packageAsPath(ComposerIT.class) + "/submission-metadata.json", forName("UTF-8")));

        SubmissionEvent event = new SubmissionEvent();
        event.setId(URI.create("http://example.org/event/1"));
        event.setPerformerRole(SubmissionEvent.PerformerRole.PREPARER);
        event.setPerformedBy(URI.create("http://example.org/user/1"));
        event.setComment("How does this submission look?");
        event.setEventType(SubmissionEvent.EventType.APPROVAL_REQUESTED_NEWUSER);
        event.setPerformedDate(DateTime.now());

        // Override the NotificationTemplate for approval invites, including a template that
        // requires parameterization
        NotificationTemplate template = config.getTemplates().stream()
                .filter(templatePrototype ->
                        templatePrototype.getNotificationType() == SUBMISSION_APPROVAL_INVITE)
                .findAny()
                .orElseThrow(() -> new RuntimeException("Missing expected template for SUBMISSION_APPROVAL_INVITE"));

        template.setTemplates(new HashMap<NotificationTemplate.Name, String>() {
            {
                put(NotificationTemplate.Name.SUBJECT, "classpath:" + packageAsPath() + "/subject-parameterize.hbr");
                put(NotificationTemplate.Name.BODY, "classpath:" + packageAsPath() + "/body-parameterize.hbr");
                put(NotificationTemplate.Name.FOOTER, "Footer");
            }
        });

        config.setTemplates(Collections.singleton(template));
        
        Link link = new Link(URI.create("http://example.org/email/dispatch/myLink"), SUBMISSION_REVIEW_INVITE);

        SimpleNotification n = new SimpleNotification();
        n.setType(SUBMISSION_APPROVAL_INVITE);
        n.setSender(SENDER);
        n.setRecipient(Collections.singleton("mailto:" + RECIPIENT));
        n.setParameters(new HashMap<Notification.Param, String>() {
            {
                put(RESOURCE_METADATA, Composer.resourceMetadata(submission, objectMapper));
                put(EVENT_METADATA, Composer.eventMetadata(event, objectMapper));
                put(FROM, SENDER);
                put(TO, RECIPIENT);
                put(LINKS, asList(link).stream().collect(serialized()));
            }
        });

        String messageId = underTest.dispatch(n);
        assertNotNull(messageId);

        newMessageCondition(messageId, imapClient).await();
        Message message = getMessage(messageId, imapClient).call();
        assertNotNull(message);

        String expectedTitle = objectMapper.readTree(submission.getMetadata()).findValue("title").asText();
        String expectedSubject = "PASS Submission titled \"" + expectedTitle + "\" awaiting your approval";
        assertEquals(expectedSubject, message.getSubject());

        String body = SimpleImapClient.getBodyAsText(message);

        assertTrue(body.contains("Dear " + n.getParameters().get(TO)));
        assertTrue(body.contains("prepared on your behalf by " + n.getParameters().get(FROM)));  // TODO FROM will be the global FROM, must insure the preparer User is represented in metadata.
        assertTrue(body.contains(event.getComment()));
        assertTrue(body.contains(expectedTitle));
        assertTrue(body.contains("Please review the submission at the following URL: " + link.getHref()));
    }

    /**
     * mailing a non-existent email address should result in the appropriate exception
     * (in coordination with a like-minded email relay)
     */
    @Test
    public void nonExistentEmailAddress() {
        String nonExistentRecipientAddress = "moo-thru@bar.edu";
        SimpleNotification n = new SimpleNotification();
        n.setType(SUBMISSION_APPROVAL_INVITE);
        n.setSender(SENDER);
        n.setRecipient(Collections.singleton("mailto:" + nonExistentRecipientAddress));

        try {
            underTest.dispatch(n);
        } catch (Exception e) {
            assertTrue(e instanceof DispatchException);
            Throwable rootCause = e.getCause();
            boolean sfeFound = false;
            while (rootCause.getCause() != null) {
                if (rootCause instanceof javax.mail.SendFailedException) {
                        sfeFound = true;
                        break;
                }
                rootCause = rootCause.getCause();
            }

            assertTrue("Missing expected javax.mail.SendFailedException in the stack trace.", sfeFound);
            assertTrue("Expected the string 'Invalid Addresses' to be in the exception message.", rootCause.getMessage().contains("Invalid Addresses"));

            return;
        }

        fail("Expected a DispatchException to be thrown.");
    }

    private static String packageAsPath() {
        return packageAsPath(EmailDispatchImplIT.class);
    }

    private static String packageAsPath(Class<?> clazz) {
        return clazz.getPackage().getName().replace('.', '/');
    }

    private static Condition<Message> newMessageCondition(String messageId, SimpleImapClient imapClient) {
        Condition<Message> c = new Condition<>(getMessage(messageId, imapClient), Objects::nonNull, "New message");
        c.setTimeoutThresholdMs(10000);
        return c;
    }

    private static Callable<Message> getMessage(String messageId, SimpleImapClient imapClient) {
        return () -> imapClient.getMessage(messageId);
    }
}
