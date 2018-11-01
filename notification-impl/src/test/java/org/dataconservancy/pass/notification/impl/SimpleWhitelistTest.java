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
package org.dataconservancy.pass.notification.impl;

import org.dataconservancy.pass.notification.model.config.RecipientConfig;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class SimpleWhitelistTest {

    private SimpleWhitelist underTest;

    private Collection<String> whitelist;

    @Before
    public void setUp() throws Exception {
        whitelist = new ArrayList<>();
        RecipientConfig recipientConfig = mock(RecipientConfig.class);
        when(recipientConfig.getWhitelist()).thenReturn(whitelist);
        underTest = new SimpleWhitelist(recipientConfig);
    }

    /**
     * when the whitelist is empty, then *all* recipients are whitelisted
     */
    @Test
    public void testEmptyWhitelist() {
        assertEquals("foo@bar.baz", underTest.apply(singletonList("foo@bar.baz")).iterator().next());
    }

    /**
     * when the candidate recipient doesn't match an address in the whitelist, the resulting list is empty.
     */
    @Test
    public void testWhitelistNoMatch() {
        String candidate1 = "foo@bar.baz";
        String whitelist1 = "joyof@physics.com";
        whitelist.add(whitelist1);
        Collection<String> result = underTest.apply(singletonList(candidate1));
        assertEquals(0, result.size());
    }

    /**
     * Candidate recipient address: case should not matter
     */
    @Test
    public void testCandidateCaseSensitivity() {
        String candidate1 = "FOO@bar.baz";
        String whitelist1 = "foo@bar.baz";
        whitelist.add(whitelist1);
        Collection<String> result = underTest.apply(singletonList(candidate1));
        assertEquals(1, result.size());
        assertEquals(candidate1, result.iterator().next());
    }

    /**
     * Whitelisted recipients: case should not matter
     */
    @Test
    public void testWhitelistCaseSensitivity() {
        String candidate1 = "foo@bar.baz";
        String whitelist1 = "FOO@bar.baz";
        whitelist.add(whitelist1);
        Collection<String> result = underTest.apply(singletonList(candidate1));
        assertEquals(1, result.size());
        assertEquals(candidate1, result.iterator().next());
    }

    /**
     * Test when a single candidate matches a whitelisted address
     */
    @Test
    public void testWhiteListWithOneAddress() {
        String candidate = "foo@bar.baz";
        whitelist.add(candidate);
        Collection<String> result = underTest.apply(singletonList(candidate));
        assertEquals(1, result.size());
        assertEquals(candidate, result.iterator().next());
    }

    /**
     * Test when a single candidate matches a whitelist containing multiple addresses
     */
    @Test
    public void testWhiteListWithTwoAddresses() {
        String candidate1 = "foo@bar.baz";
        String candidate2 = "biz@bar.com";
        whitelist.add(candidate1);
        whitelist.add(candidate2);

        Collection<String> result = underTest.apply(singletonList(candidate1));
        assertEquals(1, result.size());
        assertEquals(candidate1, result.iterator().next());
    }

    /**
     * test when multiple candidates match a whitelist containing multiple addresses
     */
    @Test
    public void testWhitelistWithMultipleCandidates() {
        String candidate1 = "foo@bar.baz";
        String candidate2 = "biz@bar.com";
        String whitelist3 = "bark@dog.com";
        String whitelist4 = "foo@biz.com";
        whitelist.add(candidate1);
        whitelist.add(candidate2);
        whitelist.add(whitelist3);
        whitelist.add(whitelist4);


        Collection<String> result = underTest.apply(Arrays.asList(candidate1, candidate2));
        assertEquals(2, result.size());
        assertTrue(result.contains(candidate1));
        assertTrue(result.contains(candidate2));
    }

    @Test
    public void testNullCandidate() {
        assertTrue(underTest.apply(null).isEmpty());
    }

    @Test
    public void testEmptyCandidate() {
        assertTrue(underTest.apply(Collections.emptyList()).isEmpty());
    }
}