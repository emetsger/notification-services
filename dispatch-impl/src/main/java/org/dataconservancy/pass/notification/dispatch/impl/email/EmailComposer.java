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

        EmailPopulatingBuilder builder = EmailBuilder.startingBlank()
                .from(n.getSender())
                .to(emailToAddress)
                .withSubject(templates.getOrDefault(NotificationTemplate.Name.SUBJECT, ""))
                .withPlainText(join("\n\n",
                        templates.getOrDefault(NotificationTemplate.Name.BODY, ""),
                        templates.getOrDefault(NotificationTemplate.Name.FOOTER, "")));

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
