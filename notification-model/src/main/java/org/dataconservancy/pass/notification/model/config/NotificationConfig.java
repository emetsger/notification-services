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
import org.dataconservancy.pass.notification.model.Notification;
import org.dataconservancy.pass.notification.model.config.smtp.SmtpServerConfig;
import org.dataconservancy.pass.notification.model.config.template.NotificationTemplate;

import java.util.Collection;
import java.util.Objects;

/**
 * The Notification Services runtime configuration.
 * <p>
 * The configuration is parsed from a JSON resource identified by the environment variable or system property {@code pass.notification.configuration}.  The resource must be identified as a Spring Resource URI (e.g. {@code classpath:/notification.json}, {@code file:///notification.json}).
 * </p>
 *
 * @see <a href="https://docs.spring.io/spring/docs/5.1.1.RELEASE/spring-framework-reference/core.html#resources">Spring Resources</a>
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class NotificationConfig {

    /**
     * Runtime mode of Notification Services (e.g. "disabled", "demo", "production")
     */
    public Mode mode;

    /**
     * Each Notification type has a set of templates
     */
    private Collection<NotificationTemplate> templates;

    /**
     * Each Notification Service mode has a recipientConfig
     */
    @JsonProperty("recipient-config")
    private Collection<RecipientConfig> recipientConfigs;

    /**
     * Global Notification Service SMTP server configuration
     */
    @JsonProperty("smtp")
    private SmtpServerConfig smtpConfig;
    
    /** 
     * User invitation token encryption key.
     */
    @JsonProperty("user-token-generator")
    private UserTokenGeneratorConfig tokenConfig;
    
    @JsonProperty("link-validators")
    private Collection<LinkValidationRule> linkValidatorConfig;

    /**
     * The runtime mode of Notification Services.  Portions of the configuration may be differentiated by mode, for example, there can be a {@link RecipientConfig} per {@link Mode}.
     * <p>
     * The mode may be set in the configuration file, or set with an environment or system property variable named {@code pass.notification.mode}.
     * </p>
     *
     * @return the runtime mode of Notification Services
     */
    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    /**
     * The email templates used for each {@link Notification#getType() notification type}.
     *
     * @return the Collection of email templates, one template per notification type
     */
    public Collection<NotificationTemplate> getTemplates() {
        return templates;
    }

    public void setTemplates(Collection<NotificationTemplate> templates) {
        this.templates = templates;
    }

    /**
     * The recipient configuration used for each {@link Mode} of Notification Services.  The active recipient configuration will have a {@link RecipientConfig#getMode() mode} equal to {@link #getMode()}.
     *
     * @return the Collection of recipient configurations, one for each supported Notification Services mode
     */
    public Collection<RecipientConfig> getRecipientConfigs() {
        return recipientConfigs;
    }

    public void setRecipientConfigs(Collection<RecipientConfig> recipientConfigs) {
        this.recipientConfigs = recipientConfigs;
    }

    /**
     * The settings used to send email notifications using SMTP.  Unlike other portions of this configuration, it is <em>not</em> a function of {@link #getMode() the runtime mode}.
     *
     * @return the global SMTP server configuration
     */
    public SmtpServerConfig getSmtpConfig() {
        return smtpConfig;
    }

    public void setSmtpConfig(SmtpServerConfig smtpConfig) {
        this.smtpConfig = smtpConfig;
    }
    
    public UserTokenGeneratorConfig getUserTokenGeneratorConfig() {
        return tokenConfig;
    }
    
    public void setUserTokenGeneratorConfig(UserTokenGeneratorConfig config) {
        this.tokenConfig = config;
    }
    
    public Collection<LinkValidationRule> getLinkValidatorConfigs() {
        return linkValidatorConfig;
    }
    
    public void setLinkValidationRules(Collection<LinkValidationRule> configs) {
        this.linkValidatorConfig = configs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NotificationConfig that = (NotificationConfig) o;
        return Objects.equals(mode, that.mode) &&
                Objects.equals(templates, that.templates) &&
                Objects.equals(recipientConfigs, that.recipientConfigs) &&
                Objects.equals(smtpConfig, that.smtpConfig) &&
                Objects.equals(tokenConfig, that.tokenConfig) &&
                Objects.equals(linkValidatorConfig, that.linkValidatorConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, templates, recipientConfigs, smtpConfig, tokenConfig, linkValidatorConfig);
    }

    @Override
    public String toString() {
        return "NotificationConfig{" +
                "mode='" + mode + '\'' +
                ", templates=" + templates +
                ", recipientConfigs=" + recipientConfigs +
                ", smtpConfig=" + smtpConfig +
                ", tokenConfig=" + tokenConfig +
                ", linkValidatorCOnfig=" + linkValidatorConfig +
                '}';
    }
}
