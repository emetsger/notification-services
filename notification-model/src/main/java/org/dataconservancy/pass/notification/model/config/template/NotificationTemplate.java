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
package org.dataconservancy.pass.notification.model.config.template;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dataconservancy.pass.notification.model.Notification;
import org.dataconservancy.pass.notification.model.config.NotificationConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Allows for the customization of notification subject, body, and footer for each notification type.  Each notification
 * type has exactly one {@code NotificationTemplate}, retrieved from the {@link NotificationConfig runtime
 * configuration} using {@link NotificationConfig#getTemplates()}.
 * <p>
 * Importantly, templates may be specified inline, or as references to a template body.  For example:
 * </p>
 * <pre>
 * NotificationConfig config = ...;
 * NotificationTemplate approvalRequestedTemplate = config.getTemplates()
 *                                       .stream()
 *                                       .filter(t -> t.getNotificationType() == SUBMISSION_APPROVAL_REQUESTED)
 *                                       .findAny()
 *                                       .orElseThrow(() -> new RuntimeException("Missing template for " + SUBMISSION_APPROVAL_REQUESTED));
 *
 * String subjectTemplate = approvalRequestedTemplate.getTemplates().get(SUBJECT);
 * // subjectTemplate can be an inline value:
 * //   "Approval Requested"
 * // or it can be a reference to a resource containing the subject value:
 * //   "classpath:/approval-requested-subject.txt"
 * </pre>
 * <p>
 * References must be specified as Spring Resource URIs.
 * </p>
 *
 * @see <a href="https://docs.spring.io/spring/docs/5.1.1.RELEASE/spring-framework-reference/core.html#resources">Spring Resources</a>
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class NotificationTemplate {

    /**
     * Names individual templates that are used to compose a single notification
     */
    public enum Name {

        /**
         * The template for composing the subject of a notification
         */
        SUBJECT("subject"),

        /**
         * The template for composing the body of a notification
         */
        BODY("body"),

        /**
         * The template for composing the footer of a notification
         */
        FOOTER("footer");

        private String templateName;

        private Name(String templateName) {
            this.templateName = templateName;
        }

        String templateName() {
            return templateName;
        }

    }

    /**
     * Inline bodies or a URI to the bodies
     */
    private Map<Name, String> templates = new HashMap<>(Name.values().length);

    /**
     * The type of notification this template is associated with
     */
    @JsonProperty("notification")
    private Notification.Type notificationType;

    /**
     * The templates for each named portion of a notification.  The values may be in-line, or reference a body using
     * Spring Resource URIs.
     *
     * @return the named templates
     */
    public Map<Name, String> getTemplates() {
        return templates;
    }

    public void setTemplates(Map<Name, String> templates) {
        this.templates = templates;
    }

    /**
     * The notification type this template will be used for.  Each notification type should have exactly one
     * {@code NotificationTemplate}.
     *
     * @return the applicable notification type
     */
    public Notification.Type getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(Notification.Type notificationType) {
        this.notificationType = notificationType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NotificationTemplate that = (NotificationTemplate) o;
        return Objects.equals(templates, that.templates) &&
                notificationType == that.notificationType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(templates, notificationType);
    }

    @Override
    public String toString() {
        return "NotificationTemplate{" +
                "bodies=" + templates +
                ", notificationType=" + notificationType +
                '}';
    }
}
