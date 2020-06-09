/*
 * Copyright 2020 Johns Hopkins University
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
package org.dataconservancy.pass.notification;

import javax.mail.Session;
import java.util.Properties;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static java.lang.System.getenv;

/**
 * Utility class to support and encapsulate the creation of Java messaging objects.  It is meant to be used when
 * Spring injection is not available, as is the case in the notification-integration module (the module treats
 * notification services as a black box).
 *
 * The configuration of the javax.mail.Session is done using environment variables (preferred) and system properties.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class MailUtil {

    /**
     * Configured using MAIL_IMAP_USER or mail.imap.user.
     */
    private String imapUser = getenv().getOrDefault("MAIL_IMAP_USER",
            getProperty("mail.imap.user", "staffwithnogrants@jhu.edu"));

    /**
     * Configured using MAIL_IMAP_PASSWORD or mail.imap.password.
     */
    private String imapPass = getenv().getOrDefault("MAIL_IMAP_PASSWORD",
            getProperty("mail.imap.password", "moo"));

    /**
     * Configured using MAIL_IMAP_HOST or mail.imap.host.
     */
    private String imapHost = getenv().getOrDefault("MAIL_IMAP_HOST",
            getProperty("mail.imap.host", "localhost"));

    /**
     * Configured using MAIL_IMAP_PORT or mail.imap.port.
     */
    private String imapPort = getenv().getOrDefault("MAIL_IMAP_PORT",
            getProperty("mail.imap.port", "143"));

    /**
     * Configured using MAIL_IMAP_SSL_ENABLE or mail.imap.ssl.enable.
     */
    private boolean useSsl = parseBoolean(getenv().getOrDefault("MAIL_IMAP_SSL_ENABLE",
            getProperty("mail.imap.ssl.enable", "true")));

    /**
     * Configured using MAIL_IMAP_SSL_TRUST or mail.imap.ssl.trust.
     */
    private String sslTrust = getenv().getOrDefault("MAIL_IMAP_SSL_TRUST",
            getProperty("mail.imap.ssl.trust", "*"));

    /**
     * Configured using MAIL_IMAP_STARTTLS_ENABLE or mail.imap.starttls.enable.
     */
    private boolean enableTlsIfSupported = parseBoolean(getenv().getOrDefault("MAIL_IMAP_STARTTLS_ENABLE",
            getProperty("mail.imap.starttls.enable", "true")));

    /**
     * Configured using MAIL_IMAP_FINALIZECLEANCLOSE or mail.imap.finalizecleanclose.
     */
    private boolean closeOnFinalize = parseBoolean(getenv().getOrDefault("MAIL_IMAP_FINALIZECLEANCLOSE",
            getProperty("mail.imap.finalizecleanclose", "true")));

    /**
     * Configured using MAIL_IMAP_CONNECTIONTIMEOUT or mail.imap.connectiontimeout.
     */
    private int connectTimeout = parseInt(getenv().getOrDefault("MAIL_IMAP_CONNECTIONTIMEOUT",
            getProperty("mail.imap.connectiontimeout", "60")));

    /**
     * Configured using MAIL_IMAP_TIMEOUT or mail.imap.timeout.
     */
    private int timeout = parseInt(getenv().getOrDefault("MAIL_IMAP_TIMEOUT",
            getProperty("mail.imap.timeout", "60")));

    /**
     * Answers a configured Session based on the state encapsulated by this instance.
     *
     * @return a configured Session
     */
    Session mailSession() {
        return Session.getDefaultInstance(new Properties() {
            {
                put("mail.imap.host", imapHost);
                put("mail.imap.port", imapPort);
                put("mail.imap.ssl.enable", useSsl);
                put("mail.imap.ssl.trust", sslTrust);
                put("mail.imap.starttls.enable", enableTlsIfSupported);
                put("mail.imap.finalizecleanclose", closeOnFinalize);
                put("mail.imap.connectiontimeout", connectTimeout);
                put("mail.imap.timeout", timeout);
            }
        });
    }

    String getImapUser() {
        return imapUser;
    }

    void setImapUser(String imapUser) {
        this.imapUser = imapUser;
    }

    String getImapPass() {
        return imapPass;
    }

    void setImapPass(String imapPass) {
        this.imapPass = imapPass;
    }

    String getImapHost() {
        return imapHost;
    }

    void setImapHost(String imapHost) {
        this.imapHost = imapHost;
    }

    String getImapPort() {
        return imapPort;
    }

    void setImapPort(String imapPort) {
        this.imapPort = imapPort;
    }

    boolean isUseSsl() {
        return useSsl;
    }

    void setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
    }

    String getSslTrust() {
        return sslTrust;
    }

    void setSslTrust(String sslTrust) {
        this.sslTrust = sslTrust;
    }

    boolean isEnableTlsIfSupported() {
        return enableTlsIfSupported;
    }

    void setEnableTlsIfSupported(boolean enableTlsIfSupported) {
        this.enableTlsIfSupported = enableTlsIfSupported;
    }

    boolean isCloseOnFinalize() {
        return closeOnFinalize;
    }

    void setCloseOnFinalize(boolean closeOnFinalize) {
        this.closeOnFinalize = closeOnFinalize;
    }

    int getConnectTimeout() {
        return connectTimeout;
    }

    void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    int getTimeout() {
        return timeout;
    }

    void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
