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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.store.Store;

/**
 * Unicast route store.
 */
public interface RouteStore extends Store<RouteEvent, RouteStoreDelegate> {

    /**
     * Adds or updates the given route in the store.
     *
     * @param route route to add or update
     */
    void updateRoute(Route route);

    /**
     * Removes the given route from the store.
     *
     * @param route route to remove
     */
    void removeRoute(Route route);

    /**
     * Returns the IDs for all route tables in the store.
     *
     * @return route table IDs
     */
    Set<RouteTableType> getRouteTables();

    /**
     * Returns the routes for a particular route table.
     *
     * @param table route table
     * @return collection of route in the table
     */
    Collection<Route> getRoutes(RouteTableType table);

    /**
     * Returns the routes that point to the given next hop IP address.
     *
     * @param ip IP address of the next hop
     * @return routes for the given next hop
     */
    Collection<Route> getRoutesForNextHop(RouteTableType table,
                                          NextHop nextHop);

    /**
     * Updates a next hop IP and MAC in the store.
     *
     * @param ip IP address
     * @param mac MAC address
     */
    void updateNextHop(IpAddress ip, MacAddress mac);

    /**
     * Removes a next hop IP and MAC from the store.
     *
     * @param ip IP address
     * @param mac MAC address
     */
    void removeNextHop(IpAddress ip, MacAddress mac);

    /**
     * Returns the MAC address of the given next hop.
     *
     * @param ip next hop IP
     * @return MAC address
     */
    MacAddress getNextHop(IpAddress ip);

    /**
     * Performs a longest prefix match with the given IP address.
     *
     * @param ip IP to look up
     * @return longest prefix match route
     */
    IpRoute longestPrefixMatch(IpAddress ip);

    /**
     * Returns all next hops in the route store.
     *
     * @return next hops
     */
    Map<IpAddress, MacAddress> getNextHops();

    /**
     * Get routes by the given instance name.
     *
     * @param evpnName the given evpn name
     * @return collection of EvpnInstanceRoute
     */
    Collection<EvpnInstanceRoute> getEvpnRoutes(EvpnInstanceName evpnName);

    /**
     * Get RouteTarget by Vpn instance name.
     *
     * @param name
     * @return RouteTarget
     */
    RouteTarget getRtByInstanceName(EvpnInstanceName name);

    /**
     * Get Route Map by Vpn instance name.
     *
     * @param name
     * @return map of route
     */
    Map<EvpnInstancePrefix, EvpnInstanceNextHop> getRouteMapByInstanceName(EvpnInstanceName name);

    /**
     * Get RouteDistinguisher by Vpn instance name.
     *
     * @param name
     * @return RouteDistinguisher
     */
    RouteDistinguisher getRdByInstanceName(EvpnInstanceName name);

}
