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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Attempts to resolve a named template using a list of template resolvers.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class CompositeResolver implements TemplateResolver {

    private static Logger LOG = LoggerFactory.getLogger(CompositeResolver.class);

    private List<TemplateResolver> resolvers;

    private final List<Exception> exceptionHolder;

    public CompositeResolver(List<TemplateResolver> resolvers) {
        Objects.requireNonNull(resolvers, "Template resolvers must not be null");
        this.resolvers = resolvers;
        exceptionHolder = new ArrayList<>(2);
    }

    public CompositeResolver(List<TemplateResolver> resolvers, List<Exception> exceptionHolder) {
        Objects.requireNonNull(resolvers, "Template resolvers must not be null");
        Objects.requireNonNull(exceptionHolder, "Exception Holder must not be null");
        this.resolvers = resolvers;
        this.exceptionHolder = exceptionHolder;
    }

    @Override
    public InputStream resolve(NotificationTemplate.Name name, String template) {
        InputStream in = null;
        for (TemplateResolver resolver : resolvers) {
            try {
                LOG.debug("Attempting resolution of template value '{}', for named template '{}'", template, name);
                in = resolver.resolve(name, template);
                if (in != null) {
                    return in;
                }
            } catch (Exception e) {
                LOG.debug("Unable to resolve template '{}' {}: ", name, e.getMessage(), e);
                exceptionHolder.add(e);
            }
        }

        StringBuilder msg = new StringBuilder("Unable to resolve template name '" + name + "', '" + template + "':\n");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream ps = new PrintStream(baos)) {
            exceptionHolder.forEach(ex -> ex.printStackTrace(ps));
        }

        msg.append(new String(baos.toByteArray()));

        throw new RuntimeException(msg.toString());
    }
}
