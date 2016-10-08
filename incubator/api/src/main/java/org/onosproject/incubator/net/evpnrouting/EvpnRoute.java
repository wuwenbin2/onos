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

import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;

/**
 * Represents a route.
 */
public class EvpnRoute {

    /**
     * Source of the route.
     */
    public enum Source {
        /**
         * Route came from the iBGP route source.
         */
        BGP,

        /**
         * Route came from the FPM route source.
         */
        FPM,

        /**
         * Route can from the static route source.
         */
        STATIC,

        /**
         * Route source was not defined.
         */
        UNDEFINED
    }

    private final Source source;
    private final MacAddress prefix;
    private final Ip4Address nextHop;
    private final RouteDistinguisher rd;
    private final RouteTarget rt;
    private final Label label;

    // new add
    public EvpnRoute(Source source, MacAddress prefix, Ip4Address nextHop,
                     RouteDistinguisher rd, RouteTarget rt, Label label) {
        checkNotNull(prefix);
        checkNotNull(nextHop);
        checkNotNull(rd);
        checkNotNull(rt);
        checkNotNull(label);
        this.source = checkNotNull(source);
        this.prefix = prefix;
        this.nextHop = nextHop;
        this.rd = rd;
        this.rt = rt;
        this.label = label;
    }

    public EvpnRoute(Source source, MacAddress prefix, Ip4Address nextHop,
                     String rdToString, String rtToString, int labelToInt) {
        checkNotNull(prefix);
        checkNotNull(nextHop);
        checkNotNull(rdToString);
        checkNotNull(rtToString);
        checkNotNull(labelToInt);
        this.source = checkNotNull(source);
        this.prefix = prefix;
        this.nextHop = nextHop;
        this.rd = RouteDistinguisher.routeDistinguisher(rdToString);
        this.rt = RouteTarget.routeTarget(rtToString);
        this.label = Label.label(labelToInt);
    }

    /**
     * Returns the route source.
     *
     * @return route source
     */
    public Source source() {
        return source;
    }

    /**
     * Returns the IP prefix of the route.
     *
     * @return IP prefix
     */
    public MacAddress prefix() {
        return prefix;
    }

    /**
     * Returns the next hop IP address.
     *
     * @return next hop
     */
    public Ip4Address nextHop() {
        return nextHop;
    }

    public RouteDistinguisher routeDistinguisher() {
        return rd;
    }

    public RouteTarget routeTarget() {
        return rt;
    }

    public Label label() {
        return label;
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, nextHop, rd, rt, label);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof EvpnRoute)) {
            return false;
        }

        EvpnRoute that = (EvpnRoute) other;

        return Objects.equals(prefix, prefix)
                && Objects.equals(nextHop, that.nextHop)
                && Objects.equals(this.rd, that.rd)
                && Objects.equals(this.rt, that.rt)
                && Objects.equals(this.label, that.label);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("prefix", prefix)
                .add("nextHop", nextHop).add("rd", this.rd).add("rt", this.rt)
                .add("label", this.label).toString();
    }
}
