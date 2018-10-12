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
import org.dataconservancy.pass.notification.model.Notification;
import org.dataconservancy.pass.notification.model.config.NotificationConfig;
import org.dataconservancy.pass.notification.model.config.template.TemplatePrototype;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class Parameterizer {

    private NotificationConfig notificationConfig;

    private TemplateResolver templateResolver;

    private TemplateParameterizer parameterizer;

    public Parameterizer(NotificationConfig notificationConfig, TemplateResolver templateResolver, TemplateParameterizer parameterizer) {
        this.notificationConfig = notificationConfig;
        this.templateResolver = templateResolver;
        this.parameterizer = parameterizer;
    }

    Map<TemplatePrototype.Name, String> resolveAndParameterize(Notification notification, Notification.Type notificationType) {
        TemplatePrototype template = notificationConfig.getTemplates().stream()
                .filter(candidate -> candidate.getNotificationType() == notificationType)
                .findAny()
                .orElseThrow(() ->
                        new DispatchException("Missing notification template for mode '" + notificationType + "'",
                                notification));


        Map<TemplatePrototype.Name, InputStream> templates =
                template.getRefs()
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                entry -> templateResolver.resolve(entry.getKey(), entry.getValue())));

        // perform pararmeterization on all templates

        return templates.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> parameterizer.parameterize(
                                entry.getKey(), ofNullable(notification.getParameters()).orElseGet(Collections::emptyMap), entry.getValue())));
    }
}
