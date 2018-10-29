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
import java.util.Map;
import java.util.stream.Collectors;

import static org.dataconservancy.pass.notification.dispatch.impl.email.RecipientParser.parseRecipientUris;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class EmailComposer {

    private static final Logger LOG = LoggerFactory.getLogger(EmailComposer.class);

    private PassClient passClient;

    @Autowired
    public EmailComposer(PassClient passClient) {
        this.passClient = passClient;
    }

    Email compose(Notification n, Map<NotificationTemplate.Name, String> templates) {
        String emailToAddress = String.join(",", parseRecipientUris(n.getRecipients()
                .stream().map(URI::create).collect(Collectors.toSet()), passClient));

        if (n.getSender() == null || n.getSender().trim().length() == 0) {
            throw new DispatchException("Notification must not have a null or empty sender!", n);
        }

        if (emailToAddress == null || emailToAddress.trim().length() == 0) {
            throw new DispatchException("Notification must not have a null or empty to address!", n);
        }

        EmailPopulatingBuilder builder = EmailBuilder.startingBlank()
                .from(n.getSender())
                .to(emailToAddress)
                .withSubject(templates.getOrDefault(NotificationTemplate.Name.SUBJECT, ""))
                .withPlainText(String.join("\n\n",
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

}
