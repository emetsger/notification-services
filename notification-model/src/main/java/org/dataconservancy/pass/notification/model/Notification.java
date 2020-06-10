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
package org.dataconservancy.pass.notification.model;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

/**
 * Represents a Notification that is to be dispatched to a set of recipients.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public interface Notification {

    /**
     * Type of Notification
     * TODO: see how these line up with SubmissionEvent, and potentially use the SubmissionEvent name as the notification type.
     */
    enum Type {

        /**
         * Preparer has requested approval of a Submission by an Authorized Submitter
         */
        SUBMISSION_APPROVAL_REQUESTED,

        /**
         * Preparer has requested approval of a Submission by an Authorized Submitter who does not have a {@code User}
         * in PASS.  The notification will include an invitation to join PASS (and upon login, a {@code User} created
         * for the Authorized Submitter)
         */
        SUBMISSION_APPROVAL_INVITE,

        /**
         * Authorized Submitter has requested changes to the Submission by the Preparer.
         */
        SUBMISSION_CHANGES_REQUESTED,

        /**
         * Submission was submitted by the Authorized Submitter
         */
        SUBMISSION_SUBMISSION_SUBMITTED,

        /**
         * Submission was cancelled by either the Authorized Submitter or Preparer
         */
        SUBMISSION_SUBMISSION_CANCELLED

    }

    /**
     * Well-known parameter names suitable for use as keys.  Parameter names may be used to map to values that
     * parametrize notification metadata or a notification body.
     */
    enum Param {

        /**
         * Placeholder for the addressee of the notification
         */
        TO("to"),

        /**
         * Placeholder for notification carbon copies
         */
        CC("cc"),

        /**
         * Placeholder for notification blind carbon copies
         */
        BCC("bcc"),

        /**
         * Placeholder for the sender of the notification
         */
        FROM("from"),

        /**
         * Placeholder for the subject of the notification
         */
        SUBJECT("subject"),

        /**
         * Placeholder for a data structure carrying metadata about the submission that relates to this notification
         */
        RESOURCE_METADATA("resource_metadata"),

        /**
         * Placeholder for a data structure carrying metadata about the event prompting the notification
         */
        EVENT_METADATA("event_metadata"),

        /**
         * Placeholder for a data structure carrying links and their descriptions that may be included in the
         * notification
         */
        LINKS("link_metadata");

        /**
         * String representation of the parameter name, suitable for use as a key in a key-value pair.
         */
        private String paramName;

        private Param(String paramName) {
            this.paramName = paramName;
        }

        /**
         * String representation of the parameter name, suitable for use as a key in a key-value pair.
         *
         * @return the string representation of the parameter name
         */
        public String paramName() {
            return this.paramName;
        }

    }

    Collection<String> getRecipients();

    Collection<String> getCc();

    Collection<String> getBcc();

    Type getType();

    Map<Param, String> getParameters();

    URI getEventUri();

    URI getResourceUri();

    String getSender();

}
