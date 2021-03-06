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

package org.dataconservancy.pass.notification.model.config;

import java.util.Objects;

/**
 * Configuration for the user token generator for invitation links.
 *
 * @author apb@jhu.edu
 */
public class UserTokenGeneratorConfig {

    private String key;

    /**
     * Set the key
     *
     * @param key Base32-encoded token encryption key
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Get the key
     *
     * @return The key
     */
    public String getKey() {
        return this.key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final UserTokenGeneratorConfig that = (UserTokenGeneratorConfig) o;
        return Objects.equals(this.key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

}
