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
     * operation of the evpn route.
     */
    public enum OperationType {
        /**
         * Update.
         */
        UPDATE,
        /**
         * Remove.
         */
        REMOVE,
    }

    /**
     * Source of the route.
     */
    public enum Source {
        /**
         * Route came from app source.
         */
        LOCAL,

        /**
         * Route came from remote bgp peer source.
         */
        REMOTE,
    }

    private final Source source;
    private final MacAddress prefixMac;
    private final Ip4Address prefixIp;
    private final Ip4Address nextHop;
    private final RouteDistinguisher rd;
    private final RouteTarget rt;
    private final Label label;

    // new add
    public EvpnRoute(Source source, MacAddress prefixMac, Ip4Address prefixIp,
                     Ip4Address nextHop, RouteDistinguisher rd, RouteTarget rt,
                     Label label) {
        checkNotNull(prefixMac);
        checkNotNull(prefixIp);
        checkNotNull(rd);
        checkNotNull(rt);
        checkNotNull(label);
        this.source = checkNotNull(source);
        this.prefixMac = prefixMac;
        this.prefixIp = prefixIp;
        this.nextHop = nextHop;
        this.rd = rd;
        this.rt = rt;
        this.label = label;
    }

    public EvpnRoute(Source source, MacAddress prefixMac, Ip4Address prefixIp,
                     Ip4Address nextHop, String rdToString, String rtToString,
                     int labelToInt) {
        checkNotNull(prefixMac);
        checkNotNull(prefixIp);
        checkNotNull(rdToString);
        checkNotNull(labelToInt);
        this.source = checkNotNull(source);
        this.prefixMac = prefixMac;
        this.prefixIp = prefixIp;
        this.nextHop = nextHop;
        this.rd = RouteDistinguisher.routeDistinguisher(rdToString);
        if (rtToString != null) {
            this.rt = RouteTarget.routeTarget(rtToString);
        } else {
            this.rt = null;
        }
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
    public MacAddress prefixMac() {
        return prefixMac;
    }

    /**
     * Returns the IP prefix of the route.
     *
     * @return IP prefix
     */
    public Ip4Address prefixIp() {
        return prefixIp;
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
        return Objects.hash(prefixMac, prefixIp, nextHop, rd, rt, label);
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

        return Objects.equals(prefixMac, prefixMac)
                && Objects.equals(prefixIp, that.prefixIp)
                && Objects.equals(nextHop, that.nextHop)
                && Objects.equals(this.rd, that.rd)
                && Objects.equals(this.rt, that.rt)
                && Objects.equals(this.label, that.label);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("prefixMac", prefixMac)
                .add("prefixIp", prefixIp).add("nextHop", nextHop)
                .add("rd", this.rd).add("rt", this.rt).add("label", this.label)
                .toString();
    }
}
