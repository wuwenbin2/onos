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

package org.onosproject.evpn.rsc;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.onosproject.incubator.net.routing.EvpnInstanceName;
import org.onosproject.incubator.net.routing.RouteDistinguisher;
import org.onosproject.incubator.net.routing.RouteTarget;

/**
 * Default implementation of vpn instance.
 */
public class DefaultVpnInstance implements VpnInstance {
    private final VpnInstanceId id;
    private final String description;
    private final EvpnInstanceName name;
    private final RouteDistinguisher routeDistinguisher;
    private final RouteTarget routeTarget;

    /**
     * creates vpn instance object.
     *
     * @param id vpn instance identifier
     * @param name the name of vpn instance
     * @param description the description of vpn instance
     * @param routeDistinguisher the routeDistinguisher of vpn instance
     * @param routeTarget the routeTarget of vpn instance
     * @param createdAt the create time of vpn instance
     * @param updatedAt the update time of vpn instance
     */
    public DefaultVpnInstance(VpnInstanceId id, EvpnInstanceName name,
                              String description,
                              RouteDistinguisher routeDistinguisher,
                              RouteTarget routeTarget) {
        this.id = checkNotNull(id, "id cannot be null");
        this.name = checkNotNull(name, "name cannot be null");
        this.description = checkNotNull(description,
                                        "description cannot be null");
        this.routeDistinguisher = checkNotNull(routeDistinguisher,
                                               "routeDistinguisher cannot be null");
        this.routeTarget = checkNotNull(routeTarget,
                                        "routeTarget cannot be null");
    }

    @Override
    public VpnInstanceId id() {
        return id;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public RouteDistinguisher routeDistinguishers() {
        return routeDistinguisher;
    }

    @Override
    public EvpnInstanceName vpnInstanceName() {
        return name;
    }

    @Override
    public RouteTarget routeTarget() {
        return routeTarget;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, routeDistinguisher,
                            routeTarget);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultVpnInstance) {
            final DefaultVpnInstance that = (DefaultVpnInstance) obj;
            return Objects.equals(this.id, that.id)
                    && Objects.equals(this.name, that.name)
                    && Objects.equals(this.description, that.description)
                    && Objects.equals(this.routeDistinguisher,
                                      that.routeDistinguisher)
                    && Objects.equals(this.routeTarget, that.routeTarget);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("id", id).add("vpnInstanceName", name)
                .add("description", description)
                .add("routeDistinguisher", routeDistinguisher).toString();
    }
}
