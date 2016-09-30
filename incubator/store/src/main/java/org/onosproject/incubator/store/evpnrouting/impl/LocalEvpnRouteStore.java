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

package org.onosproject.incubator.store.evpnrouting.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.incubator.net.evpnrouting.EvpnNextHop;
import org.onosproject.incubator.net.evpnrouting.EvpnPrefix;
import org.onosproject.incubator.net.evpnrouting.EvpnRoute;
import org.onosproject.incubator.net.evpnrouting.EvpnRouteEvent;
import org.onosproject.incubator.net.evpnrouting.EvpnRouteStore;
import org.onosproject.incubator.net.evpnrouting.EvpnRouteStoreDelegate;
import org.onosproject.store.AbstractStore;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * Route store based on in-memory storage.
 */
@Service
@Component
public class LocalEvpnRouteStore
        extends AbstractStore<EvpnRouteEvent, EvpnRouteStoreDelegate>
        implements EvpnRouteStore {

    private EvpnRouteTable routeTable = new EvpnRouteTable();
    // private static final RouteTableId IPV4 = new RouteTableId("ipv4");
    // private static final RouteTableId IPV6 = new RouteTableId("ipv6");

    @Activate
    public void activate() {
    }

    @Deactivate
    public void deactivate() {
    }

    @Override
    public void updateEvpnRoute(EvpnRoute route) {
        routeTable.update(route);
    }

    @Override
    public void removeEvpnRoute(EvpnRoute route) {
        routeTable.remove(route);
    }

    @Override
    public Collection<EvpnRoute> getEvpnRoutes() {
        if (routeTable == null) {
            return Collections.emptySet();
        }
        return routeTable.getRoutes();
    }

    /**
     * Route table into which routes can be placed.
     */
    private class EvpnRouteTable {

        private final Map<EvpnPrefix, EvpnRoute> routesMap = new ConcurrentHashMap<>();
        private final Multimap<EvpnNextHop, EvpnRoute> reverseIndex = Multimaps
                .synchronizedMultimap(HashMultimap.create());

        /**
         * Adds or updates the route in the route table.
         *
         * @param route route to update
         */
        public void update(EvpnRoute route) {
            synchronized (this) {
                EvpnPrefix prefix = EvpnPrefix
                        .evpnPrefix(route.routeDistinguisher(), route.prefix());
                EvpnNextHop nextHop = EvpnNextHop.evpnNextHop(route.nextHop(),
                                                              route.routeTarget(),
                                                              route.label());
                EvpnRoute oldRoute = routesMap.put(prefix, route);

                // TODO manage routes from multiple providers

                reverseIndex.put(nextHop, route);

                if (oldRoute != null) {
                    EvpnNextHop odlNextHop = EvpnNextHop
                            .evpnNextHop(oldRoute.nextHop(),
                                         oldRoute.routeTarget(),
                                         oldRoute.label());
                    reverseIndex.remove(odlNextHop, oldRoute);
                }

                if (route.equals(oldRoute)) {
                    // No need to send events if the new route is the same
                    return;
                }

                if (oldRoute != null) {
                    notifyDelegate(new EvpnRouteEvent(EvpnRouteEvent.Type.ROUTE_REMOVED,
                                                      oldRoute));
                    // notifyDelegate(new
                    // EvpnRouteEvent(EvpnRouteEvent.Type.ROUTE_UPDATED,
                    // route));
                }
                notifyDelegate(new EvpnRouteEvent(EvpnRouteEvent.Type.ROUTE_ADDED,
                                                  route));
                return;
            }
        }

        /**
         * Removes the route from the route table.
         *
         * @param route route to remove
         */
        public void remove(EvpnRoute route) {
            synchronized (this) {
                EvpnPrefix prefix = EvpnPrefix
                        .evpnPrefix(route.routeDistinguisher(), route.prefix());
                EvpnRoute removedRoute = routesMap.remove(prefix);

                if (removedRoute != null) {
                    reverseIndex.remove(removedRoute.nextHop(), removedRoute);
                    notifyDelegate(new EvpnRouteEvent(EvpnRouteEvent.Type.ROUTE_REMOVED,
                                                      removedRoute));
                }
            }
        }

        public Collection<EvpnRoute> getRoutes() {
            List<EvpnRoute> routes = new LinkedList<>();
            for (Map.Entry<EvpnPrefix, EvpnRoute> e : routesMap.entrySet()) {
                routes.add(e.getValue());
            }
            return routes;
        }

    }

}
