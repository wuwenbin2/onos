/*
 * Copyright 2015-present Open Networking Laboratory
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

/**
 * Implementation of RouteDistinguisher.
 */
package org.onosproject.incubator.net.routing;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

/**
 * Represents Route Distinguisher of device in the network.
 */
public final class EvpnInstanceName {
    private final String evpnName;

    /**
     * Constructor to initialize parameters.
     *
     * @param routeDistinguisher route distinguisher
     */
    private EvpnInstanceName(String evpnName) {
        this.evpnName = evpnName;
    }

    public static EvpnInstanceName evpnName(String evpnName) {
        return new EvpnInstanceName(evpnName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(evpnName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof EvpnInstanceName) {
            EvpnInstanceName other = (EvpnInstanceName) obj;
            return Objects.equals(evpnName, other.evpnName);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("evpnName", evpnName).toString();
    }
}
