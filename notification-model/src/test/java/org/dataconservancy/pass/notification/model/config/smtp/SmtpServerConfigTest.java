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
package org.dataconservancy.pass.notification.model.config.smtp;

import org.dataconservancy.pass.notification.model.config.AbstractJacksonMappingTest;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class SmtpServerConfigTest extends AbstractJacksonMappingTest {

    private static final String SMTP_CONFIG_JSON = "" +
            "{\n" +
            "      \"host\": \"smtp.gmail.com\",\n" +
            "      \"port\": \"587\",\n" +
            "      \"smtpUser\": \"foo\",\n" +
            "      \"smtpPassword\": \"bar\",\n" +
            "      \"smtpTransport\": \"SMTP\"\n" +
            "    }";


    @Test
    public void parseJson() throws IOException {
        SmtpServerConfig config = mapper.readValue(SMTP_CONFIG_JSON, SmtpServerConfig.class);
        assertEquals("smtp.gmail.com", config.getHost());
        assertEquals("587", config.getPort());
        assertEquals("foo", config.getSmtpUser());
        assertEquals("bar", config.getSmtpPassword());
        assertEquals("SMTP", config.getSmtpTransport());
        assertRoundTrip(config, SmtpServerConfig.class);
    }

}