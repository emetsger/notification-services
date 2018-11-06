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
package org.dataconservancy.pass.notification.model.config.smtp;

import java.util.Objects;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class SmtpServerConfig {

    private String host;

    private String port;

    private String smtpUser;

    private String smtpPassword;

    private String smtpTransport;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getSmtpUser() {
        return smtpUser;
    }

    public void setSmtpUser(String smtpUser) {
        this.smtpUser = smtpUser;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public String getSmtpTransport() {
        return smtpTransport;
    }

    public void setSmtpTransport(String smtpTransport) {
        this.smtpTransport = smtpTransport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SmtpServerConfig that = (SmtpServerConfig) o;
        return Objects.equals(host, that.host) &&
                Objects.equals(port, that.port) &&
                Objects.equals(smtpUser, that.smtpUser) &&
                Objects.equals(smtpPassword, that.smtpPassword) &&
                Objects.equals(smtpTransport, that.smtpTransport);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, smtpUser, smtpPassword, smtpTransport);
    }
}
