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
import org.dataconservancy.pass.notification.dispatch.DispatchException;
import org.dataconservancy.pass.notification.model.Notification;
import org.dataconservancy.pass.notification.model.config.template.NotificationTemplate;
import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.email.EmailPopulatingBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.join;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class EmailComposer {

    private static final Logger LOG = LoggerFactory.getLogger(EmailComposer.class);

    private static final Logger NOTIFICATION_LOG = LoggerFactory.getLogger("NOTIFICATION_LOG");

    public static final String SUBMISSION_SMTP_HEADER = "X-PASS-Submission-ID";

    public static final String NOTIFICATION_TYPE_SMTP_HEADER = "X-PASS-Notification-Type";

    private PassClient passClient;

    private Function<Collection<String>, Collection<String>> whitelist;

    @Autowired
    public EmailComposer(PassClient passClient, Function<Collection<String>, Collection<String>> whitelist) {
        this.passClient = passClient;
        this.whitelist = whitelist;
    }

    Email compose(Notification n, Map<NotificationTemplate.Name, String> templates) {
        if (n.getSender() == null || n.getSender().trim().length() == 0) {
            throw new DispatchException("Notification must not have a null or empty sender!", n);
        }

        // Resolve the Notification Recipient URIs to email addresses, then apply the whitelist of email addresses

        Set<URI> recipientUris = n.getRecipients()
                .stream().map(URI::create).collect(Collectors.toSet());


        LOG.debug("Initial recipients: [{}]", join(",", recipientUris.stream().map(URI::toString).collect(Collectors.toSet())));

        Collection<String> resolvedRecipients = recipientUris
                .stream()
                .map(uri -> {
                    if (uri.getScheme() != null && uri.getScheme().startsWith("http")) {
                        User user = passClient.readResource(uri, User.class);
                        return user.getEmail();
                    }

                    return uri.getSchemeSpecificPart();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        LOG.debug("Applying whitelist to resolved recipients: [{}]", join(",", resolvedRecipients));

        Collection<String> whitelistedRecipients = whitelist.apply(resolvedRecipients);

        LOG.debug("Whitelisted recipients: [{}]", join(", ", whitelistedRecipients));
        NOTIFICATION_LOG.info("Whitelisted recipients: [{}]", join(", ", whitelistedRecipients));

        String emailToAddress = join(",", whitelistedRecipients);

        if (emailToAddress == null || emailToAddress.trim().length() == 0) {
            throw new DispatchException("Notification must not have a null or empty to address!", n);
        }

        // Build the email
        String subject = templates.getOrDefault(NotificationTemplate.Name.SUBJECT, "");
        String body = templates.getOrDefault(NotificationTemplate.Name.BODY, "");
        String footer = templates.getOrDefault(NotificationTemplate.Name.FOOTER, "");
        String from = n.getSender();

        LOG.debug("Building email with the following:\n" +
                " {}: {}\n" +
                " {}: {}\n" +
                " {}: {}\n" +
                " {}: {}\n" +
                " {}: {}\n" +
                " {}: {}\n" +
                " {}: {}\n",
                SUBMISSION_SMTP_HEADER, n.getResourceUri(),
                NOTIFICATION_TYPE_SMTP_HEADER, n.getType().toString(),
                "From", from,
                "To", emailToAddress,
                "Subject", subject,
                "body text", body,
                "footer text", footer);

        EmailPopulatingBuilder builder = EmailBuilder.startingBlank()
                .from(from)
                .to(emailToAddress)
                .withSubject(subject)
                .withPlainText(join("\n\n",
                        body,
                        footer));

        // These should never be null in production; being defensive because some tests may not set them
        if (n.getResourceUri() != null) {
            builder.withHeader(SUBMISSION_SMTP_HEADER, n.getResourceUri());
        }

        if (n.getType() != null) {
            builder.withHeader(NOTIFICATION_TYPE_SMTP_HEADER, n.getType().toString());
        }

        // builder refuses to build the cc with an empty collection
        if (n.getCc() != null && n.getCc().size() > 0) {
            // A configuration with a non-existent "${pass.notification.demo.global.cc.address}" property will
            // result in a list with one element, an empty string.  Filter out any empty string values before
            // continuing
            String filtered = n.getCc().stream()
                    .filter(ccAddress -> ccAddress.length() > 0)
                    .collect(Collectors.joining(","));
            if (filtered.length() > 0) {
                builder.cc(filtered);
            }
        }

        return builder.buildEmail();
    }

    void setWhitelist(Function<Collection<String>, Collection<String>> whitelist) {
        this.whitelist = whitelist;
    }
}
