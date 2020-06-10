/*
 *
 *  * Copyright 2018 Johns Hopkins University
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.dataconservancy.pass.notification.model.config;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RecipientConfigTest extends AbstractJacksonMappingTest {

    private static final String RECIPIENT_CONFIG = "" +
            "{\n" +
            "        \"mode\": \"DEMO\",\n" +
            "        \"global_cc\": [\n" +
            "          \"demo@pass.jhu.edu\"\n" +
            "        ],\n" +
            "        \"global_bcc\": [\n" +
            "          \"bccdemo1@pass.jhu.edu\",\n" +
            "          \"bccdemo2@pass.jhu.edu\"\n" +
            "        ],\n" +
            "        \"whitelist\": [\n" +
            "          \"emetsger@jhu.edu\",\n" +
            "          \"hvu@jhu.edu\",\n" +
            "          \"apb@jhu.edu\",\n" +
            "          \"khanson@jhu.edu\"\n" +
            "        ]\n" +
            "      }";

    private static final String RECIPIENT_CONFIG_NULL_WHITELIST = "" +
            "{\n" +
            "        \"mode\": \"DEMO\",\n" +
            "        \"global_cc\": [\n" +
            "          \"demo@pass.jhu.edu\"\n" +
            "        ]\n" +
            "      }";

    private static final String RECIPIENT_CONFIG_EMPTY_WHITELIST = "" +
            "{\n" +
            "        \"mode\": \"DEMO\",\n" +
            "        \"global_cc\": [\n" +
            "          \"demo@pass.jhu.edu\"\n" +
            "        ],\n" +
            "        \"whitelist\": [\n" +
            "        ]\n" +
            "      }";

    @Test
    public void parseJson() throws IOException {
        RecipientConfig config = mapper.readValue(RECIPIENT_CONFIG, RecipientConfig.class);
//        mapper.writer(SerializationFeature.INDENT_OUTPUT).writeValue(System.err, config);
        assertEquals(Mode.DEMO, config.getMode());
        assertEquals(1, config.getGlobalCc().size());
        assertEquals(2, config.getGlobalBcc().size());
        assertEquals(4, config.getWhitelist().size());
        assertTrue(config.getGlobalCc().contains("demo@pass.jhu.edu"));
        assertTrue(config.getWhitelist().contains("apb@jhu.edu"));
        assertTrue(config.getGlobalBcc().contains("bccdemo1@pass.jhu.edu"));
        assertTrue(config.getGlobalBcc().contains("bccdemo2@pass.jhu.edu"));
        assertRoundTrip(config, RecipientConfig.class);
    }

    @Test
    public void parseJsonNullWhitelist() throws IOException {
        RecipientConfig config = mapper.readValue(RECIPIENT_CONFIG_NULL_WHITELIST, RecipientConfig.class);
        assertNull(config.getWhitelist());
    }

    @Test
    public void parseJsonEmptyWhitelist() throws IOException {
        RecipientConfig config = mapper.readValue(RECIPIENT_CONFIG_EMPTY_WHITELIST, RecipientConfig.class);
        assertTrue(config.getWhitelist().isEmpty());
    }
}