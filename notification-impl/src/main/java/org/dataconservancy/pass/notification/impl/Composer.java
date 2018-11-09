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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dataconservancy.pass.model.Submission;
import org.dataconservancy.pass.model.SubmissionEvent;
import org.dataconservancy.pass.notification.model.Notification;
import org.dataconservancy.pass.notification.model.SimpleNotification;
import org.dataconservancy.pass.notification.model.config.NotificationConfig;
import org.dataconservancy.pass.notification.model.config.RecipientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static java.lang.String.join;
import static org.dataconservancy.pass.notification.impl.Composer.RecipientConfigFilter.modeFilter;
import static org.dataconservancy.pass.notification.impl.Links.concat;
import static org.dataconservancy.pass.notification.impl.Links.serialized;

/**
 * Composes a {@link Notification} from a {@link SubmissionEvent} and its corresponding {@link Submission}, according
 * to a {@link RecipientConfig}.  Responsible for determining the type of notification, and who the recipients and
 * sender of the notification are.  It is also responsible for populating other parameters of the notification such as
 * resource metadata, link metadata, or event metadata.
 * <p>
 * The implementation applies a whitelist to the recipients of the notification according to the recipient
 * configuration.  If the recipient configuration has a null or empty whitelist, that means that *all* recipients are
 * whitelisted (each recipient will receive the notification).  If the recipient configuration has a non-empty
 * whitelist, then only those users specified in the whitelist will receive a notification.  (N.B. the global CC field
 * of the recipient configuration is not run through the whitelist).
 * </p>
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 * @see RecipientConfig
 * @see Notification.Type
 * @see Notification.Param
 */
public class Composer implements BiFunction<Submission, SubmissionEvent, Notification> {

    private static final Logger LOG = LoggerFactory.getLogger(Composer.class);

    private RecipientConfig recipientConfig;

    private RecipientAnalyzer recipientAnalyzer;
    
    private SubmissionLinkAnalyzer submissionLinkAnalyzer;
    
    private LinkValidator linkValidator;

    private ObjectMapper mapper;

    public Composer(NotificationConfig config, ObjectMapper mapper) {
        this.mapper = mapper;
        Objects.requireNonNull(config, "NotificationConfig must not be null.");
        recipientConfig = getRecipientConfig(config);
        recipientAnalyzer = new RecipientAnalyzer();
        submissionLinkAnalyzer = new SubmissionLinkAnalyzer(new UserTokenGenerator(config));
        linkValidator = new LinkValidator(config);
    }

    public Composer(NotificationConfig config, RecipientAnalyzer recipientAnalyzer,
            SubmissionLinkAnalyzer submissionLinkAnalyzer, LinkValidator linkValidator, ObjectMapper mapper) {
        this(config, mapper);
        Objects.requireNonNull(config, "RecipientAnalyzer must not be null.");
        this.recipientAnalyzer = recipientAnalyzer;
        this.submissionLinkAnalyzer = submissionLinkAnalyzer;
        this.linkValidator = linkValidator;
    }

    /**
     * Composes a {@code Notification} from a {@code Submission} and {@code SubmissionEvent}.
     *
     * @param submission
     * @param event
     * @return
     */
    @Override
    public Notification apply(Submission submission, SubmissionEvent event) {
        Objects.requireNonNull(submission, "Submission must not be null.");
        Objects.requireNonNull(event, "Event must not be null.");

        if (!event.getSubmission().equals(submission.getId())) {
            // TODO: exception?
            LOG.warn("Composing a Notification for tuple [{},{}] but {} references a different Submission: {}.",
                    submission.getId(), event.getId(), event.getId(), event.getSubmission());
        }

        SimpleNotification notification = new SimpleNotification();
        HashMap<Notification.Param, String> params = new HashMap<>();
        notification.setParameters(params);

        notification.setEventUri(event.getId());
        params.put(Notification.Param.EVENT_METADATA, eventMetadata(event, mapper));

        Collection<String> cc = recipientConfig.getGlobalCc();
        if (cc != null && !cc.isEmpty()) {
            notification.setCc(cc);
            params.put(Notification.Param.CC, join(",", cc));
        }

        notification.setResourceUri(submission.getId());
        params.put(Notification.Param.RESOURCE_METADATA, resourceMetadata(submission, mapper));

        String from = recipientConfig.getFromAddress();
        notification.setSender(from);
        params.put(Notification.Param.FROM, from);

        Collection<String> recipients = recipientAnalyzer.apply(submission, event);
        notification.setRecipient(recipients);
        params.put(Notification.Param.TO, join(",", recipients));
        
        params.put(Notification.Param.LINKS, concat(submissionLinkAnalyzer.apply(submission, event))
                .filter(linkValidator)
                .collect(serialized()));

        switch (event.getEventType()) {
            case APPROVAL_REQUESTED_NEWUSER: {
                notification.setType(Notification.Type.SUBMISSION_APPROVAL_INVITE);
                break;
            }

            case APPROVAL_REQUESTED: {
                notification.setType(Notification.Type.SUBMISSION_APPROVAL_REQUESTED);
                break;
            }

            case CHANGES_REQUESTED: {
                notification.setType(Notification.Type.SUBMISSION_CHANGES_REQUESTED);
                break;
            }

            case SUBMITTED: {
                notification.setType(Notification.Type.SUBMISSION_SUBMISSION_SUBMITTED);
                break;
            }

            case CANCELLED: {
                notification.setType(Notification.Type.SUBMISSION_SUBMISSION_CANCELLED);
                break;
            }

            default: {
                throw new RuntimeException("Unknown SubmissionEvent type '" + event.getEventType() + "'");
            }
        }

        return notification;
    }

