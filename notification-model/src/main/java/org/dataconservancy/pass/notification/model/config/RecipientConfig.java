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
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Allows recipients of a notification to be configured depending on the Notification Service {@link #mode mode}.
 * Each Notification Service mode can have its own RecipientConfig, which can be used to control the dispatch of
 * notifications to users during testing or production.
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
     * All notifications for {@link #mode} will be sent to these recipients
     */
    @JsonProperty("global_cc")
    private Collection<String> globalCc;

    /**
     * All notifications for {@link #mode} will be blind carbon copied to these recipients
     */
    @JsonProperty("global_bcc")
    private Collection<String> globalBcc;

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

    /**
     * The Notification Service mode (e.g. "demo", "prod") that this {@code RecipientConfiguration} applies to.
     * <p>
     * This configuration is considered active if the mode matches {@link NotificationConfig#getMode() the runtime
     * mode}.
     * </p>
     *
     * @return the notification services mode this configuration applies to
     */
    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    /**
     * All notifications for {@link #mode} will be sent to these recipients.  Differs from {@link #getGlobalBcc()} as
     * this method does not blind carbon-copy recipients.
     *
     * @return global recipients for dispatched notifications
     */
    public Collection<String> getGlobalCc() {
        return globalCc;
    }

    public void setGlobalCc(Collection<String> globalCc) {
        this.globalCc = globalCc;
    }

    /**
     * All notifications for {@link #mode} will be blind carbon copied to these recipients.  Differs from
     * {@link #getGlobalCc()} as this method blind carbon-copies recipients.
     *
     * @return global, blind, recipients for dispatched notifications
     */
    public Collection<String> getGlobalBcc() {
        return globalBcc;
    }

    public void setGlobalBcc(Collection<String> globalBcc) {
        this.globalBcc = globalBcc;
    }

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
     *
     * @return the whitelist
     */
    public Collection<String> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(Collection<String> whitelist) {
        this.whitelist = whitelist;
    }

    /**
     * All notifications will appear to be delivered from this address
     *
     * @return the from address
     */
    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    @Override
    public String toString() {
        return new StringJoiner("\n  ", RecipientConfig.class.getSimpleName() + "[", "]")
                .add("mode=" + mode)
                .add("globalCc=" + globalCc)
                .add("globalBcc=" + globalBcc)
                .add("whitelist=" + whitelist)
                .add("fromAddress='" + fromAddress + "'")
                .toString();
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
                Objects.equals(globalBcc, that.globalBcc) &&
                Objects.equals(whitelist, that.whitelist) &&
                Objects.equals(fromAddress, that.fromAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, globalCc, globalBcc, whitelist, fromAddress);
    }
}
