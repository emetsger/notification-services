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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SpringUriTemplateResolverTest {

    private SpringUriTemplateResolver underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new SpringUriTemplateResolver();
    }

    @Test
    public void resolveClasspathUrl() {
        String packageName = this.getClass().getPackage().getName().replace('.', '/');
        assertNotNull(underTest.resolve(null, "classpath:" + packageName + "/" + "templateToResolve"));
    }

    @Test
    public void resolveHttpUrl() {
        assertNotNull(underTest.resolve(null, "http://example.org"));
    }

    @Test
    public void resolveHttpsUrl() {
        assertNotNull(underTest.resolve(null, "https://example.org"));
    }

    @Test
    @Ignore("TODO")
    public void resolveAuthenticatedUrl() {

    }

    @Test
    public void resolveFileUrl() {
        URL u = this.getClass().getResource("templateToResolve");
        assertEquals("file", u.getProtocol());
        assertNotNull(underTest.resolve(null, u.toString()));
    }

    @Test
    public void resolveLocalFile() {
        URL u = this.getClass().getResource("templateToResolve");
        assertEquals("file", u.getProtocol());
        assertNotNull(underTest.resolve(null, u.getPath()));
    }

    @Test(expected = RuntimeException.class)
    public void resolveUnsupportedProtocol() throws MalformedURLException {
        underTest.resolve(null,
                new URL("jar:file:/path/to/file.jar!/path/to/resource").toString());
    }
}