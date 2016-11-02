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

import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;

/**
 * Represents a route.
 */
public final class EvpnPrefix implements Prefix {

    private final RouteDistinguisher rd;
    private final MacAddress macAddress;
    private final Ip4Address ipAddress;

    // new add
    private EvpnPrefix(RouteDistinguisher rd, MacAddress macAddress,
                       Ip4Address ipAddress) {
        checkNotNull(rd);
        checkNotNull(macAddress);
        checkNotNull(ipAddress);
        this.rd = rd;
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
    }

    public static EvpnPrefix evpnPrefix(RouteDistinguisher rd,
                                        MacAddress macAddress,
                                        Ip4Address ipAddress) {
        return new EvpnPrefix(rd, macAddress, ipAddress);
    }

    public RouteDistinguisher routeDistinguisher() {
        return rd;
    }

    /**
     * Returns the IP prefix of the route.
     *
     * @return IP prefix
     */
    public MacAddress macAddress() {
        return macAddress;
    }

    public Ip4Address ipAddress() {
        return ipAddress;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rd, macAddress, ipAddress);
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

        return Objects.equals(this.macAddress(), that.macAddress())
                && Objects.equals(this.ipAddress, that.ipAddress)
                && Objects.equals(this.rd, that.rd);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("macAddress", this.macAddress())
                .add("ipAddress", this.ipAddress()).add("rd", this.rd)
                .toString();
    }
}
