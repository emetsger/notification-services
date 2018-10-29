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

import org.dataconservancy.pass.notification.app.config.JmsConfig;
import org.dataconservancy.pass.notification.app.config.SpringBootNotificationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * Spring Boot entry point for launching Notification Services.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@SpringBootApplication
@Import(SpringBootNotificationConfig.class)
@ComponentScan("org.dataconservancy.pass")
@EnableAspectJAutoProxy
public class NotificationApp {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationApp.class);

    private static final String GIT_BUILD_VERSION_KEY = "git.build.version";

    private static final String GIT_COMMIT_HASH_KEY = "git.commit.id.abbrev";

    private static final String GIT_COMMIT_TIME_KEY = "git.commit.time";

    private static final String GIT_DIRTY_FLAG = "git.dirty";

    private static final String GIT_BRANCH = "git.branch";

    private static final String GIT_PROPERTIES_RESOURCE_PATH = "/git.properties";

    /**
     * Spring Boot entry point.
     *
     * @param args command line args to be processed
     */
    public static void main(String[] args) {
        URL gitPropertiesResource = NotificationApp.class.getResource(GIT_PROPERTIES_RESOURCE_PATH);
        if (gitPropertiesResource == null) {
            LOG.info(">>>> Starting DepositServices (no Git commit information available)");
        } else {
            Properties gitProperties = new Properties();
            try {
                gitProperties.load(gitPropertiesResource.openStream());
                boolean isDirty = Boolean.valueOf(gitProperties.getProperty(GIT_DIRTY_FLAG));

                LOG.info(">>>> Starting Notification Services (version: {} branch: {} commit: {} commit date: {})",
                        gitProperties.get(GIT_BUILD_VERSION_KEY), gitProperties.get(GIT_BRANCH), gitProperties.get(GIT_COMMIT_HASH_KEY), gitProperties.getProperty(GIT_COMMIT_TIME_KEY));

                if (isDirty) {
                    LOG.warn(">>>> ** Notification Services was compiled from a Git repository with uncommitted changes! **");
                }
            } catch (IOException e) {
                LOG.warn(">>>> Error parsing Notification Services git information (" + GIT_PROPERTIES_RESOURCE_PATH + " could not be parsed: " + e.getMessage() + ")");
            }
        }

        SpringApplicationBuilder appBuilder = new SpringApplicationBuilder();
        appBuilder.main(NotificationApp.class)
                .sources(NotificationApp.class, JmsConfig.class)
                .run(args);

    }

}
