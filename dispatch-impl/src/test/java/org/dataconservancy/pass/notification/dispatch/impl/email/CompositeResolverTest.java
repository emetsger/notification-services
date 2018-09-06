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

import org.apache.commons.io.input.NullInputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;

import java.util.ArrayList;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CompositeResolverTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private ArrayList<TemplateResolver> resolvers = new ArrayList<>();

    private ArrayList<Exception> exholder = new ArrayList<>();

    private CompositeResolver underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new CompositeResolver(resolvers, exholder);
    }

    @Test
    public void resolutionOrder() {
        TemplateResolver one = mock(TemplateResolver.class);
        TemplateResolver two = mock(TemplateResolver.class);
        TemplateResolver three = mock(TemplateResolver.class);
        resolvers.add(one);
        resolvers.add(two);
        resolvers.add(three);

        String template = "a template";

        when(three.resolve(null, template)).thenReturn(new NullInputStream(-1L));

        underTest.resolve(null, template);

        InOrder orderVerifier = inOrder(one, two, three);

        orderVerifier.verify(one).resolve(null, template);
        orderVerifier.verify(two).resolve(null, template);
        orderVerifier.verify(three).resolve(null, template);
    }

    @Test
    public void exceptionLogging() {
        expectedException.expectMessage("one");
        expectedException.expectMessage("two");

        TemplateResolver one = mock(TemplateResolver.class);
        TemplateResolver two = mock(TemplateResolver.class);

        resolvers.add(one);
        resolvers.add(two);

        String template = "a template";

        when(one.resolve(null, template)).thenThrow(new RuntimeException("one"));
        when(two.resolve(null, template)).thenThrow(new RuntimeException("two"));

        underTest.resolve(null, template);
    }
}