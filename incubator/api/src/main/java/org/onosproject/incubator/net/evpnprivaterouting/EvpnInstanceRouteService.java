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

import java.util.Collection;
import java.util.Map;

import org.onosproject.event.ListenerService;
import org.onosproject.incubator.net.evpnrouting.RouteDistinguisher;
import org.onosproject.incubator.net.evpnrouting.RouteTarget;

/**
 * Unicast IP route service.
 */
public interface EvpnInstanceRouteService
        extends
        ListenerService<EvpnInstanceRouteEvent, EvpnInstanceRouteListener> {

    /**
     * Returns all routes for all route tables in the system.
     *
     * @return map of route table name to routes in that table
     */
    Collection<EvpnInstanceRoute> getAllRoutes();

    /**
     * Get Route Map by Vpn instance name.
     *
     * @param name
     * @return map of route
     */
    Map<EvpnInstancePrefix, EvpnInstanceNextHop> getRouteMapByInstanceName(EvpnInstanceName name);

    /**
     * Get RouteTarget by Vpn instance name.
     *
     * @param name
     * @return RouteTarget
     */
    RouteTarget getRtByInstanceName(EvpnInstanceName name);

    /**
     * Get Rd by Vpn instance name.
     *
     * @param name
     * @return rd
     */
    RouteDistinguisher getRdByInstanceName(EvpnInstanceName name);

}
