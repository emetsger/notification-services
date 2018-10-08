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
package org.dataconservancy.pass.notification.model.config.template;

import org.dataconservancy.pass.notification.model.Notification;
import org.dataconservancy.pass.notification.model.config.AbstractJacksonMappingTest;
import org.junit.Test;

import java.io.IOException;

import static org.dataconservancy.pass.notification.model.config.template.TemplatePrototype.Name.BODY;
import static org.dataconservancy.pass.notification.model.config.template.TemplatePrototype.Name.FOOTER;
import static org.dataconservancy.pass.notification.model.config.template.TemplatePrototype.Name.SUBJECT;
import static org.junit.Assert.assertEquals;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class TemplatePrototypeTest extends AbstractJacksonMappingTest {

    private final String TEMPLATE_JSON = "" +
            "{\n" +
            "        \"notification\": \"SUBMISSION_APPROVAL_INVITE\",\n" +
            "        \"refs\": {\n" +
            "          \"SUBJECT\": \"PASS Submission Approval: ${RESOURCE_METADATA.title}\",\n" +
            "          \"BODY\": \"classpath*:pass-body-submission-approval-invite-template.vm\",\n" +
            "          \"FOOTER\": \"classpath*:pass-footer-template.vm\"\n" +
            "        }\n" +
            "      }";

    @Test
    public void parseJson() throws IOException {
        TemplatePrototype template = mapper.readValue(TEMPLATE_JSON, TemplatePrototype.class);
//        mapper.writer(SerializationFeature.INDENT_OUTPUT).writeValue(System.err, template);
        assertEquals(Notification.Type.SUBMISSION_APPROVAL_INVITE, template.getNotificationType());
        assertEquals(3, template.getRefs().size());
        assertEquals("PASS Submission Approval: ${RESOURCE_METADATA.title}", template.getRefs().get(SUBJECT));
        assertEquals("classpath*:pass-body-submission-approval-invite-template.vm", template.getRefs().get(BODY));
        assertEquals("classpath*:pass-footer-template.vm", template.getRefs().get(FOOTER));
        assertRoundTrip(template, TemplatePrototype.class);
    }
}