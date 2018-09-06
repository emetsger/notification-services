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
package org.dataconservancy.pass.notification.model.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.dataconservancy.pass.notification.model.Notification;
import org.dataconservancy.pass.notification.model.config.template.TemplatePrototype;
import org.junit.Test;

import java.io.IOException;

import static org.dataconservancy.pass.notification.model.Notification.Type.SUBMISSION_APPROVAL_INVITE;
import static org.dataconservancy.pass.notification.model.config.Mode.DEMO;
import static org.dataconservancy.pass.notification.model.config.Mode.PRODUCTION;
import static org.junit.Assert.*;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class NotificationConfigTest extends AbstractJacksonMappingTest {

    private static final String MINIMAL_NOTIFICATION_CONFIG = "" +
            "{\n" +
            "    \"mode\": \"DEMO\",\n" +
            "    \"recipient-config\": [\n" +
            "      {\n" +
            "        \"mode\": \"PRODUCTION\",\n" +
            "        \"global_cc\": [\n" +
            "          \"pass@pass.jhu.edu\"\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"mode\": \"DEMO\",\n" +
            "        \"global_cc\": [\n" +
            "          \"demo@pass.jhu.edu\"\n" +
            "        ],\n" +
            "        \"whitelist\": [\n" +
            "          \"emetsger@jhu.edu\",\n" +
            "          \"hvu@jhu.edu\",\n" +
            "          \"apb@jhu.edu\",\n" +
            "          \"khanson@jhu.edu\"\n" +
            "        ]\n" +
            "      }\n" +
            "    ],\n" +
            "    \"templates\": [\n" +
            "      {\n" +
            "        \"notification\": \"SUBMISSION_APPROVAL_INVITE\",\n" +
            "        \"refs\": {\n" +
            "          \"SUBJECT\": \"PASS Submission Approval: ${RESOURCE_METADATA.title}\",\n" +
            "          \"BODY\": \"classpath*:pass-body-submission-approval-invite-template.vm\",\n" +
            "          \"FOOTER\": \"classpath*:pass-footer-template.vm\"\n" +
            "        }\n" +
            "      }\n" +
            "    ],\n" +
            "    \"smtp\": {\n" +
            "      \"host\": \"smtp.gmail.com\",\n" +
            "      \"port\": \"587\",\n" +
            "      \"smtpUser\": \"foo\",\n" +
            "      \"smtpPassword\": \"bar\"\n" +
            "    }\n" +
            "}";

    @Test
    public void parseJson() throws IOException {
        NotificationConfig config = mapper.readValue(MINIMAL_NOTIFICATION_CONFIG, NotificationConfig.class);
//        mapper.writer(SerializationFeature.INDENT_OUTPUT).writeValue(System.err, config);
        assertEquals(DEMO, config.getMode());
        assertEquals(2, config.getRecipientConfigs().size());
        config.getRecipientConfigs()
                .stream().filter(rc -> rc.getMode() == PRODUCTION).findAny()
                .orElseThrow(() -> new RuntimeException("Missing RecipientConfig for mode PRODUCTION."));
        config.getRecipientConfigs()
                .stream().filter(rc -> rc.getMode() == DEMO).findAny()
                .orElseThrow(() -> new RuntimeException("Missing RecipientConfig for mode DEMO."));
        assertEquals(1, config.getTemplates().size());
        config.getTemplates()
                .stream().filter(tc -> tc.getNotificationType() == SUBMISSION_APPROVAL_INVITE).findAny()
                .orElseThrow(() -> new RuntimeException("Missing TemplatePrototype for type SUBMISSION_APPROVAL_INVITE"));
        assertNotNull(config.getSmtpConfig());
        assertEquals("smtp.gmail.com", config.getSmtpConfig().getHost());
        assertRoundTrip(config, NotificationConfig.class);
    }
}