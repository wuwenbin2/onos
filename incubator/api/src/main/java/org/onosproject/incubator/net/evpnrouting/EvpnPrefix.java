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

package org.onosproject.incubator.net.evpnrouting;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onlab.packet.MacAddress;

/**
 * Represents a route.
 */
public final class EvpnPrefix {

    private final RouteDistinguisher rd;
    private final MacAddress prefix;

    // new add
    private EvpnPrefix(RouteDistinguisher rd, MacAddress prefix) {
        checkNotNull(rd);
        checkNotNull(prefix);
        this.rd = rd;
        this.prefix = prefix;
    }

    public static EvpnPrefix evpnPrefix(RouteDistinguisher rd,
                                        MacAddress prefix) {
        return new EvpnPrefix(rd, prefix);
    }

    public RouteDistinguisher routeDistinguisher() {
        return rd;
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
        return Objects.hash(rd, prefix);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof EvpnPrefix)) {
            return false;
        }

        EvpnPrefix that = (EvpnPrefix) other;

        return Objects.equals(this.prefix(), that.prefix())
                && Objects.equals(this.rd, that.rd);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("prefix", this.prefix())
                .add("rd", this.rd).toString();
    }
}
