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

Who _receives_ a notification is a function of the type of `SubmissionEvent` handled by Notification Services (NS).  However, there are ways the recipient list can be manipulated, discussed below.

### Whitelist

Each configuration mode (discussed above) may have an associated whitelist.  If the whitelist is empty, _all_ recipients for a given notification will receive an email.  If the whitelist is _not empty_, the recipients for a given notification will be filtered, and _only_ whitelisted recipients will receive the notification.  Having a whitelist for the `DEMO` mode is useful to prevent accidental spamming of end users with test notifications.

Production should use an empty whitelist (i.e. all potential notification recipients are whitelisted).

### Global Carbon Copy

Each configuration mode (discussed above) may specify one or more "global carbon copy" addresses.  These addresses will receive a copy of each email sent by Notification Services (NS).  Global carbon copy addresses are implicitly whitelisted; they do not need to be explicitly configured in a whitelist.

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

## Model

### Notification type

### Parameters

## Templates

## Dispatch