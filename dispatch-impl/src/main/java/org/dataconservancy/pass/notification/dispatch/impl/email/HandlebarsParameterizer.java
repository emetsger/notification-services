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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import org.apache.commons.io.IOUtils;
import org.dataconservancy.pass.notification.model.Notification;
import org.dataconservancy.pass.notification.model.config.template.TemplatePrototype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parameterizes Mustache templates using Handlebars.
 * <p>
 * <em>Implementation note:</em> performance could be improved by pre-compiling templates.
 * </p>
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class HandlebarsParameterizer implements TemplateParameterizer {

    private static final Logger LOG = LoggerFactory.getLogger(HandlebarsParameterizer.class);

    private Handlebars handlebars;

    private ObjectMapper mapper;

    public HandlebarsParameterizer(Handlebars handlebars, ObjectMapper mapper) {
        this.handlebars = handlebars;
        this.mapper = mapper;
    }

    @Override
    public String parameterize(TemplatePrototype.Name templateName, Map<Notification.Param, String> paramMap,
                               InputStream template) {

        Map<String, Object> mustacheModel = paramMap
                .entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> entry.getKey().paramName(),
                        entry -> {
                            Notification.Param param = entry.getKey();
                            switch (param) {
                                case EVENT_METADATA:
                                case RESOURCE_METADATA:
                                    try {
                                        return mapper.readValue(entry.getValue(), Map.class);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e.getMessage(), e);
                                    }
                                case LINKS:
                                    try {
                                        return mapper.readValue(entry.getValue(), List.class);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e.getMessage(), e);
                                    }
                            }

                            return entry.getValue();
                        }));

        String parameterizedTemplate = null;
        try {
            String templateString = IOUtils.toString(template, "UTF-8");
            Template t = handlebars.compileInline(templateString);
            parameterizedTemplate = t.apply(mustacheModel);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return parameterizedTemplate;
    }
}
