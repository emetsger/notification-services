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
import org.dataconservancy.pass.notification.dispatch.DispatchService;
import org.dataconservancy.pass.notification.model.Notification;
import org.dataconservancy.pass.notification.model.config.NotificationConfig;
import org.dataconservancy.pass.notification.model.config.template.TemplatePrototype;
import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.Mailer;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import static org.dataconservancy.pass.notification.dispatch.impl.email.RecipientParser.parseRecipientUris;

/**
 * Dispatches {@link Notification}s as email messages.  Email templates are configured by {@link TemplatePrototype}s
 * obtained from the {@link NotificationConfig}, and processed using a {@link TemplateParameterizer}.
 * <p>
 * This implementation expects notification recipients to be encoded as URIs.  Email addresses can be encoded
 * as {@code mailto} URIs, and PASS {@code User} resources encoded as Fedora repository URIs.  {@code mailto}
 * URIs will be parsed for an email address and optionally a name enclosed with &lt; and &gt;.  PASS {@code User} URIs
 * will be de-referenced and the {@code "email"} property used as the recipient email address.
 * </p>
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 * @see <a href="https://tools.ietf.org/html/rfc6068">RFC 6068</a>
 */
public class EmailDispatchImpl implements DispatchService {

    private NotificationConfig notificationConfig;

    private PassClient passClient;

    private TemplateResolver templateResolver;

    private TemplateParameterizer parameterizer;

    private Mailer mailer;

    public EmailDispatchImpl(NotificationConfig notificationConfig, PassClient passClient,
                             TemplateResolver templateResolver,
                             TemplateParameterizer parameterizer, Mailer mailer) {
        this.notificationConfig = notificationConfig;
        this.passClient = passClient;
        this.templateResolver = templateResolver;
        this.parameterizer = parameterizer;
        this.mailer = mailer;
    }

    @Override
    public void dispatch(Notification notification) {

        Notification.Type notificationType = notification.getType();

        // resolve templates for subject, body, footer based on notification type

        TemplatePrototype template = notificationConfig.getTemplates().stream()
                .filter(candidate -> candidate.getNotificationType() == notificationType)
                .findAny()
                .orElseThrow(() ->
                        new RuntimeException("Missing notification template for mode '" + notificationType + "'"));

        Map<TemplatePrototype.Name, InputStream> templates =
            template.getRefs()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            entry -> templateResolver.resolve(entry.getKey(), entry.getValue())));

        // perform pararmeterization on all templates

        Map<TemplatePrototype.Name, String> parameterized =
                templates.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> parameterizer.parameterize(
                                entry.getKey(), notification.getParameters(), entry.getValue())));

        // compose email

        String emailToAddress = String.join(",", parseRecipientUris(notification.getRecipients()
                .stream().map(URI::create).collect(Collectors.toSet()), passClient));

        Email email = EmailBuilder.startingBlank()
                .from(notification.getSender())
                .to(emailToAddress)
                .cc(String.join(",", notification.getCc()))
                .withSubject(parameterized.getOrDefault(TemplatePrototype.Name.SUBJECT, ""))
                .withPlainText(String.join("\n\n",
                        parameterized.getOrDefault(TemplatePrototype.Name.BODY, ""),
                        parameterized.getOrDefault(TemplatePrototype.Name.FOOTER, "")))
                .buildEmail();

        // send email

        mailer.sendMail(email);

    }

}
