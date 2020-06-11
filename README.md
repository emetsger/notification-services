# Notification Services

Notification Services (NS) reacts to `SubmissionEvent` messages emitted by the Fedora repository by composing and dispatching notifications in the form of emails to the participants related to the event.

# Runtime Configuration

The runtime configuration for Notification Services (NS) is referenced by the environment variable `PASS_NOTIFICATION_CONFIGURATION` or a system property named `pass.notification.configuration`.  The value of this environment variable must be a [Spring Resource URI](https://docs.spring.io/spring/docs/5.1.2.RELEASE/spring-framework-reference/core.html#resources), beginning with `classpath:/`, `file:/`, or `http://`.

A sample configuration file is listed below. 

## Mode

Notification Services (NS) has three runtime modes:
- `DISABLED`: No notifications will be composed or emitted.  All JMS messages received by NS will be immediately acknowledged and subsequently discarded.
- `DEMO`: Allows a whitelist, global carbon copy recipient list, and notification templates to be configured distinct from the `PRODUCTION` mode.  Otherwise exactly the same as `PRODUCTION`.
- `PRODUCTION`: Allows a whitelist, global carbon copy recipient list, and notification templates to be configured distinct from the `DEMO` mode.  Otherwise exactly the same as `DEMO`.

Configuration elements for both `PRODUCTION` and `DEMO` modes may reside in the same configuration file.  There is no need to have separate configuration files for a "demo" and "production" instance of NS.

The environment variable `PASS_NOTIFICATION_MODE` (or its system property equivalent `pass.notification.mode`) is used to set the runtime mode.

## SMTP Server

Notification Services (NS) emits notifications in the form of email.  Therefore an SMTP relay must be configured for notification delivery.
- `PASS_NOTIFICATION_SMTP_HOST` (`pass.notification.smtp.host`): the hostname or IP address of an SMTP mail relay
- `PASS_NOTIFICATION_SMTP_PORT` (`pass.notification.smtp.port`): the TCP port for SMTP mail relay or submission
- `PASS_NOTIFICATION_SMTP_USER` (`pass.notification.smtp.user`): optional username for SMTP auth
- `PASS_NOTIFICATION_SMTP_PASS` (`pass.notification.smtp.pass`): optional password for SMTP auth
- `PASS_NOTIFICATION_SMTP_TRANSPORT` (`pass.notification.smtp.transport`): valid options are: `SMTP`, `SMTPS`, `SMTP_TLS`

## Notification Recipients

The recipient(s) of a notification (e.g. email) is a function of a `{Submission, SubmissionEvent}` tuple.  After the recipient list has been determined, it can be manipulated as discussed below.

### Whitelist

Each configuration mode (discussed above) may have an associated whitelist.  If the whitelist is empty, _all_ recipients for a given notification will receive an email.  If the whitelist is _not empty_, the recipients for a given notification will be filtered, and _only_ whitelisted recipients will receive the notification.  Having a whitelist for the `DEMO` mode is useful to prevent accidental spamming of end users with test notifications.

Production should use an empty whitelist (i.e. all potential notification recipients are whitelisted).

### Global Carbon Copy Support

Each configuration mode (discussed above) may specify one or more "global carbon copy" addresses.  These addresses will receive a copy of each email sent by Notification Services (NS).  Global carbon copy addresses are implicitly whitelisted; they do not need to be explicitly configured in a whitelist.

Blind carbon copy is also supported.

Here is an example recipient configuration that specifies a global carbon copy and a global blind carbon copy:

    "recipient-config": [
        {
          "mode": "DEMO",
          "fromAddress": "pass-noreply@jhu.edu",
          "global_cc": [
            "pass-support@jhu.edu"
          ],
          "global_bcc": [
            "pass-ops@jhu.edu"
          ]          
        }
    ]
    
Multiple email addresses may be specified.

### Example

For example, let's say that NS is preparing to send a notification to `user@example.org`.

If the runtime mode of NS is `DEMO`, and:
- the `DEMO` mode has no (or an empty) whitelist, then `user@example.org` and the global carbon copy address (for the `DEMO` mode) receives the notification.
- the `DEMO` mode has a whitelist that does _not_ contain `user@example.org`, then only the global carbon copy address receives the notification
- the `DEMO` mode has a whitelist that _does contain_ `user@example.org`, then `user@example.org` and the global carbon copy address receives the notification
- the `DEMO` mode has a whitelist that does _not_ contain `user@example.org` and there is no global carbon copy address (for the `DEMO` mode), then no notification will be dispatched

If the runtime mode of NS is `PRODUCTION`, and:
- the `PRODUCTION` mode has no (or an empty) whitelist, then `user@example.org` and the global carbon copy address (for the `PRODUCTION` mode) receives the notification.
- the `PRODUCTION` mode has a whitelist that does _not_ contain `user@example.org`, then only the global carbon copy address receives the notification
- the `PRODUCTION` mode has a whitelist that _does contain_ `user@example.org`, then `user@example.org` and the global carbon copy address receives the notification
- the `PRODUCTION` mode has a whitelist that does _not_ contain `user@example.org` and there is no global carbon copy address (for the `PRODUCTION` mode), then no notification will be dispatched

## Templates

Templates allow the content of notification emails to be customized.  There are three templates for every type of notification:
- Subject template: used as the content for the subject line of the email notification
- Body template: used as the content for the body of the email
- Footer template: used as the content for the footer of the email

This allows each part of the email to be templatized independently, or to share a template across notification types (e.g. all notifications could use the same footer template).

Templates can either be specified as in-line configuration values, or they can be specified as Spring Resource URIs referencing the location of the template content.  The latter provides the most flexibility, allowing the templates to be managed independent of Notification Services (NS) configuration.

Templates are parameterized by the NS model.  This allows for simple variable substitution when rendering the content of an email notification.  The template language supported by NS is [Mustache](https://mustache.github.io/), specifically the [Handlebars](https://github.com/jknack/handlebars.java) Java implementation.  For details on Mustache and Handlebars, check out the [Handlebars blog](http://jknack.github.io/handlebars.java/) and the [Mustache(5) man page](http://mustache.github.io/mustache.5.html).  Sample templates are available in the `templates/` folder of the [`notification-services`](https://github.com/OA-PASS/pass-docker) container in [pass-docker](https://github.com/OA-PASS/pass-docker) or in the `HandlebarsParameterizerTest` class.

The model provided for template parameterization will depend on the version of NS used, because NS composes and parameterizes the model at compile time.  Initially NS provides the following model to the Handlebars templating engine:
- `to`: a string containing the email address of the recipient of the notification
- `cc`: a string containing comma delimited email addresses of any carbon copy recipients
- `from`: a string containing the email address of the sender of the notification
- `resource_metadata`: a JSON object containing metadata about the `Submission`:
    - `title`: the title of the `Submission`
    - `journal-title`: the name of the journal that the author accepted manuscript is being published to
    - `volume`: the volume of the journal that the author accepted manuscript is being published to
    - `issue`: the issue of the journal that the author accepted manuscript is being published to
    - `abstract`: the abstract of the `Submission`
    - `doi`: the DOI assigned by the publisher to the author accepted manuscript
    - `publisher`: the name of the publisher
    - `authors`: a JSON array of author objects
- `event_metadata`: a JSON object containing metadata about the `SubmissionEvent`:
    - `id`: the identifier of the event, a URI to the `SubmissionEvent` resource
    - `comment`: the comment provided by the preparer or authorized submitter associated with the `SubmissionEvent`
    - `performedDate`: the DateTime the action precipitating the event was performed
    - `performedBy`: the URI of the `User` resource responsible for precipitating the event
    - `performerRole`: the role the `performedBy` user held at the time the event was precipitated
- `link_metadata`: a JSON array of link objects associated with the `SubmissionEvent`
    - each link object has an `href` attribute containing the URL, and a `rel` attribute describing its relationship to the `SubmissionEvent`
    - supported `rel` values are:
        - `submission-view`: a link to view the `Submission` resource in the Ember User Interface
        - `submission-review`: a link to review and approve a `Submission` in the Ember User Interface
        - `submission-review-invite`: a link which invites the recipient of the notification to the Ember User Interface, and subsequently presents the review and approve workflow in the Ember User Interface

## Environment Variables

Supported environment variables (system property analogs) and default values are:

- `SPRING_ACTIVEMQ_BROKER_URL` (`spring.activemq.broker-url`): `${activemq.broker.uri:tcp://${jms.host:localhost}:${jms.port:61616}}`
- `SPRING_JMS_LISTENER_CONCURRENCY` (`spring.jms.listener.concurrency`): `4`
- `SPRING_JMS_LISTENER_AUTO_STARTUP` (`spring.jms.listener.auto-startup`): `true`
- `PASS_NOTIFICATION_QUEUE_EVENT_NAME` (`pass.notification.queue.event.name`): `event`
- `PASS_FEDORA_USER` (`pass.fedora.user`): `fedoraAdmin`
- `PASS_FEDORA_PASSWORD` (`pass.fedora.password`): `moo`
- `PASS_FEDORA_BASEURL` (`pass.fedora.baseurl`): `http://${fcrepo.host:localhost}:${fcrepo.port:8080}/fcrepo/rest/`
- `PASS_ELASTICSEARCH_URL` (`pass.elasticsearch.url`): `http://${es.host:localhost}:${es.port:9200}/pass`
- `PASS_ELASTICSEARCH_LIMIT` (`pass.elasticsearch.limit`): `100`
- `PASS_NOTIFICATION_MODE` (`pass.notification.mode`): `DEMO`
- `PASS_NOTIFICATION_SMTP_HOST` (`pass.notification.smtp.host`): `${pass.notification.smtp.host:localhost}`
- `PASS_NOTIFICATION_SMTP_PORT` (`pass.notification.smtp.port`): `${pass.notification.smtp.port:587}`
- `PASS_NOTIFICATION_SMTP_USER` (`pass.notification.smtp.user`):
- `PASS_NOTIFICATION_SMTP_PASS` (`pass.notification.smtp.pass`):
- `PASS_NOTIFICATION_SMTP_TRANSPORT` (`pass.notification.smtp.transport`): `${pass.notification.smtp.transport:SMTP}`  
- `PASS_NOTIFICATION_MAILER_DEBUG` (`pass.notification.mailer.debug`): `false`
- `PASS_NOTIFICATION_CONFIGURATION` (`pass.notification.configuration`): `classpath:/notification.json`
- `PASS_NOTIFICATION_HTTP_AGENT` (`pass.notification.http.agent`): `pass-notification/x.y.z`

## Example Configuration

An example configuration file is provided below:

```json
{
  "mode": "${pass.notification.mode}",
  "recipient-config": [
    {
      "mode": "DEMO",
      "fromAddress": "demo-pass@mail.local.domain",
      "global_cc": [
        "demo@mail.local.domain"
      ],
      "whitelist": [
        "mailto:emetsger@mail.local.domain"
      ]
    }
  ],
  "templates": [
    {
      "notification": "SUBMISSION_APPROVAL_INVITE",
      "templates": {
        "SUBJECT": "Approval Invite Subject",
        "BODY": "Approval Invite Body",
        "FOOTER": "classpath:/templates/footer.hbr"
      }
    },
    {
      "notification": "SUBMISSION_APPROVAL_REQUESTED",
      "templates": {
        "SUBJECT": "Approval Requested Subject",
        "BODY": "Approval Requested Body",
        "FOOTER": "classpath:/templates/footer.hbr"
      }
    },
    {
      "notification": "SUBMISSION_CHANGES_REQUESTED",
      "templates": {
        "SUBJECT": "Changes Requested Subject",
        "BODY": "Changes Requested Body",
        "FOOTER": "classpath:/templates/footer.hbr"
      }
    },
    {
      "notification": "SUBMISSION_SUBMISSION_SUBMITTED",
      "templates": {
        "SUBJECT": "Submission Submitted Subject",
        "BODY": "Submission Submitted Body",
        "FOOTER": "classpath:/templates/footer.hbr"
      }
    },
    {
      "notification": "SUBMISSION_SUBMISSION_CANCELLED",
      "templates": {
        "SUBJECT": "Submission Cancelled Subject",
        "BODY": "Submission Cancelled Body",
        "FOOTER": "classpath:/templates/footer.hbr"
      }
    }
  ],
  "smtp": {
    "host": "${pass.notification.smtp.host}",
    "port": "${pass.notification.smtp.port}",
    "smtpUser": "${pass.notification.smtp.user}",
    "smtpPassword": "${pass.notification.smtp.pass}",
    "smtpTransport": "SMTP_TLS"
  },
  "user-token-generator": {
    "key": "BETKPFHWGGDIEWIIYKYQ33LUS4"
  },
  "link-validators": [
    {
      "rels" : [
        "submission-view",
        "submission-review",
        "submission-review-invite"
      ],
      "requiredBaseURI" : "http://example.org",
      "throwExceptionWhenInvalid": true
    }, 
    {
      "rels": ["*"],
      "requiredBaseURI" : "http",
      "throwExceptionWhenInvalid": false
    }
  ]
}
```

# Developers

Design document is [here](https://docs.google.com/document/d/1k4dWIe-2pOb-E8qf-C0BE7tGDBsEZxGlHAfZ_KaDIGY/edit?usp=sharing).

The major components of Notification Services (NS) are the model, business logic used to compose a `Notification`, and the dispatch of `Notification`s. 

Currently each `SubmissionEvent` received by NS results in the creation of a single `Notification`, which results in the dispatch of a single email.  Multiple recipients (e.g. using CC or BCC email headers) can be specified on the email if needed.

## Model

The notification model is below.  While email is the natural form of dispatching notifications, the model tries to remain independent of an underlying transport or dispatch mechanism.

![Notification Services Model](src/main/resources/ns-model.png)

Highlights of this model are:
- the `parameters` map: this is the model that is injected into the templating engine
- the `resouceUri` references the PASS resource this notification pertains to
- the `eventUri` references the PASS resource that this notification is responding to
- the `type` is the type of the `Notification`.  In this case, there is a 1:1 correspondence between the `SubmissionEvent` type and the `Notification` type.

### Notification Types

|Notification Type               |Description
|--------------------------------|---------------------
|SUBMISSION_APPROVAL_REQUESTED   |Preparer has requested approval of a Submission by an Authorized Submitter
|SUBMISSION_APPROVAL_INVITE      |Preparer has requested approval of a Submission by an Authorized Submitter who does not have a User in PASS.
|SUBMISSION_CHANGES_REQUESTED    |Authorized Submitter has requested changes to the submission by the Preparer.
|SUBMISSION_SUBMISSION_SUCCESS   |Submission was successfully submitted by the Authorized Submitter
|SUBMISSION_SUBMISSION_CANCELLED |Submission was cancelled by either the Authorized Submitter or Preparer

### Submission State

The state of the `Submission` vis-a-vis the `SubmissionEvent` (adapted from the [design document](https://docs.google.com/document/d/1k4dWIe-2pOb-E8qf-C0BE7tGDBsEZxGlHAfZ_KaDIGY/edit?usp=sharing)).

![Submission State Model](src/main/resources/submission-state.png)

|What happened to the Submission |SubmissionEvent Type                             |Notification Recipient List
|--------------------------------|-------------------------------------------------|----------------------------
|AS Cancelled                    |CANCELLED                                        |Preparer
|AS Submitted                    |SUBMITTED                                        |Preparer
|AS Request Changes              |CHANGES_REQUESTED                                |Preparer
|Preparer Cancelled              |CANCELLED                                        |AS
|Preparer Request Approval       |APPROVAL_REQUESTED, APPROVAL_REQUESTED_NEWUSER   |AS
 
* AS = Authorized Submitter
* Preparer = Proxy who has prepared the submitter on behalf of the AS

### Parameters

The `parameters` map carries simple strings or serialized JSON structures.
- `TO`, `CC`, `BCC`, `FROM`, and `SUBJECT` are all simple strings
- `RESOURCE_METADATA`, `EVENT_METADATA`, and `LINKS` all contain serialized JSON structures
- Handlebars, the Mustache-based template engine, can navigate the JSON structures to pull out the desired information for email templates.

## Notification Service

The `NotificationService` is the primary interface that abstracts the business logic associated with composing a `Notification` and handing it off for dispatch.  If future requirements dictate multiple `Notification`s were to arise from a single `SubmissionEvent`, `DefaultNotificationService` would be the starting point for implementing the fan out.

The `Composer` class does the heavy lifting within the `DefaultNotificationService`.  It is responsible for composing a `Notification` from the `Submission` and `SubmissionEvent`, including:
- determining the type of `Notification`
- creating and populating the data structures used in the `parameters` map
- determining the recipients of the `Notification`, and from whom the `Notification` should come from

After a `Notification` has been created and populated, it is sent to the `DispatchApi`, which returns a unique identifier for each `Notification` it dispatches.

## Dispatch

The Dispatch API accepts a `Notification` and returns a unique identifier for each `Notification` it dispatches.  The unique identifier is determined by the underlying notification implementation.  For example, `EmailDispatchImpl` returns the SMTP `Message-ID`.  The identifier can be used to associate a `Notification` with the underlying notification transport.

The Dispatch portion of Notification Services is not concerned with populating the `Notification`; it expects that business logic to have been performed earlier in the call stack.

Dispatch _does_ have to adapt a `Notification` to the underlying transport used - in this case, email.  This means resolving URIs to recipient email addresses, and invoking the templating engine for composing email subject, body, and footer.

### Email Implementation

The only `DispatchService` implementation is the `EmailDispatchImpl`, which is composed of three main classes:
- `Parameterizer`: responsible for resolving template content and invoking the templating engine, producing the content for the email subject, body, and footer
- `EmailComposer`: responsible for adapting the `Notification` to an email (including resolving and setting the from, to, and cc addresses), provided the parameterized templates
- `Mailer`: responsible for actually sending the email to recipients

### Templates

Templates are used to customize the subject, body, and footer of email messages that result from a notification.  Each notification type has a corresponding template, and the templates and their content are configured in the `notification.js` configuration file.  A sample portion of the configuration is below:

```json
"templates": [
    {
      "notification": "SUBMISSION_APPROVAL_INVITE",
      "templates": {
        "SUBJECT": "Approval Invite Subject",
        "BODY": "Approval Invite Body",
        "FOOTER": "Approval Invite Footer"
      }
    },
    {
      "notification": "SUBMISSION_APPROVAL_REQUESTED",
      "templates": {
        "SUBJECT": "Approval Requested Subject",
        "BODY": "Approval Requested Body",
        "FOOTER": "Approval Requested Footer"
      }
    },
    {
      "notification": "SUBMISSION_CHANGES_REQUESTED",
      "templates": {
        "SUBJECT": "Changes Requested Subject",
        "BODY": "Changes Requested Body",
        "FOOTER": "Changes Requested Footer"
      }
    },
    {
      "notification": "SUBMISSION_SUBMISSION_SUBMITTED",
      "templates": {
        "SUBJECT": "Submission Submitted Subject",
        "BODY": "Submission Submitted Body",
        "FOOTER": "Submission Submitted Footer"
      }
    },
    {
      "notification": "SUBMISSION_SUBMISSION_CANCELLED",
      "templates": {
        "SUBJECT": "Submission Cancelled Subject",
        "BODY": "Submission Cancelled Body",
        "FOOTER": "Submission Cancelled Footer"
      }
    }
  ]
```

You can see that there is an object identifying each notification type, and for each notification type a `SUBJECT`, `BODY`, and `FOOTER` template may be defined.

The value associated with `SUBJECT`, `BODY`, and `FOOTER` may be in-line content as seen in the example, or it can be a reference to a Spring Resource URI, e.g.:
```json
    {
      "notification": "SUBMISSION_APPROVAL_INVITE",
      "templates": {
        "SUBJECT": "classpath:/templates/submission-approval-subject.txt",
        "BODY": "classpath:/templates/submission-approval-body.txt",
        "FOOTER": "classpath:/templates/submission-approval-footer.txt"
      }
    }
```
Using Spring Resource URIs to refer to the template location is a more flexible and maintainable way of managing notification templates, because it allows the templates to be updated in place without having to edit the primary configuration file (`notification.js`) any time a template needs updating.  Using Spring Resource URIs also allows template content to be shared across notification types.  For example, each notification type could use the same `FOOTER` content.  The `CompositeResolver` is responsible for determining whether or not the value represents inline content, or if it represents a Spring Resource URI to be resolved.

Notification Services supports Mustache templates, specifically implemented using Handlebars.  Each template is injected with the `parameters` map from the `Notification`.  See above for the documented fields of the `parameters` map.  It is beyond the scope of this README to provide guidance on using Mustache or Handlebars, but there are some examples in `pass-docker`, and in the `HandlebarsParameterizerTest`.  Both inline template content and referenced template content (i.e. Spring Resource URIs) can be Mustache templates.

### Composition

The `EmailComposer` is responsible for adapting the `Notification` to an email.  This includes: 
* Resolving `Notification` recipient URIs to email addresses
    * In the case of `mailto` URIs, the scheme specific part is used as the recipient
    * In the case of `http` or `https` URIs, they are assumed to reference `User` resources in the Fedora repository.  The
      URIs are de-referenced and the `User.email` is used
* Applying the email recipient whitelist
* Creating the email itself, including the email subject and message body, and encoding.
    * the subject and message body are provided to the `EmailComposer` from the templating engine.

After the `EmailComposer` has created an email, it is returned to the `EmailDispatchImpl` for dispatch via SMTP.
