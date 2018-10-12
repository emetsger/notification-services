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
package org.dataconservancy.pass.notification;

import com.sun.mail.imap.IMAPStore;
import org.dataconservancy.pass.notification.util.mail.SimpleImapClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.mail.MessagingException;
import javax.mail.Session;
import java.util.Properties;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@Configuration
public class SpringBootIntegrationConfig {

    @Value("${mail.imap.user}")
    private String imapUser;

    @Value("${mail.imap.password}")
    private String imapPass;

    @Value("${mail.imap.host}")
    private String imapHost;

    @Value("${mail.imap.port}")
    private String imapPort;

    @Value("${mail.imap.ssl.enable}")
    private boolean useSsl;

    @Value("${mail.imap.ssl.trust}")
    private String sslTrust;

    @Value("${mail.imap.starttls.enable}")
    private boolean enableTlsIfSupported;

    @Value("${mail.imap.finalizecleanclose}")
    private boolean closeOnFinalize;

    @Bean
    public Session mailSession() {
        return Session.getDefaultInstance(new Properties() {
            {
                put("mail.imap.user", imapUser);
                put("mail.imap.host", imapHost);
                put("mail.imap.port", imapPort);
                put("mail.imap.ssl.enable", useSsl);
                put("mail.imap.ssl.trust", sslTrust);
                put("mail.imap.starttls.enable", enableTlsIfSupported);
                put("mail.imap.finalizecleanclose", closeOnFinalize);
            }
        });
    }

    @Bean
    public IMAPStore imapStore(Session session) {
        try {
            IMAPStore store = (IMAPStore)session.getStore("imap");
            store.connect(imapUser, imapPass);
            return store;
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public SimpleImapClient imapClient(Session session, IMAPStore store) {
        return new SimpleImapClient(session, store);
    }
}
