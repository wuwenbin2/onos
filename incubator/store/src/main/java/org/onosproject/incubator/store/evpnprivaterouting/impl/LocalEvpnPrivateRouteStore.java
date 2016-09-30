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

package org.onosproject.incubator.store.evpnprivaterouting.impl;

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
import org.onosproject.incubator.net.evpnprivaterouting.EvpnPrivateNextHop;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnPrivatePrefix;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnPrivateRoute;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnPrivateRouteEvent;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnPrivateRouteStore;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnPrivateRouteStoreDelegate;
import org.onosproject.store.AbstractStore;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * Route store based on in-memory storage.
 */
@Service
@Component
public class LocalEvpnPrivateRouteStore
        extends
        AbstractStore<EvpnPrivateRouteEvent, EvpnPrivateRouteStoreDelegate>
        implements EvpnPrivateRouteStore {

    private EvpnPrivateRouteTable routeTable = new EvpnPrivateRouteTable();

    @Activate
    public void activate() {
    }

    @Deactivate
    public void deactivate() {
    }

    @Override
    public void updateEvpnRoute(EvpnPrivateRoute route) {
        routeTable.update(route);
    }

    @Override
    public void removeEvpnRoute(EvpnPrivateRoute route) {
        routeTable.remove(route);
    }

    @Override
    public Collection<EvpnPrivateRoute> getEvpnRoutes() {
        if (routeTable == null) {
            return Collections.emptySet();
        }
        return routeTable.getRoutes();
    }

    /**
     * Route table into which routes can be placed.
     */
    private class EvpnPrivateRouteTable {

        private final Map<EvpnPrivatePrefix, EvpnPrivateRoute> routesMap = new ConcurrentHashMap<>();
        private final Multimap<EvpnPrivateNextHop, EvpnPrivateRoute> reverseIndex = Multimaps
                .synchronizedMultimap(HashMultimap.create());

        /**
         * Adds or updates the route in the route table.
         *
         * @param route route to update
         */
        public void update(EvpnPrivateRoute route) {
            synchronized (this) {
                EvpnPrivateRoute oldRoute = routesMap.put(route.prefix(),
                                                          route);
                reverseIndex.put(route.nextHop(), route);

                if (oldRoute != null) {
                    reverseIndex.remove(oldRoute.nextHop(), oldRoute);
                }

                if (route.equals(oldRoute)) {
                    // No need to send events if the new route is the same
                    return;
                }

                if (oldRoute != null) {
                    notifyDelegate(new EvpnPrivateRouteEvent(EvpnPrivateRouteEvent.Type.ROUTE_REMOVED,
                                                             oldRoute));
                }
                notifyDelegate(new EvpnPrivateRouteEvent(EvpnPrivateRouteEvent.Type.ROUTE_ADDED,
                                                         route));
                return;
            }
        }

        /**
         * Removes the route from the route table.
         *
         * @param route route to remove
         */
        public void remove(EvpnPrivateRoute route) {
            synchronized (this) {
                EvpnPrivateRoute removedRoute = routesMap
                        .remove(route.prefix());

                if (removedRoute != null) {
                    reverseIndex.remove(removedRoute.nextHop(), removedRoute);
                    notifyDelegate(new EvpnPrivateRouteEvent(EvpnPrivateRouteEvent.Type.ROUTE_REMOVED,
                                                             removedRoute));
                }
            }
        }

        public Collection<EvpnPrivateRoute> getRoutes() {
            List<EvpnPrivateRoute> routes = new LinkedList<>();
            for (Map.Entry<EvpnPrivatePrefix, EvpnPrivateRoute> e : routesMap
                    .entrySet()) {
                routes.add(e.getValue());
            }
            return routes;
        }

    }

}
