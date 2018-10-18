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

import org.dataconservancy.pass.notification.dispatch.DispatchException;
import org.dataconservancy.pass.notification.dispatch.DispatchService;
import org.dataconservancy.pass.notification.model.Notification;
import org.dataconservancy.pass.notification.model.config.NotificationConfig;
import org.dataconservancy.pass.notification.model.config.template.NotificationTemplate;
import org.simplejavamail.email.Email;
import org.simplejavamail.mailer.Mailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Dispatches {@link Notification}s as email messages.  Email templates are configured by {@link NotificationTemplate}s
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

    private static final Logger LOG = LoggerFactory.getLogger(EmailDispatchImpl.class);

    private Parameterizer parameterizer;

    private Mailer mailer;

    private EmailComposer composer;

    public EmailDispatchImpl(Parameterizer parameterizer, Mailer mailer, EmailComposer composer) {
        this.parameterizer = parameterizer;
        this.mailer = mailer;
        this.composer = composer;
    }

    @Override
    public String dispatch(Notification notification) {
        try {
            Notification.Type notificationType = notification.getType();

            // resolve templates for subject, body, footer based on notification type

            Map<NotificationTemplate.Name, String> parameterizedTemplates = parameterizer.
                    resolveAndParameterize(notification, notificationType);

            // compose email

            Email email = composer.compose(notification, parameterizedTemplates);

            // send email

            mailer.sendMail(email);

            LOG.trace("Dispatched email with id '{}'", email.getId());

            return email.getId();
        } catch (Exception e) {
            throw new DispatchException(e.getMessage(), e, notification);
        }

    }

}
