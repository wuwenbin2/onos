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

import java.util.Objects;

import org.onlab.packet.IpAddress;

/**
 * Represents a route.
 */
public final class EvpnNextHop implements NextHop {

    private final IpAddress nextHop;
    private final RouteTarget rt;
    private final Label label;

    // new add
    private EvpnNextHop(IpAddress nextHop, RouteTarget rt, Label label) {
        this.nextHop = nextHop;
        this.rt = rt;
        this.label = label;
    }

    public static EvpnNextHop evpnNextHop(IpAddress nextHop, RouteTarget rt,
                                          Label label) {
        return new EvpnNextHop(nextHop, rt, label);
    }

    /**
     * Returns the next hop IP address.
     *
     * @return next hop
     */
    public IpAddress nextHop() {
        return nextHop;
    }

    public RouteTarget routeTarget() {
        return rt;
    }

    public Label label() {
        return label;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nextHop, rt, label);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof EvpnNextHop)) {
            return false;
        }

        EvpnNextHop that = (EvpnNextHop) other;

        return Objects.equals(this.nextHop(), that.nextHop())
                && Objects.equals(this.rt, that.rt)
                && Objects.equals(this.label, that.label);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("nextHop", this.nextHop())
                .add("rt", this.rt).add("label", this.label).toString();
    }
}
