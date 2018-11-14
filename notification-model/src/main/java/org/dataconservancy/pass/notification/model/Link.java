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
import java.util.Objects;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class Link {

    private String rel;

    private URI href;

    public Link() {

    }

    public Link(URI href, String rel) {
        Objects.requireNonNull(href, "Link must not be null.");
        Objects.requireNonNull(rel, "Rel must not be null.");

        this.rel = rel;
        this.href = href;
    }

    public String getRel() {
        return rel;
    }

    public void setRel(String rel) {
        Objects.requireNonNull(rel, "Rel must not be null.");
        this.rel = rel;
    }

    public URI getHref() {
        return href;
    }

    public void setHref(URI href) {
        Objects.requireNonNull(rel, "Href must not be null.");
        this.href = href;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Link link1 = (Link) o;
        return Objects.equals(rel, link1.rel) &&
                Objects.equals(href, link1.href);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rel, href);
    }

    @Override
    public String toString() {
        return "Link{" +
                "rel='" + rel + '\'' +
                ", href='" + href + '\'' +
                '}';
    }
    
    
    public interface Rels {
        
        /** 
         * Link to view a submission.
         * <p>
         * This is used to link to a Submission in the UI, without an 
         * expectation that any action needs to be taken.
         * </p>
         */
        public static final String SUBMISSION_VIEW = "submission-view";
        
        /** 
         * Link to Review a submission.
         * <p>
         * This is used to link to a Submission in the UI, with
         * expectation that some sort of action needs to be taken.
         * </p>
         */
        public static final String SUBMISSION_REVIEW = "submission-review";
        
        /**
         * Link which invites a new user to review a submission.
         */
        public static final String SUBMISSION_REVIEW_INVITE = "submission-review-invite";
        
    }

}
