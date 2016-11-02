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

import org.onosproject.incubator.net.routing.EvpnInstanceName;
import org.onosproject.incubator.net.routing.RouteDistinguisher;
import org.onosproject.incubator.net.routing.RouteTarget;

/**
 * Representation of a VPNInstance.
 */
public interface VpnInstance {

    /**
     * Returns the VPN instance identifier.
     *
     * @return VPN instance identifier
     */
    VpnInstanceId id();

    /**
     * Returns the VPN instance description.
     *
     * @return VPN instance description
     */
    String description();

    /**
     * Returns the VPN instance route distinguishes.
     *
     * @return VPN instance route distinguishes
     */
    RouteDistinguisher routeDistinguishers();

    /**
     * Returns the VPN instance name.
     *
     * @return VPN instance name
     */
    EvpnInstanceName vpnInstanceName();

    /**
     * Returns the VPN instance ipv4 family.
     *
     * @return VPN instance ipv4 family
     */
    RouteTarget routeTarget();
}
