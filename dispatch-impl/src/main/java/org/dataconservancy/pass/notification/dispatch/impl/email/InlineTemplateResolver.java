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

import org.dataconservancy.pass.notification.model.config.template.NotificationTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Considers the supplied template to be "inline".
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class InlineTemplateResolver implements TemplateResolver {

    /**
     * {@inheritDoc}
     * <p>
     * <em>Implementation note:</em> returns the bytes of the supplied string as an {@code InputStream}
     * </p>
     * @param name the name of the template, ignored in this implementation
     * @param template the content of an inline template, simply returned as an {@code InputStream}
     * @return the supplied {@code template} as an {@code InputStream}
     */
    @Override
    public InputStream resolve(NotificationTemplate.Name name, String template) {
        return new ByteArrayInputStream(template.getBytes());
    }

}