    RecipientConfig getRecipientConfig() {
        return recipientConfig;
    }

    void setRecipientConfig(RecipientConfig recipientConfig) {
        this.recipientConfig = recipientConfig;
    }

    RecipientAnalyzer getRecipientAnalyzer() {
        return recipientAnalyzer;
    }

    void setRecipientAnalyzer(RecipientAnalyzer recipientAnalyzer) {
        this.recipientAnalyzer = recipientAnalyzer;
    }

    static RecipientConfig getRecipientConfig(NotificationConfig config) {
        return config.getRecipientConfigs().stream()
                .filter(modeFilter(config)).findAny()
                .orElseThrow(() ->
                        new RuntimeException("Missing recipient configuration for Mode '" + config.getMode() + "'"));
    }

    public static String resourceMetadata(Submission submission, ObjectMapper mapper) {
        String metadata = submission.getMetadata();
        if (metadata == null || metadata.trim().length() == 0) {
            return "{}";
        }
        JsonNode metadataNode = null;
        try {
            metadataNode = mapper.readTree(metadata);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ObjectNode resourceMetadata = mapper.createObjectNode();
        resourceMetadata.put("title", field("title", metadataNode).orElse(""));
        resourceMetadata.put("journal-title", field("journal-title", metadataNode).orElse(""));
        resourceMetadata.put("volume", field("volume", metadataNode).orElse(""));
        resourceMetadata.put("issue", field("issue", metadataNode).orElse(""));
        resourceMetadata.put("abstract", field("abstract", metadataNode).orElse(""));
        resourceMetadata.put("doi", field("doi", metadataNode).orElse(""));
        resourceMetadata.put("publisher", field("publisher", metadataNode).orElse(""));
        resourceMetadata.put("authors", field("authors", metadataNode).orElse(""));
        return resourceMetadata.toString();
    }

    public static String eventMetadata(SubmissionEvent event, ObjectMapper mapper) {
        ObjectNode eventMetadata = mapper.createObjectNode();
        eventMetadata.put("id", field("id", event).orElse(""));
        eventMetadata.put("comment", field("comment", event).orElse(""));
        eventMetadata.put("performedDate", field("performedDate", event).orElse(""));
        eventMetadata.put("performedBy", field("performedBy", event).orElse(""));
        eventMetadata.put("performerRole", field("performerRole", event).orElse(""));
        eventMetadata.put("performedDate", field("performedDate", event).orElse(""));
        eventMetadata.put("eventType", field("eventType", event).orElse(""));
        return eventMetadata.toString();
    }

    static class RecipientConfigFilter {

        /**
         * Selects the correct {@link RecipientConfig} for the current {@link NotificationConfig#mode mode} of Notification Services.
         * @param config the Notification Services runtime configuration
         * @return the current mode's {@code RecipientConfig}
         */
        static Predicate<RecipientConfig> modeFilter(NotificationConfig config) {
            return (rc) -> config.getMode() == rc.getMode();
        }

    }

    static Optional<String> field(String fieldname, JsonNode metadata) {
        Optional<JsonNode> node = Optional.ofNullable(metadata.findValue(fieldname));
        if (node.isPresent() && node.get().isArray()) {
            return node.map(Objects::toString);
        }
        return node.map(JsonNode::asText);
    }

    static Optional<String> field(String fieldname, SubmissionEvent event) {
        Optional<Field> field = Optional.ofNullable(ReflectionUtils.findField(SubmissionEvent.class, fieldname));
        return field
                .map(f -> {
                    ReflectionUtils.makeAccessible(f);
                    return f;
                })
                .map(f -> ReflectionUtils.getField(f, event))
                .map(Object::toString);
    }
}
