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
package org.dataconservancy.pass.notification.util.mail;

import com.sun.mail.imap.IMAPStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.search.MessageIDTerm;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class SimpleImapClient {

    final static String TEXT_PLAIN = "text/plain";

    final static String MULTIPART = "multipart";

    final static String MULTIPART_RELATED = MULTIPART + "/related";

    private final static Logger LOG = LoggerFactory.getLogger(SimpleImapClient.class);

    private Session mailSession;

    private IMAPStore store;

    public SimpleImapClient(Session mailSession, IMAPStore store) {
        this.mailSession = mailSession;
        this.store = store;
    }

    /**
     * Searches all Folders in the mail store for messages with the supplied id.
     *
     * @param messageId
     * @return
     * @throws MessagingException
     */
    public Message getMessage(String messageId) throws MessagingException {
        // iterate over all folders looking for a matching message

        MessageIDTerm idTerm = new MessageIDTerm(messageId);

        return getFolders().stream().filter(folder -> folder.getName().length() > 0).flatMap(folder -> {
            try {
                LOG.trace("Opening folder '{}'", folder.getName());
                folder.open(Folder.READ_ONLY);
                Message[] messages = folder.search(idTerm);
                if (messages != null && messages.length > 0) {
                    return Arrays.stream(messages);
                }
                folder.close();
                return Stream.empty();
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }).findAny().orElseThrow(() -> new RuntimeException("Message '" + messageId + "' not found in any folder."));
    }

    public static String getBodyAsText(Message message) throws IOException, MessagingException {
        LOG.trace("Parsing message with Content-Type {}", message.getContentType());
        return getNestedTextPlainPart((Multipart) message.getContent());
    }

    private static String getNestedTextPlainPart(Multipart mp) throws MessagingException, IOException {
        for (int i = 0; i < mp.getCount(); i++) {
            BodyPart part = mp.getBodyPart(i);
            LOG.trace("Parsing BodyPart {} Content-Type {}", i, part.getContentType());
            if (part.getContentType().startsWith(TEXT_PLAIN)) {
                return (String) part.getContent();
            }

            if (part.getContentType().startsWith(MULTIPART)) {
                LOG.trace("Recursively processing BodyPart {} Content-Type {}", i, part.getContentType());
                return getNestedTextPlainPart((Multipart) part.getContent());
            }
        }

        return null;
    }

    private Set<Folder> getFolders() throws MessagingException {
        return Arrays.stream(store.getDefaultFolder().list()).collect(Collectors.toSet());
    }

}
