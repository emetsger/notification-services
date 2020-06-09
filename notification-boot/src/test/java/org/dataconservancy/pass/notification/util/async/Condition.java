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
package org.dataconservancy.pass.notification.util.async;

import org.dataconservancy.pass.notification.util.mail.SimpleImapClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Message;
import javax.mail.search.SearchTerm;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class Condition<T> {
    private static Logger LOG = LoggerFactory.getLogger(Condition.class);

    /**
     * The executor service used to execute the {@link #condition}
     */
    private ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Encapsulates the check to be performed until {@link #result} is {@code true} or the timeout has expired.
     */
    private Callable<T> condition;

    /**
     * A short, human-readable name describing this {@code Condition}
     */
    private String name;

    /**
     * Governs how long this condition will be re-tried once {@link #submit() submitted}.  The condition will fail if
     * the {@link #result} is {@code null} and this {@link #timeoutThresholdMs threshold} has been exceeded.
     */
    private long timeoutThresholdMs = 60000L;

    /**
     * Governs how often the condition is retried.  For a linear retries, set this to {@code 1}.  For a geometric/
     * exponential backoff, set to some number greater than {@code 1}.
     */
    private float backoffFactor = 1.5F;

    /**
     * Initial retry attempt will wait this long.  The wait between successive retry attempts will be multiplied by the
     * {@link #backoffFactor}.
     */
    private long initialBackoffMs = 1000;

    /**
     * The task that represents a condition that should "succeed".  Success is defined by the {@code Function} passed to
     * {@link #verify(Function)} or {@link #awaitAndVerify(Function)}.  By default success is defined as a {@link
     * #conditionFuture} that returns not null.
     */
    private Future<T> conditionFuture;

    /**
     * The value of the Future (i.e. the result of {@link Future#get()}
     */
    private T futureResult;

    /**
     * The result of executing the verification function on the return of the {@link #conditionFuture}
     */
    private boolean result = false;

    /**
     * Records whether or not this condition has been submitted to the {@link #executorService}
     */
    private boolean submitted = false;

    /**
     * Verification function
     */
    private Function<T, Boolean> verificationFunc;

    /**
     * Create a new Condition.  By default, the condition is deemed successful when it returns a non-null object.
     *
     * @param condition the condition to be checked
     * @param name      a short, human-readable description of this Condition
     */
    public Condition(Callable<T> condition, String name) {
        this(condition, Objects::nonNull, name);
    }

    /**
     * Create a new Condition.  The condition is deemed successful when the {@code verificationFunc} returns {@code
     * true}.
     *
     * @param condition        the condition to be checked
     * @param verificationFunc the verification function used to verify the result of the condition
     * @param name             a short, human-readable description of this Condition
     */
    public Condition(Callable<T> condition, Function<T, Boolean> verificationFunc, String name) {
        this.condition = condition;
        this.verificationFunc = verificationFunc;
        this.name = name;
    }

    /**
     * Creates a Condition that is satisfied by the presence of a Message for a given SMTP message id.
     *
     * Useful for asynchronously waiting for a message to show up in an inbox from the Dispatch API.
     *
     * @param messageId the SMTP message id
     * @param imapClient a fully configured IMAP client
     * @return the Condition.
     */
    public static Condition<Message> newGetMessageCondition(String messageId, SimpleImapClient imapClient) {
        Condition<Message> c = new Condition<>(getMessage(messageId, imapClient), Objects::nonNull, "Get message");
        c.setTimeoutThresholdMs(10000);
        return c;
    }

    /**
     * Creates a Condition that is satisfied as soon as one Message matches the supplied search term.
     *
     * Useful for asynchronously waiting for a message to show up in an inbox for arbitrary search terms.  For example,
     * if you don't have the SMTP message ID you can search for messages that contain the PASS Submission ID in the
     * SMTP headers.
     *
     * @param term the IMAP search term
     * @param imapClient a fully configured IMAP client
     * @return the Condition.
     */
    public static Condition<Collection<Message>> newSearchMessageCondition(SearchTerm term, SimpleImapClient imapClient) {
        Condition<Collection<Message>> c = new Condition<>(searchMessage(term, imapClient), m -> m.size() > 0, "Search messages");
        c.setTimeoutThresholdMs(60000);
        return c;
    }

    /**
     * Returns a Callable that will invoke {@link SimpleImapClient#getMessage(String)} with the supplied message id.
     *
     * @param messageId the SMTP message ID
     * @param imapClient a fully configured IMAP client
     * @return the Callable
     */
    public static Callable<Message> getMessage(String messageId, SimpleImapClient imapClient) {
        return () -> imapClient.getMessage(messageId);
    }

    /**
     * Returns a Callable that will invoke {@link SimpleImapClient#search(SearchTerm)} with the supplied term.
     *
     * @param term the IMAP search term
     * @param imapClient a fully configured IMAP client
     * @return the Callable
     */
    public static Callable<Collection<Message>> searchMessage(SearchTerm term, SimpleImapClient imapClient) {
        return () -> imapClient.search(term);
    }

    /**
     * Verify that the results of {@code await(...)} returned a non-null value.
     *
     * @return checks that the condition returns a non-null value
     */
    public boolean verify() {
        return verify(verificationFunc);
    }

    /**
     * Verify the results of a call to {@code await(...)} using the supplied function.
     *
     * @param verification verifies the result of the condition
     * @return the result of the verification function
     */
    public boolean verify(Function<T, Boolean> verification) {
        return awaitAndVerify(verification);
    }

    /**
     * Execute and re-try this condition until the timeout expires or the result is non-null.
     */
    public void await() {
        awaitAndVerify(verificationFunc);
    }

    /**
     * Execute and re-try this condition until the timeout expires, or the result is verified.
     *
     * @param verification the verification function applied to the result of the condition
     * @return the result of the verification function
     */
    public boolean awaitAndVerify(Function<T, Boolean> verification) {
        return awaitAndVerify(timeoutThresholdMs, verification);
    }

    /**
     * Execute and re-try this condition until the timeout expires, or the result is verified.
     *
     * @param timeoutMs    the timeout threshold, in milliseconds
     * @param verification the verification function applied to the result of the condition
     * @return the result of the verification function
     */
    public boolean awaitAndVerify(long timeoutMs, Function<T, Boolean> verification) {
        long start = System.currentTimeMillis();
        Boolean result = Boolean.FALSE;
        long backoffMs = initialBackoffMs;
        Exception failureException = null;

        if (!this.submitted) {
            submit();
        }

        do {
            try {
                LOG.debug("Checking condition {}", name);
                futureResult = this.conditionFuture.get(timeoutMs, MILLISECONDS);
                result = verification.apply(futureResult);
                if (result == null || !result) {
                    LOG.debug("Condition {} failed, sleeping for {} ms before re-trying.", name, backoffMs);
                    Thread.sleep(backoffMs);
                    backoffMs = Math.round(backoffMs * backoffFactor);
                    submitInternal();
                }
            } catch (InterruptedException ie) {
                LOG.debug("Condition {} was interrupted after {} ms; aborting.", name, System.currentTimeMillis() - start);
                result = false;
                failureException = ie;
                break;
            } catch (Exception e) {
                LOG.debug("Condition {} threw exception; will re-try in {} ms: {}", name, backoffMs, e.getMessage());
                failureException = e;
                try {
                    Thread.sleep(backoffMs);
                    // must re-submit after catching an exception, because Future.get will perpetually return the exception unless the Future is re-executed
                    submitInternal();
                } catch (InterruptedException ie) {
                    LOG.debug("Condition {} was interrupted after {} ms; aborting.", name, System.currentTimeMillis() - start);
                    result = false;
                    failureException = ie;
                    break;
                }
                backoffMs = Math.round(backoffMs * backoffFactor);
            }
        } while ((System.currentTimeMillis() - start < timeoutMs) && (result == null || !result));

        if (result == null || !result) {
            LOG.warn("Condition {} failed, elapsed time {} ms", name, System.currentTimeMillis() - start);
            if (failureException != null) {
                LOG.warn("Condition {} failed with exception: {}", name, failureException.getMessage(), failureException);
            }
            this.result = false;
        } else {
            LOG.debug("Condition {} satisfied, elapsed time {} ms", name, System.currentTimeMillis() - start);
            this.result = true;
        }

        return this.result;
    }

    public void submit() {
        // allow the public to only call submit() once.
        if (!this.submitted) {
            this.conditionFuture = executorService.submit(condition);
            this.submitted = true;
        }
    }

    private void submitInternal() {
        this.submitted = true;
        this.conditionFuture = executorService.submit(condition);
    }


    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTimeoutThresholdMs() {
        return timeoutThresholdMs;
    }

    public void setTimeoutThresholdMs(long timeoutThresholdMs) {
        this.timeoutThresholdMs = timeoutThresholdMs;
    }

    public float getBackoffFactor() {
        return backoffFactor;
    }

    public void setBackoffFactor(float backoffFactor) {
        this.backoffFactor = backoffFactor;
    }

    public long getInitialBackoffMs() {
        return initialBackoffMs;
    }

    public void setInitialBackoffMs(long initialBackoffMs) {
        this.initialBackoffMs = initialBackoffMs;
    }
}
