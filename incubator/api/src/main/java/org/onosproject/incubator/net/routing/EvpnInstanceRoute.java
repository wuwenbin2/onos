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

package org.onosproject.incubator.net.routing;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * Represents a route.
 */
public class EvpnInstanceRoute implements Route {

    private final EvpnInstanceName evpnName;
    private final RouteDistinguisher rd;
    private final RouteTarget rt;
    private final EvpnInstancePrefix prefix;
    private final EvpnInstanceNextHop nextHop;

    // new add
    public EvpnInstanceRoute(EvpnInstanceName evpnName, RouteDistinguisher rd,
                             RouteTarget rt, EvpnInstancePrefix prefix,
                             EvpnInstanceNextHop nextHop) {
        checkNotNull(prefix);
        checkNotNull(nextHop);
        this.evpnName = evpnName;
        this.rd = rd;
        this.rt = rt;
        this.prefix = prefix;
        this.nextHop = nextHop;
    }

    /**
     * Returns the IP prefix of the route.
     *
     * @return IP prefix
     */
    public EvpnInstanceName evpnInstanceName() {
        return evpnName;
    }

    /**
     * Returns the IP prefix of the route.
     *
     * @return IP prefix
     */
    public RouteDistinguisher routeDistinguisher() {
        return rd;
    }

    /**
     * Returns the IP prefix of the route.
     *
     * @return IP prefix
     */
    public RouteTarget routeTarget() {
        return rt;
    }

    /**
     * Returns the IP prefix of the route.
     *
     * @return IP prefix
     */
    public EvpnInstancePrefix prefix() {
        return prefix;
    }

    /**
     * Returns the next hop IP address.
     *
     * @return next hop
     */
    public EvpnInstanceNextHop nextHop() {
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

        if (!(other instanceof EvpnInstanceRoute)) {
            return false;
        }

        EvpnInstanceRoute that = (EvpnInstanceRoute) other;

        return Objects.equals(prefix, prefix)
                && Objects.equals(nextHop, that.nextHop);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("prefix", prefix)
                .add("nextHop", nextHop).toString();
    }
}
