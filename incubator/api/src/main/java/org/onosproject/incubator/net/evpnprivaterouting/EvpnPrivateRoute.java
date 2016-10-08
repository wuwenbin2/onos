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

/**
 * Represents a route.
 */
public class EvpnPrivateRoute {

    private final EvpnPrivatePrefix prefix;
    private final EvpnPrivateNextHop nextHop;

    // new add
    public EvpnPrivateRoute(EvpnPrivatePrefix prefix, EvpnPrivateNextHop nextHop) {
        checkNotNull(prefix);
        checkNotNull(nextHop);
        this.prefix = prefix;
        this.nextHop = nextHop;
    }

    /**
     * Returns the IP prefix of the route.
     *
     * @return IP prefix
     */
    public EvpnPrivatePrefix prefix() {
        return prefix;
    }

    /**
     * Returns the next hop IP address.
     *
     * @return next hop
     */
    public EvpnPrivateNextHop nextHop() {
        return nextHop;
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, nextHop);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof EvpnPrivateRoute)) {
            return false;
        }

        EvpnPrivateRoute that = (EvpnPrivateRoute) other;

        return Objects.equals(prefix, prefix)
                && Objects.equals(nextHop, that.nextHop);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("prefix", prefix)
                .add("nextHop", nextHop).toString();
    }
}
