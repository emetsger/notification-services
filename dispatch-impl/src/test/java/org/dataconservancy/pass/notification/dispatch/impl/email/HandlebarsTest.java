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

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class HandlebarsTest {

    private Handlebars underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new Handlebars();
    }

    @Test
    public void simpleThis() throws IOException {
        Template t = underTest.compileInline("Hello {{this}}");
        assertEquals("Hello world", t.apply("world"));
    }

    @Test
    public void simpleJson() throws IOException {
        HashMap<String, String> model = new HashMap<String, String>() {
            {
                put("world", "foo");
            }
        };

        Template t = underTest.compileInline("Hello {{world}}");
        assertEquals("Hello foo", t.apply(model));
    }
}
