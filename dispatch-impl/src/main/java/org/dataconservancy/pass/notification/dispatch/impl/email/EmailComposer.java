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
import org.dataconservancy.pass.notification.model.Notification;
import org.dataconservancy.pass.notification.model.config.template.TemplatePrototype;
import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import static org.dataconservancy.pass.notification.dispatch.impl.email.RecipientParser.parseRecipientUris;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class EmailComposer {

    private PassClient passClient;

    @Autowired
    public EmailComposer(PassClient passClient) {
        this.passClient = passClient;
    }

    Email compose(Notification n, Map<TemplatePrototype.Name, String> templates) {
        String emailToAddress = String.join(",", parseRecipientUris(n.getRecipients()
                .stream().map(URI::create).collect(Collectors.toSet()), passClient));

        return EmailBuilder.startingBlank()
                .from(n.getSender())
                .to(emailToAddress)
                .cc(String.join(",", n.getCc()))
                .withSubject(templates.getOrDefault(TemplatePrototype.Name.SUBJECT, ""))
                .withPlainText(String.join("\n\n",
                        templates.getOrDefault(TemplatePrototype.Name.BODY, ""),
                        templates.getOrDefault(TemplatePrototype.Name.FOOTER, "")))
                .buildEmail();
    }

}
