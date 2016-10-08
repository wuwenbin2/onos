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

import org.onosproject.incubator.net.evpnrouting.RouteDistinguisher;
import org.onosproject.incubator.net.evpnrouting.RouteTarget;

/**
 * Represents a route.
 */
public final class EvpnMessage {

    private final RouteDistinguisher rd;
    private final RouteTarget rt;
    private final EvpnName evpnName;

    // new add
    private EvpnMessage(RouteDistinguisher rd, RouteTarget rt,
                        EvpnName evpnName) {
        checkNotNull(rd);
        checkNotNull(rt);
        checkNotNull(evpnName);
        this.rd = rd;
        this.rt = rt;
        this.evpnName = evpnName;
    }

    public static EvpnMessage evpnMessage(RouteDistinguisher rd, RouteTarget rt,
                                          EvpnName evpnName) {
        return new EvpnMessage(rd, rt, evpnName);
    }

    public RouteDistinguisher routeDistinguisher() {
        return rd;
    }

    public RouteTarget routeTarget() {
        return rt;
    }

    public EvpnName evpnName() {
        return evpnName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rd, rt, evpnName);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof EvpnMessage)) {
            return false;
        }

        EvpnMessage that = (EvpnMessage) other;

        return Objects.equals(this.evpnName, that.evpnName)
                && Objects.equals(this.rd, that.rd)
                && Objects.equals(this.rt, that.rt);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("evpnName", this.evpnName)
                .add("rd", this.rd).add("rt", this.rt).toString();
    }
}
