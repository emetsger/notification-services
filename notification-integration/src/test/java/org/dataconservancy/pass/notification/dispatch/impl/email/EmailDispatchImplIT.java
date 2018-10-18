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

import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.model.User;
import org.dataconservancy.pass.notification.SpringBootIntegrationConfig;
import org.dataconservancy.pass.notification.app.NotificationApp;
import org.dataconservancy.pass.notification.model.SimpleNotification;
import org.dataconservancy.pass.notification.model.config.NotificationConfig;
import org.dataconservancy.pass.notification.model.config.template.NotificationTemplate;
import org.dataconservancy.pass.notification.util.async.Condition;
import org.dataconservancy.pass.notification.util.mail.SimpleImapClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.mail.Message;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.Callable;

import static java.lang.String.join;
import static org.dataconservancy.pass.notification.model.Notification.Type.SUBMISSION_APPROVAL_INVITE;
import static org.dataconservancy.pass.notification.util.mail.SimpleImapClient.getBodyAsText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { NotificationApp.class, SpringBootIntegrationConfig.class })
public class EmailDispatchImplIT {

    private static final Logger LOG = LoggerFactory.getLogger(EmailDispatchImplIT.class);

    @Autowired
    private EmailDispatchImpl underTest;

    @Autowired
    private PassClient passClient;

    @Autowired
    private SimpleImapClient imapClient;

    @Autowired
    private NotificationConfig config;

    /**
     * Simple test insuring the basic parts of the dispatch email are where they belong.
     */
    @Test
    public void simpleSuccess() throws Exception {
        String sender = "preparer@mail.local.domain";
        String cc = "cc@mail.local.domain";
        String recipient = "emetsger@mail.local.domain";
        String expectedBody = "Approval Invite Body\r\n\r\nApproval Invite Footer";

        SimpleNotification n = new SimpleNotification();
        n.setType(SUBMISSION_APPROVAL_INVITE);
        n.setSender(sender);
        n.setCc(Collections.singleton(cc));
        n.setRecipient(Collections.singleton("mailto:" + recipient));

        String messageId = underTest.dispatch(n);
        assertNotNull(messageId);

        newMessageCondition(messageId, imapClient).await();
        Message message = getMessage(messageId, imapClient).call();
        assertNotNull(message);

        assertEquals("Approval Invite Subject", message.getSubject());
        assertEquals(expectedBody, getBodyAsText(message));
        assertEquals(sender, message.getFrom()[0].toString());
        assertEquals(cc, message.getRecipients(Message.RecipientType.CC)[0].toString());
        assertEquals(recipient, message.getRecipients(Message.RecipientType.TO)[0].toString());
    }

    /**
     * Dispatching a notification with a PASS User URI as a recipient should result in the proper resolution of the {@code to} recipient.
     */
    @Test
    public void dispatchResolveUserUri() throws Exception {
        String sender = "preparer@mail.local.domain";
        String recipient = "emetsger@mail.local.domain";

        User recipientUser = new User();
        recipientUser.setEmail(recipient);
        URI recipientUri = passClient.createResource(recipientUser);

        SimpleNotification n = new SimpleNotification();
        n.setType(SUBMISSION_APPROVAL_INVITE);
        n.setSender(sender);
        n.setRecipient(Collections.singleton(recipientUri.toString()));

        String messageId = underTest.dispatch(n);

        newMessageCondition(messageId, imapClient).await();
        Message message = getMessage(messageId, imapClient).call();

        assertEquals(recipient, message.getRecipients(Message.RecipientType.TO)[0].toString());
    }

    /**
     * References to subject/body/footer templates should be resolved
     */
    @Test
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
        n.setSender("preparer@mail.local.domain");
        n.setRecipient(Collections.singleton("mailto:emetsger@mail.local.domain"));

        String messageId = underTest.dispatch(n);
        assertNotNull(messageId);

        newMessageCondition(messageId, imapClient).await();
        Message message = getMessage(messageId, imapClient).call();
        assertNotNull(message);

        assertEquals("Handlebars Subject", message.getSubject());
        assertEquals("Handlebars Body\r\n\r\nHandlebars Footer", getBodyAsText(message));
    }



    private static String packageAsPath() {
        return EmailDispatchImplIT.class.getPackage().getName().replace('.', '/');
    }

    private static Condition<Message> newMessageCondition(String messageId, SimpleImapClient imapClient) {
        return new Condition<>(getMessage(messageId, imapClient), Objects::nonNull, "New message");
    }

    private static Callable<Message> getMessage(String messageId, SimpleImapClient imapClient) {
        return () -> imapClient.getMessage(messageId);
    }
}
