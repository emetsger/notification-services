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

import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.client.PassClientDefault;
import org.dataconservancy.pass.client.PassJsonAdapter;
import org.dataconservancy.pass.client.adapter.PassJsonAdapterBasic;
import org.dataconservancy.pass.model.Repository;
import org.dataconservancy.pass.model.Submission;
import org.dataconservancy.pass.model.SubmissionEvent;
import org.dataconservancy.pass.model.User;
import org.dataconservancy.pass.notification.impl.ComposerIT;
import org.dataconservancy.pass.notification.model.Link;
import org.dataconservancy.pass.notification.util.PathUtil;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;

import static java.nio.charset.Charset.forName;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.dataconservancy.pass.notification.model.Link.Rels.SUBMISSION_REVIEW_INVITE;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class NotificationSmokeIT {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationSmokeIT.class);

    private static final String SENDER = "staffWithGrants@jhu.edu";

    private static final String RECIPIENT = "staffWithNoGrants@jhu.edu";

    private static final String CC = "facultyWithGrants@jhu.edu";

    private PassClient passClient;

    private OkHttpClient httpClient;

    private String contextUri;

    @Before
    public void setUp() throws Exception {
        passClient = passClient();
        OkHttpClient.Builder builder = new OkHttpClient.Builder().authenticator((route, res) -> {
            if (res.request().header("Authorization") != null) {
                return null; // Give up, we've already failed to authenticate.
            }

            assertTrue(System.getProperties().containsKey("pass.fedora.user"));
            assertTrue(System.getProperties().containsKey("pass.fedora.password"));
            String credential = Credentials.basic(System.getProperty("pass.fedora.user", "fedoraAdmin"),
                    System.getProperty("pass.fedora.password", "moo"));
            return res.request().newBuilder()
                    .header("Authorization", credential)
                    .build();
        });

        httpClient = builder.build();

        contextUri = System.getProperty("pass.jsonld.context", "https://oa-pass.github.io/pass-data-model/src/main/resources/context-3.4.jsonld");
    }

    /**
     * A Submission resource that uses context version 3.4 should be able to be deserialized and read by the version of
     * the pass client used with notification services.  The Submission and its associated resources are populated using
     * OkHttpClient instead of the PassClient in order to insure resources are created with the specified context uri.
     *
     * The creation of the SubmissionEvent in Fedora should kick off the notification process.
     */
    @Test
    public void readContext34submissionResource() throws IOException, InterruptedException {
        String fedoraBaseUrl = System.getProperty("pass.fedora.baseurl", "http://localhost:8080/fcrepo/rest");

        // Repositories JSON
        String repositories = "" +
                "{\n" +
                "  \"@id\" : \"\",\n" +
                "  \"@type\" : \"Repository\",\n" +
                "  \"agreementText\" : \"NON-EXCLUSIVE LICENSE FOR USE OF MATERIALS This non-exclusive license defines the terms for the deposit of Materials in all formats into the digital repository of materials collected, preserved and made available through the Johns Hopkins Digital Repository, JScholarship. The Contributor hereby grants to Johns Hopkins a royalty free, non-exclusive worldwide license to use, re-use, display, distribute, transmit, publish, re-publish or copy the Materials, either digitally or in print, or in any other medium, now or hereafter known, for the purpose of including the Materials hereby licensed in the collection of materials in the Johns Hopkins Digital Repository for educational use worldwide. In some cases, access to content may be restricted according to provisions established in negotiation with the copyright holder. This license shall not authorize the commercial use of the Materials by Johns Hopkins or any other person or organization, but such Materials shall be restricted to non-profit educational use. Persons may apply for commercial use by contacting the copyright holder. Copyright and any other intellectual property right in or to the Materials shall not be transferred by this agreement and shall remain with the Contributor, or the Copyright holder if different from the Contributor. Other than this limited license, the Contributor or Copyright holder retains all rights, title, copyright and other interest in the images licensed. If the submission contains material for which the Contributor does not hold copyright, the Contributor represents that s/he has obtained the permission of the Copyright owner to grant Johns Hopkins the rights required by this license, and that such third-party owned material is clearly identified and acknowledged within the text or content of the submission. If the submission is based upon work that has been sponsored or supported by an agency or organization other than Johns Hopkins, the Contributor represents that s/he has fulfilled any right of review or other obligations required by such contract or agreement. Johns Hopkins will not make any alteration, other than as allowed by this license, to your submission. This agreement embodies the entire agreement of the parties. No modification of this agreement shall be of any effect unless it is made in writing and signed by all of the parties to the agreement.\",\n" +
                "  \"formSchema\" : \"{\\\"id\\\":\\\"JScholarship\\\",\\\"schema\\\":{\\\"title\\\":\\\"Johns Hopkins - JScholarship <br><p class='lead text-muted'>Deposit requirements for JH's institutional repository JScholarship.</p>\\\",\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"authors\\\":{\\\"title\\\":\\\"<div class='row'><div class='col-6'>Author(s) <small class='text-muted'>(required)</small></div><div class='col-6 p-0'></div></div>\\\",\\\"type\\\":\\\"array\\\",\\\"uniqueItems\\\":true,\\\"items\\\":{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"author\\\":{\\\"type\\\":\\\"string\\\",\\\"fieldClass\\\":\\\"body-text col-6 pull-left pl-0\\\"}}}}}},\\\"options\\\":{\\\"fields\\\":{\\\"authors\\\":{\\\"hidden\\\":false}}}}\",\n" +
                "  \"integrationType\" : \"full\",\n" +
                "  \"name\" : \"JScholarship\",\n" +
                "  \"repositoryKey\" : \"jscholarship\",\n" +
                "  \"schemas\" : [ \"https://oa-pass.github.io/metadata-schemas/jhu/common.json\", \"https://oa-pass.github.io/metadata-schemas/jhu/jscholarship.json\" ],\n" +
                "  \"url\" : \"https://jscholarship.library.jhu.edu/\",\n" +
                "  \"@context\" : \"" + contextUri + "\"\n" +
                "}";

        // Create the resource in Fedora
        String repositoriesUri = createResource(fedoraBaseUrl, "repositories", repositories);

        // Policy JSON
        String policy = "" +
                "{\n" +
                "  \"@id\" : \"\",\n" +
                "  \"@type\" : \"Policy\",\n" +
                "  \"description\" : \"The university expects that every scholarly article produced by full-time faculty members be accessible in an open access repository. This can be achieved through deposits into existing public access repositories (such as PubMed Central, arXiv, etc.) and/or into Johns Hopkins institutional repository, JScholarship.\",\n" +
                "  \"policyUrl\" : \"https://provost.jhu.edu/about/open-access/\",\n" +
                "  \"repositories\" : [ \"" + repositoriesUri + "\" ],\n" +
                "  \"title\" : \"Johns Hopkins University (JHU) Open Access Policy\",\n" +
                "  \"@context\" : \"" + contextUri + "\"\n" +
                "}";

        // Create the resource in Fedora
        String policyUri = createResource(fedoraBaseUrl, "policies", policy);

        // Preparers JSON
        String preparers = "" +
                "{\n" +
                "  \"@id\" : \"\",\n" +
                "  \"@type\" : \"User\",\n" +
                "  \"displayName\" : \"Robin Sinn\",\n" +
                "  \"email\" : \"rsinn@jhu.edu\",\n" +
                "  \"locatorIds\" : [ \"johnshopkins.edu:hopkinsid:EGPUI7\", \"johnshopkins.edu:jhed:rsinn1\", \"johnshopkins.edu:employeeid:00019042\" ],\n" +
                "  \"roles\" : [ \"submitter\" ],\n" +
                "  \"username\" : \"rsinn1@johnshopkins.edu\",\n" +
                "  \"@context\" : \"" + contextUri + "\"\n" +
                "}";

        // Create the resource in Fedora
        String preparersUri = createResource(fedoraBaseUrl, "users", preparers);

        // Journal JSON
        String journal = "" +
                "{\n" +
                "  \"@id\" : \"\",\n" +
                "  \"@type\" : \"Journal\",\n" +
                "  \"issns\" : [ \"Online:1538-3598\", \"Print:0098-7484\" ],\n" +
                "  \"journalName\" : \"JAMA\",\n" +
                "  \"nlmta\" : \"JAMA\",\n" +
                "  \"@context\" : \"" + contextUri + "\"\n" +
                "}";
        String journalUri = createResource(fedoraBaseUrl, "journals", journal);

        // Publication JSON
        String publication = "" +
                "{\n" +
                "  \"@id\" : \"\",\n" +
                "  \"@type\" : \"Publication\",\n" +
                "  \"doi\" : \"10.1001/jama.2020.10175\",\n" +
                "  \"journal\" : \"" + journalUri + "\",\n" +
                "  \"title\" : \"The Urgency and Challenge of Opening K-12 Schools in the Fall of 2020\",\n" +
                "  \"@context\" : \"" + contextUri + "\"\n" +
                "}";
        // Create the resource in Fedora
        String publicationUri = createResource(fedoraBaseUrl, "publications", publication);

        // Submitter JSON
        String submitter = "" +
                "{\n" +
                "  \"@id\" : \"\",\n" +
                "  \"@type\" : \"User\",\n" +
                "  \"displayName\" : \"Josh Sharfstein\",\n" +
                // Must use a whitelisted address for integration tests, otherwise random people may receive emails.
//                "  \"email\" : \"jsharfs1@johnshopkins.edu\",\n" +
                "  \"email\" : \"staffWithGrants@jhu.edu\",\n" +
                "  \"firstName\" : \"Josh\",\n" +
                "  \"lastName\" : \"Sharfstein\",\n" +
                "  \"locatorIds\" : [ \"johnshopkins.edu:jhed:jsharfs1\", \"johnshopkins.edu:employeeid:00071462\", \"johnshopkins.edu:hopkinsid:53KS2I\" ],\n" +
                "  \"roles\" : [ \"submitter\" ],\n" +
                "  \"@context\" : \"" + contextUri + "\"\n" +
                "}";

        // Create the resource in Fedora
        String submitterUri = createResource(fedoraBaseUrl, "users", submitter);

        // Submission JSON
        String submission = "" +
                "{\n" +
                "  \"@id\" : \"\",\n" +
                "  \"@type\" : \"Submission\",\n" +
                "  \"aggregatedDepositStatus\" : \"not-started\",\n" +
                "  \"effectivePolicies\" : [ \"" + policyUri + "\" ],\n" +
                "  \"metadata\" : \"{\\\"hints\\\":{\\\"collection-tags\\\":[\\\"covid\\\"]},\\\"publisher\\\":\\\"American Medical Association (AMA)\\\",\\\"title\\\":\\\"The Urgency and Challenge of Opening K-12 Schools in the Fall of 2020\\\",\\\"journal-title\\\":\\\"JAMA\\\",\\\"issns\\\":[{\\\"issn\\\":\\\"0098-7484\\\",\\\"pubType\\\":\\\"Print\\\"},{\\\"issn\\\":\\\"1538-3598\\\",\\\"pubType\\\":\\\"Online\\\"}],\\\"authors\\\":[{\\\"author\\\":\\\"Joshua M. Sharfstein\\\"},{\\\"author\\\":\\\"Christopher C. Morphew\\\"}],\\\"journal-NLMTA-ID\\\":\\\"JAMA\\\",\\\"publicationDate\\\":\\\"2020-6-1\\\",\\\"doi\\\":\\\"10.1001/jama.2020.10175\\\",\\\"$schema\\\":\\\"https://oa-pass.github.com/metadata-schemas/jhu/global.json\\\",\\\"agent_information\\\":{\\\"name\\\":\\\"Chrome\\\",\\\"version\\\":\\\"81\\\"}}\",\n" +
                "  \"preparers\" : [ \"" + preparersUri + "\" ],\n" +
                "  \"publication\" : \"" + publicationUri + "\",\n" +
                "  \"repositories\" : [ \"" + repositoriesUri + "\" ],\n" +
                "  \"source\" : \"pass\",\n" +
                "  \"submissionStatus\" : \"approval-requested\",\n" +
                "  \"submitted\" : false,\n" +
                "  \"submitter\" : \"" + submitterUri + "\",\n" +
                "  \"@context\" : \"" + contextUri + "\"\n" +
                "}";

        // Create the resource in Fedora
        String submissionUri = createResource(fedoraBaseUrl, "submissions", submission);

        // FIXME: the comment below is incorrect based on event type of APPROVAL_REQUESTED
        // When this event is processed, the authorized submitter will receive an email notification with a link that
        // will invite them to use PASS, and link the Submission to their newly created User (created when they login to PASS for the first time)
        SubmissionEvent event = new SubmissionEvent();
        event.setSubmission(URI.create(submissionUri));
        event.setPerformerRole(SubmissionEvent.PerformerRole.PREPARER);
        event.setPerformedBy(URI.create(preparersUri));
        event.setComment("How does this submission look?");
        event.setEventType(SubmissionEvent.EventType.APPROVAL_REQUESTED);
        event.setPerformedDate(DateTime.now());
        Link link = new Link(URI.create("https://pass.local/email/dispatch/myLink"), SUBMISSION_REVIEW_INVITE);
        event.setLink(link.getHref());

        event = passClient.createAndReadResource(event, SubmissionEvent.class);

        Thread.sleep(60000);
    }

    private String createResource(String fedoraBaseUrl, String container, String json) throws IOException {
        String postUrl;
        if (fedoraBaseUrl.endsWith("/")) {
            postUrl = fedoraBaseUrl + container;
        } else {
            postUrl = fedoraBaseUrl + "/" + container;
        }

        String resultUri;
        try (Response res = httpClient.newCall(new Request.Builder()
                .header("Content-Type", "application/ld+json")
                .post(RequestBody.create(MediaType.parse("application/ld+json"), json))
                .url(postUrl)
                .build()).execute()) {
            assertTrue(res.isSuccessful());
            resultUri = res.header("Location");
            assertNotNull(resultUri);
        }

        return resultUri;
    }

    @Test
    public void postNewEvent() throws IOException, InterruptedException {
        // This User prepares the submission on behalf of the Submission.submitter
        // Confusingly, this User has the ability to submit to PASS.  The authorization-related role of
        // User.Role.SUBMITTER should not be confused with the the logical role as a preparer of a submission.
        User preparer = new User();
        preparer.setEmail("emetsger@gmail.com");
        preparer.setDisplayName("Submission Preparer");
        preparer.setFirstName("Pre");
        preparer.setLastName("Parer");
        preparer.setRoles(Collections.singletonList(User.Role.SUBMITTER));

        preparer = passClient.createAndReadResource(preparer, User.class);

        // The Submission as prepared by the preparer.
        // The preparer did not find the authorized submitter in PASS, so they filled in the email address of the authorized submitter
        // Therefore, the Submission.submitter field will be null (because that *must* be a URI to a User resource, and the User does not exist)
        // The Submission.submitterEmail will be set to the email address of the authorized submitter
        Submission submission = new Submission();
        submission.setMetadata(resourceToString("/" + PathUtil.packageAsPath(ComposerIT.class) + "/submission-metadata.json", forName("UTF-8")));
        submission.setPreparers(Collections.singletonList(preparer.getId()));
        submission.setSource(Submission.Source.PASS);
        submission.setSubmitter(null);
        submission.setSubmitterEmail(URI.create("mailto:" + SENDER));

        submission = passClient.createAndReadResource(submission, Submission.class);

        // When this event is processed, the authorized submitter will recieve an email notification with a link that
        // will invite them to use PASS, and link the Submission to their newly created User (created when they login to PASS for the first time)
        SubmissionEvent event = new SubmissionEvent();
        event.setSubmission(submission.getId());
        event.setPerformerRole(SubmissionEvent.PerformerRole.PREPARER);
        event.setPerformedBy(preparer.getId());
        event.setComment("How does this submission look?");
        event.setEventType(SubmissionEvent.EventType.APPROVAL_REQUESTED_NEWUSER);
        event.setPerformedDate(DateTime.now());
        Link link = new Link(URI.create("https://pass.local/email/dispatch/myLink"), SUBMISSION_REVIEW_INVITE);
        event.setLink(link.getHref());

        event = passClient.createAndReadResource(event, SubmissionEvent.class);

        Thread.sleep(60000);

    }

    private PassClientDefault passClient() {
        assertTrue(System.getProperties().containsKey("pass.fedora.user"));
        assertTrue(System.getProperties().containsKey("pass.fedora.password"));
        assertTrue(System.getProperties().containsKey("pass.fedora.baseurl"));
        assertTrue(System.getProperties().containsKey("pass.elasticsearch.url"));
        assertTrue(System.getProperties().containsKey("pass.elasticsearch.limit"));
        assertTrue(System.getProperties().containsKey("http.agent"));

        return new PassClientDefault();
    }
}
