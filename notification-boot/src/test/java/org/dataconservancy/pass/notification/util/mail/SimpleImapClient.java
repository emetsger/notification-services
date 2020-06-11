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

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.search.MessageIDTerm;
import javax.mail.search.SearchTerm;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.join;
import static java.util.Optional.ofNullable;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class SimpleImapClient implements AutoCloseable {

    final static String TEXT_PLAIN = "text/plain";

    final static String MULTIPART = "multipart";

    final static String MULTIPART_RELATED = MULTIPART + "/related";

    private final static Logger LOG = LoggerFactory.getLogger(SimpleImapClient.class);

    private Session mailSession;

    private IMAPStore store;

    public SimpleImapClient(Session mailSession, IMAPStore store) {
        this.mailSession = mailSession;
        this.store = store;
        LOG.trace("Constructed store '{}@{}' isConnected? '{}'",
                store.getClass().getName(),
                Integer.toHexString(System.identityHashCode(store)),
                store.isConnected());
    }

    /**
     * Searches all Folders in the mail store for messages with the supplied id.
     *
     * @param messageId the SMTP message id
     * @return the matching message
     * @throws MessagingException
     * @throws RuntimeException if the message is not found
     */
    public Message getMessage(String messageId) throws MessagingException {
        // iterate over all folders looking for a matching message

        MessageIDTerm idTerm = new MessageIDTerm(messageId);

        LOG.trace("Store '{}@{}' isConnected? '{}'",
                store.getClass().getName(),
                Integer.toHexString(System.identityHashCode(store)),
                store.isConnected());

        return getFolders().stream().filter(folder -> folder.getName().length() > 0).flatMap(folder -> {
            try {
                LOG.trace("Store '{}@{}' opening folder '{}'",
                        store.getClass().getName(),
                        Integer.toHexString(System.identityHashCode(store)),
                        folder.getName());
                folder.open(Folder.READ_ONLY);
                LOG.trace("Folder {} contains {} messages", folder.getName(), folder.getMessageCount());
                if (LOG.isTraceEnabled()) {
                    Arrays.stream(folder.getMessages()).forEach(msg -> {
                        try {
                            LOG.trace("  \n  Message-ID: {}\n  From: {}\n  To: {}\n  CC: {}\n  BCC: {}\n  Received: {}\n",
                                    join(",", msg.getHeader("Message-ID")),
                                    csvString(Arrays.stream(msg.getFrom())).orElseGet(() -> "<Not Present>"),
                                    csvString(Arrays.stream(msg.getRecipients(Message.RecipientType.TO))).orElseGet(() -> "<Not Present>"),
                                    csvString(Arrays.stream(ofNullable(msg.getRecipients(Message.RecipientType.CC)).orElse(new Address[]{}))).orElseGet(() -> "<Not Present>"),
                                    csvString(Arrays.stream(ofNullable(msg.getRecipients(Message.RecipientType.BCC)).orElse(new Address[]{}))).orElseGet(() -> "<Not Present>"),
                                    msg.getReceivedDate().toInstant().toString());

                        } catch (MessagingException e) {
                            LOG.trace("Unable to list messages in folder: {}", e.getMessage(), e);
                        }
                    });
                }
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

    /**
     * Searches all Folders in the mail store for messages that match the supplied search term.  Note this method does
     * not throw a RuntimeException if no messages are found.
     *
     * @param term the search term
     * @return the matching messages
     * @throws MessagingException
     */
    public Collection<Message> search(SearchTerm term) throws MessagingException {
        // iterate over all folders looking for a matching message
        LOG.trace("Store '{}@{}' isConnected? '{}'",
                store.getClass().getName(),
                Integer.toHexString(System.identityHashCode(store)),
                store.isConnected());

        List<Message> result = getFolders().stream().filter(folder -> folder.getName().length() > 0).flatMap(folder -> {
            try {
                LOG.trace("Store '{}@{}' opening folder '{}'",
                        store.getClass().getName(),
                        Integer.toHexString(System.identityHashCode(store)),
                        folder.getName());

                folder.open(Folder.READ_ONLY);
                LOG.trace("Folder {} contains {} messages", folder.getName(), folder.getMessageCount());
                if (LOG.isTraceEnabled()) {
                    Arrays.stream(folder.getMessages()).forEach(msg -> {
                        try {
                            LOG.trace("  \n  Message-ID: {}\n  From: {}\n  To: {}\n  CC: {}\n  BCC: {}\n  Received: {}\n",
                                    join(",", msg.getHeader("Message-ID")),
                                    csvString(Arrays.stream(msg.getFrom())).orElseGet(() -> "<Not Present>"),
                                    csvString(Arrays.stream(msg.getRecipients(Message.RecipientType.TO))).orElseGet(() -> "<Not Present>"),
                                    csvString(Arrays.stream(ofNullable(msg.getRecipients(Message.RecipientType.CC)).orElse(new Address[]{}))).orElseGet(() -> "<Not Present>"),
                                    csvString(Arrays.stream(ofNullable(msg.getRecipients(Message.RecipientType.BCC)).orElse(new Address[]{}))).orElseGet(() -> "<Not Present>"),
                                    msg.getReceivedDate().toInstant().toString());
                        } catch (MessagingException e) {
                            LOG.trace("Unable to list messages in folder: {}", e.getMessage(), e);
                        }
                    });
                }

                Message[] messages = folder.search(term);
                if (messages != null && messages.length > 0) {
                    LOG.trace("Found {} message(s) in folder {}. First message: {}", messages.length, folder.getName(), messages[0]);
                    // TODO: call folder.close()?
                    return Arrays.stream(messages);
                }
                folder.close();
                return Stream.empty();
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());

        return result;
    }

    @Override
    public void close() throws Exception {
        if (store.isConnected()) {
            LOG.trace("Store '{}@{}' is being closed",
                    store.getClass().getName(),
                    Integer.toHexString(System.identityHashCode(store)));

            store.close();
        }
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

    /**
     * A configuration with a non-existent "${pass.notification.demo.global.cc.address}" property will
     * result in a list with one element, an empty string.  Filter out any empty string values before
     * continuing.
     *
     * @param values stream of values, some of which may be the empty string
     * @return values joined by commas, with any empty values present in {@code values} removed
     */
    private static Optional<String> csvString(Stream<Address> values) {
        String filtered = values
                .filter(v -> v.toString().length() > 0)
                .map(Address::toString)
                .collect(Collectors.joining(","));
        return Optional.of(filtered);
    }

}
