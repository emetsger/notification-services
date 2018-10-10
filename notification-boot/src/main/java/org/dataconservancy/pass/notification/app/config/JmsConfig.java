/*
 *
 *  * Copyright 2018 Johns Hopkins University
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.dataconservancy.pass.notification.app.config;

import org.dataconservancy.pass.notification.impl.NotificationService;
import org.dataconservancy.pass.notification.model.config.Mode;
import org.dataconservancy.pass.notification.model.config.NotificationConfig;
import org.dataconservancy.pass.support.messaging.constants.Constants;
import org.dataconservancy.pass.support.messaging.json.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.JmsHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.util.ErrorHandler;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

/**
 * JMS configuration for Notification Services.  Primary entry point to the Notification Services stack when in
 * production.
 * <p>
 * If {@link Mode} is equal to {@link Mode#DISABLED}, then Notification Services will drain any JMS messages in the
 * queue, and acknowledge (and immediately discard) any new messages it receives.
 * </p>
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@EnableJms
public class JmsConfig {

    private static final Logger LOG = LoggerFactory.getLogger(JmsConfig.class);

    @Autowired
    private JsonParser jsonParser;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationConfig config;

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(@Value("${spring.jms.listener.concurrency}")
                                                                                  String concurrency,
                                                                          @Value("${spring.jms.listener.auto-startup}")
                                                                                  boolean autoStart,
                                                                          ConnectionFactory connectionFactory,
                                                                          ErrorHandler errorHandler) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
        factory.setErrorHandler(errorHandler);
        factory.setConcurrency(concurrency);
        factory.setConnectionFactory(connectionFactory);
        factory.setAutoStartup(autoStart);
        return factory;
    }

    @JmsListener(destination = "${pass.notification.queue.event.name}", containerFactory = "jmsListenerContainerFactory")
    public void processMessage(@Header(Constants.JmsFcrepoHeader.FCREPO_RESOURCE_TYPE) String resourceType,
                                         @Header(Constants.JmsFcrepoHeader.FCREPO_EVENT_TYPE) String eventType,
                                         @Header(JmsHeaders.MESSAGE_ID) String id,
                                         Message<String> message,
                                         javax.jms.Message jmsMessage) {

        if (Mode.DISABLED == config.getMode()) {
            try {
                jmsMessage.acknowledge();
            } catch (JMSException e) {
                LOG.warn("Error acknowledging JMS message {}: {}", id, e.getMessage(), e);
            }
            return;
        }

        if (!resourceType.equals(Constants.PassType.SUBMISSION_EVENT_RESOURCE) ||
                !eventType.equals(Constants.JmsFcrepoEvent.RESOURCE_CREATION)) {
            try {
                jmsMessage.acknowledge();
            } catch (JMSException e) {
                LOG.warn("Error acknowledging JMS message {}: {}", id, e.getMessage(), e);
            }
            return;
        }

        String eventUri = jsonParser.parseId(message.getPayload().getBytes());
        notificationService.notify(eventUri);

        try {
            jmsMessage.acknowledge();
        } catch (JMSException e) {
            LOG.warn("Error acknowledging JMS message {}: {}", id, e.getMessage(), e);
        }

    }

}
