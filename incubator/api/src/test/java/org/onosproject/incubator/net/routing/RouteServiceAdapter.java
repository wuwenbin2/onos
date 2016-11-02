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
import org.onosproject.incubator.net.routing.EvpnRoute.OperationType;

/**
 * Adapter class for the route service.
 */
public class RouteServiceAdapter implements RouteAdminService {
    @Override
    public void update(Collection<Route> routes) {

    }

    @Override
    public void withdraw(Collection<Route> routes) {

    }

    @Override
    public Map<RouteTableType, Collection<Route>> getAllRoutes() {
        return null;
    }

    @Override
    public IpRoute longestPrefixMatch(IpAddress ip) {
        return null;
    }

    @Override
    public Set<NextHop> getNextHops() {
        return null;
    }

    @Override
    public void addListener(RouteListener listener) {

    }

    @Override
    public void removeListener(RouteListener listener) {

    }

    @Override
    public Collection<Route> getRoutesForNextHop(RouteTableType id,
                                                 NextHop nextHop) {
        return null;
    }

    @Override
    public Map<EvpnInstancePrefix, EvpnInstanceNextHop> getRouteMapByInstanceName(EvpnInstanceName name) {
        return null;
    }

    @Override
    public RouteTarget getRtByInstanceName(EvpnInstanceName name) {
        return null;
    }

    @Override
    public RouteDistinguisher getRdByInstanceName(EvpnInstanceName name) {
        return null;
    }

    @Override
    public void sendEvpnMessage(OperationType remove, EvpnRoute evpnRoute) {
    }

}
