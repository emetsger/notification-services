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

package org.dataconservancy.pass.notification.dispatch.impl.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import org.dataconservancy.pass.notification.model.Notification;
import org.dataconservancy.pass.notification.model.Notification.Param;
import org.dataconservancy.pass.notification.model.config.template.NotificationTemplate.Name;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class HandlebarsParameterizerTest {

    private static final String BODY_TEMPLATE = "" +
            "Dear {{to}},\n" +
            "\n" +
            "A submission titled \"{{#resource_metadata}}{{title}}{{/resource_metadata}}\" been prepared on your behalf by {{from}} {{#event_metadata}}{{#if comment}}with comment \"{{comment}}\"{{else}}.{{/if}}{{/event_metadata}}\n" +
            "\n" +
            "Please review the submission at the following URL:\n" +
            "{{#each link_metadata}}{{#eq rel \"submissionReview\"}}{{href}}{{else}}{{/eq}}{{/each}}";

    private static final String SUBJECT_TEMPLATE = "PASS Submission titled \"{{#resource_metadata}}{{title}}{{/resource_metadata}}\" awaiting your approval";

    private static final String COMMENT_STRING = "How does this look?";

    private static final String EVENT_METADATA = "" +
            "{\n" +
            "  \"comment\": \"" + COMMENT_STRING + "\"\n" +
            "}";

    private static final String ARTICLE_TITLE = "Article title";

    private static final String RESOURCE_METADATA = "" +
            "{\n" +
            "  \"title\": \"" + ARTICLE_TITLE + "\"\n" +
            "}";

    private static final String SUBMISSION_REVIEW_LINK = "https://pass.jhu.edu/app/submission/abc123";

    private static final String LINK_METADATA = "" +
            "[\n" +
            "  {\n" +
            "    \"rel\": \"submissionReview\",\n" +
            "    \"href\": \"" + SUBMISSION_REVIEW_LINK + "\"\n" +
            "  }\n" +
            "]";

    private static final String FROM = "preparer@jhu.edu";

    private static final String TO = "authorized-submitter@jhu.edu";

    private Map<Notification.Param, String> paramMap;

    private HandlebarsParameterizer underTest;

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        paramMap = new HashMap<>();
        paramMap.put(Param.TO, TO);
        paramMap.put(Param.FROM, FROM);
        paramMap.put(Param.RESOURCE_METADATA, RESOURCE_METADATA);
        paramMap.put(Param.EVENT_METADATA, EVENT_METADATA);
        paramMap.put(Param.LINKS, LINK_METADATA);
        Handlebars handlebars = new Handlebars();
        handlebars.registerHelper("eq", ConditionalHelpers.eq);
        underTest = new HandlebarsParameterizer(handlebars, mapper);
    }

    @Test
    public void simpleParameterization() throws IOException {
        String parameterized = underTest.parameterize(Name.BODY, paramMap, new ByteArrayInputStream(BODY_TEMPLATE.getBytes()));

        assertTrue(parameterized.contains(TO));
        assertTrue(parameterized.contains(FROM));
        assertTrue(parameterized.contains(COMMENT_STRING));
        assertTrue(parameterized.contains(SUBMISSION_REVIEW_LINK));
        assertTrue(parameterized.contains(ARTICLE_TITLE));
    }
}