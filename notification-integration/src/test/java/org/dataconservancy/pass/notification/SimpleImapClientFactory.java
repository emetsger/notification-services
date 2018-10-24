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
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.mail.MessagingException;
import javax.mail.Session;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@Configuration
public class SimpleImapClientFactory implements FactoryBean<SimpleImapClient> {

    private IMAPStore imapStore;

    private Session session;

    @Value("${mail.imap.user}")
    private String imapUser;

    @Value("${mail.imap.password}")
    private String imapPass;

    @Autowired
    public SimpleImapClientFactory(IMAPStore imapStore, Session session) {
        this.imapStore = imapStore;
        this.session = session;
    }

    public String getImapUser() {
        return imapUser;
    }

    public void setImapUser(String imapUser) {
        this.imapUser = imapUser;
    }

    public String getImapPass() {
        return imapPass;
    }

    public void setImapPass(String imapPass) {
        this.imapPass = imapPass;
    }

    @Override
    public SimpleImapClient getObject() throws Exception {
        try {
            imapStore.connect(imapUser, imapPass);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        return new SimpleImapClient(session, imapStore);
    }

    @Override
    public Class<?> getObjectType() {
        return SimpleImapClient.class;
    }
}
