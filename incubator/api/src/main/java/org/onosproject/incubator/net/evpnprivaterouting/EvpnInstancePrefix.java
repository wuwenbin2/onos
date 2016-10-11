/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.incubator.net.evpnprivaterouting;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onlab.packet.MacAddress;

/**
 * Represents a route.
 */
public final class EvpnInstancePrefix {

    private final MacAddress prefix;

    // new add
    private EvpnInstancePrefix(EvpnInstance em, MacAddress prefix) {
        checkNotNull(prefix);
        this.prefix = prefix;
    }

    public static EvpnInstancePrefix evpnPrefix(EvpnInstance em,
                                                MacAddress prefix) {
        return new EvpnInstancePrefix(em, prefix);
    }

    /**
     * Returns the IP prefix of the route.
     *
     * @return IP prefix
     */
    public MacAddress prefix() {
        return prefix;
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof EvpnInstancePrefix)) {
            return false;
        }

        EvpnInstancePrefix that = (EvpnInstancePrefix) other;

        return Objects.equals(this.prefix, that.prefix);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("prefix", this.prefix).toString();
    }
}
