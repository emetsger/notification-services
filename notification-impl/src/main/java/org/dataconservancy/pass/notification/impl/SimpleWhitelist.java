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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Filters a list of notification recipients against a whitelist.
 * <p>
 * Provided a collection notification recipients, this function will determine which recipients are eligible for receiving the notification (i.e. whitelisted recipients), and return a collection containing the whitelisted addresses.
 * </p>
 * <p>
 * The whitelist is configured in the notification services {@link RecipientConfig recipient configuration}.  If the configured whitelist is empty, then all notification recipients are considered to be whitelisted.
 * </p>
 * <p>
 * A practical configuration is to use an empty whitelist in production, but provide a whitelist when performing testing or demonstrations, preventing notifications from being sent to recipients who didn't request them.
 * </p>
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class SimpleWhitelist implements Function<Collection<String>, Collection<String>> {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleWhitelist.class);

    private RecipientConfig recipientConfig;

    public SimpleWhitelist(RecipientConfig recipientConfig) {
        this.recipientConfig = recipientConfig;
    }

    @Override
    public Collection<String> apply(Collection<String> candidates) {
        // if the supplied candidate is null, then no recipient will be allowed
        if (candidates == null) {
            LOG.debug("No recipient to whitelist: supplied candidate email address was null.");
            return Collections.emptyList();
        }

        // an empty or null whitelist is carries the semantics "any recipient is whitelisted"
        if (recipientConfig.getWhitelist() == null || recipientConfig.getWhitelist().isEmpty()) {
            LOG.debug("Any recipient will be whitelisted: the whitelist is empty.");
            return candidates;
        }

        return candidates.stream()
                .filter(candidate -> {
                    boolean isWhitelisted = isWhitelisted(candidate.toLowerCase(), recipientConfig.getWhitelist());
                    LOG.debug("{} is whitelisted: {}", candidate, isWhitelisted);
                    return isWhitelisted;
                })
                .collect(Collectors.toSet());
    }

    private static boolean isWhitelisted(String candidate, Collection<String> whitelist) {
        // an empty or null whitelist is carries the semantics "any recipient is whitelisted"
        if (whitelist == null || whitelist.isEmpty()) {
            return true;
        }

        return whitelist.stream().map(String::toLowerCase).anyMatch(candidate::equals);
    }
}
