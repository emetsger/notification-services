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
package org.dataconservancy.pass.notification.app.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import org.springframework.core.env.Environment;

import java.io.IOException;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class SpringEnvStringDeserializer extends StringDeserializer {

    private Environment env;

    public SpringEnvStringDeserializer(Environment env) {
        this.env = env;
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext ctx) throws IOException {
        String result = super.deserialize(parser, ctx);
        return (result != null && !result.trim().equals("")) ? env.resolveRequiredPlaceholders(result) : result;
    }

}
