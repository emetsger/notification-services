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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Allows for the customization of notification subject, body, and footer.  The {@code TemplatePrototype} used is a
 * function of {@link Notification#getType() notification type}; each notification type has exactly one {@code
 * TemplatePrototype} used to compose notifications for that type.
 * <p>
 * {@code TemplatePrototype}
 * </p>
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class TemplatePrototype {

    public enum Name {

        SUBJECT("subject"),
        BODY("body"),
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
    private Map<Name, String> refs = new HashMap<>(Name.values().length);

    /**
     * The type of notification this template is associated with
     */
    @JsonProperty("notification")
    private Notification.Type notificationType;

    public Map<Name, String> getRefs() {
        return refs;
    }

    public void setRefs(Map<Name, String> refs) {
        this.refs = refs;
    }

    public void addBody(Name name, String body) {
        refs.put(name, body);
    }

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
        TemplatePrototype that = (TemplatePrototype) o;
        return Objects.equals(refs, that.refs) &&
                notificationType == that.notificationType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(refs, notificationType);
    }

    @Override
    public String toString() {
        return "TemplatePrototype{" +
                "bodies=" + refs +
                ", notificationType=" + notificationType +
                '}';
    }
}
