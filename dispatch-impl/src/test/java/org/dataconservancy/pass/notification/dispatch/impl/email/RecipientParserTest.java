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

import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.model.User;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;

import static org.dataconservancy.pass.notification.dispatch.impl.email.RecipientParser.parseRecipientUris;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RecipientParserTest {

    private URI mailtoUri = URI.create("mailto:John%20Doe%3Cjohndoe%40example.org%3E");

    private URI mailtoUriWithParams = URI.create("mailto:John%20Doe%3Cjohndoe%40example.org%3E?subject=hello");

    private URI userUri = URI.create("https://pass.jhu.edu/fcrepo/users/123456");

    private PassClient passClient;

    @Before
    public void setUp() throws Exception {
        passClient = mock(PassClient.class);
    }

    @Test
    public void testParseMailtoUri() {
        assertEquals("John Doe<johndoe@example.org>",
                parseRecipientUris(Collections.singleton(mailtoUri), passClient).iterator().next());
    }

    @Test
    public void testParseMailtoUriWithParams() {
        assertEquals("John Doe<johndoe@example.org>",
                parseRecipientUris(Collections.singleton(mailtoUriWithParams), passClient).iterator().next());
    }

    @Test
    public void testParsePassUserUri() {
        String expectedEmail = "user@example.org";
        User u = mock(User.class);
        when(u.getEmail()).thenReturn(expectedEmail);

        when(passClient.readResource(userUri, User.class)).thenReturn(u);

        assertEquals(expectedEmail,
                parseRecipientUris(Collections.singleton(userUri), passClient).iterator().next());

        verify(u).getEmail();
    }
}