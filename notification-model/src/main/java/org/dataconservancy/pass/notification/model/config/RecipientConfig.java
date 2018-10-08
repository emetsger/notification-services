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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Allows recipients of a notification to be configured depending on the Notification Service {@link #mode mode}.
 * Each Notification Service mode can have its own RecipientConfig, which can be used to control the dispatch of
 * notifications to users during testing.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 * @see Mode
 */
public class RecipientConfig {

    /**
     * The Notification Service mode (e.g. "demo", "prod")
     */
    private Mode mode;

    /**
     * All notifications for {@link #mode} will be sent to this recipient
     */
    @JsonProperty("global_cc")
    private Collection<String> globalCc;

    /**
     * Whitelisted recipients for {@link #mode} will receive notifications directly.  If the recipient on the
     * {@code Notification} matches a recipient on the whitelist, then the notification is delivered.
     * <p>
     * If a whitelist does not exist in the configuration, then it is treated as "allow all", or "matching *".  Any
     * recipient configured on the {@code Notification} will have notifications delivered.  A {@code null} whitelist is
     * what would be normally desired in production: each recipient will receive notifications.
     * </p>
     * <p>
     * If a whitelist does exist, then the recipient configured on the {@code Notification} will be used <em>only</em>
     * if they are on the whitelist.  This is a handy mode for demo purposes: only the identified recipients on the
     * whitelist can receive notifications.
     * </p>
     * <p>
     * In all cases, notifications are also sent to {@link #globalCc}.
     * </p>
     */
    private Collection<String> whitelist;

    private String fromAddress;

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Collection<String> getGlobalCc() {
        return globalCc;
    }

    public void setGlobalCc(Collection<String> globalCc) {
        this.globalCc = globalCc;
    }

    public Collection<String> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(Collection<String> whitelist) {
        this.whitelist = whitelist;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RecipientConfig that = (RecipientConfig) o;
        return mode == that.mode &&
                Objects.equals(globalCc, that.globalCc) &&
                Objects.equals(whitelist, that.whitelist) &&
                Objects.equals(fromAddress, that.fromAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, globalCc, whitelist, fromAddress);
    }

    @Override
    public String toString() {
        return "RecipientConfig{" +
                "mode=" + mode +
                ", globalCc=" + globalCc +
                ", whitelist=" + whitelist +
                ", fromAddress='" + fromAddress + '\'' +
                '}';
    }
}
