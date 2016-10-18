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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceName;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceNextHop;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstancePrefix;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceRoute;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceRouteEvent;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceRouteStore;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceRouteStoreDelegate;
import org.onosproject.incubator.net.evpnrouting.RouteDistinguisher;
import org.onosproject.incubator.net.evpnrouting.RouteTarget;
import org.onosproject.store.AbstractStore;

import com.google.common.collect.Lists;

/**
 * Route store based on in-memory storage.
 */
@Service
@Component
public class LocalEvpnPrivateRouteStore
        extends
        AbstractStore<EvpnInstanceRouteEvent, EvpnInstanceRouteStoreDelegate>
        implements EvpnInstanceRouteStore {

    private Map<EvpnInstanceName, EvpnInstanceRouteTable> routeTables = new HashMap<>();;

    @Activate
    public void activate() {
    }

    @Deactivate
    public void deactivate() {
    }

    @Override
    public void updateEvpnRoute(EvpnInstanceRoute route) {
        if (routeTables.get(route.evpnInstanceName()) == null) {
            EvpnInstanceRouteTable evpnInstanceRouteTable = new EvpnInstanceRouteTable(route
                    .evpnInstanceName(), route
                            .routeDistinguisher(), route.routeTarget());
            routeTables.put(route.evpnInstanceName(), evpnInstanceRouteTable);
        } else {
            routeTables.get(route.evpnInstanceName()).update(route);
        }

    }

    @Override
    public void removeEvpnRoute(EvpnInstanceRoute route) {
        if (routeTables.get(route.evpnInstanceName()) == null) {
            return;
        }
        routeTables.get(route.evpnInstanceName()).remove(route);
    }

    @Override
    public Collection<EvpnInstanceRoute> getEvpnRoutes(EvpnInstanceName evpnName) {
        if (routeTables == null) {
            return Collections.emptySet();
        }
        return routeTables.get(evpnName).getRoutes();
    }

    @Override
    public Collection<EvpnInstanceRoute> getEvpnRoutes() {
        if (routeTables == null) {
            return Collections.emptySet();
        }
        Collection<EvpnInstanceRoute> list = Lists.newLinkedList();
        routeTables.keySet().forEach(evpnName -> {
            list.addAll(routeTables.get(evpnName).getRoutes());
        });
        return list;
    }
    @Override
    public Map<EvpnInstancePrefix, EvpnInstanceNextHop> getRouteMapByInstanceName(EvpnInstanceName name) {
        if (routeTables == null) {
            return null;
        }
        return routeTables.get(name).routesMap;
    }
    @Override
    public RouteTarget getRtByInstanceName(EvpnInstanceName name) {
        if (routeTables == null) {
            return null;
        }
        return routeTables.get(name).rt;
    }

    /**
     * Route table into which routes can be placed.
     */
    private class EvpnInstanceRouteTable {

        private final EvpnInstanceName evpnName;
        private final RouteDistinguisher rd;
        private final RouteTarget rt;
        private final Map<EvpnInstancePrefix, EvpnInstanceNextHop> routesMap = new ConcurrentHashMap<>();

        // private final Multimap<EvpnInstanceNextHop, EvpnInstanceRoute>
        // reverseIndex = Multimaps
        // .synchronizedMultimap(HashMultimap.create());

        public EvpnInstanceRouteTable(EvpnInstanceName evpnName,
                                      RouteDistinguisher rd, RouteTarget rt) {
            this.evpnName = evpnName;
            this.rd = rd;
            this.rt = rt;
        }

        /**
         * Adds or updates the route in the route table.
         *
         * @param route route to update
         */
        public void update(EvpnInstanceRoute route) {
            synchronized (this) {
                if (route.evpnInstanceName().equals(evpnName)) {
                    EvpnInstanceNextHop oldNextHop = routesMap
                            .put(route.prefix(), route.nextHop());

                    notifyDelegate(new EvpnInstanceRouteEvent(EvpnInstanceRouteEvent.Type.ROUTE_ADDED,
                                                              route));
                }
                // reverseIndex.put(route.nextHop(), route);

                // if (oldRoute != null) {
                // reverseIndex.remove(oldRoute.nextHop(), oldRoute);
                // }

            }
        }

        /**
         * Removes the route from the route table.
         *
         * @param route route to remove
         */
        public void remove(EvpnInstanceRoute route) {
            synchronized (this) {
                if (route.evpnInstanceName().equals(evpnName)) {
                    routesMap.remove(route.prefix());
                    // reverseIndex.remove(removedRoute.nextHop(),
                    // removedRoute);
                    notifyDelegate(new EvpnInstanceRouteEvent(EvpnInstanceRouteEvent.Type.ROUTE_REMOVED,
                                                              route));
                }
            }
        }

        public Collection<EvpnInstanceRoute> getRoutes() {
            List<EvpnInstanceRoute> routes = new LinkedList<>();
            for (Map.Entry<EvpnInstancePrefix, EvpnInstanceNextHop> e : routesMap
                    .entrySet()) {
                EvpnInstanceRoute route = new EvpnInstanceRoute(evpnName, rd,
                                                                rt, e.getKey(),
                                                                e.getValue());
                routes.add(route);
            }
            return routes;
        }

    }

    @Override
    public RouteDistinguisher getRdByInstanceName(EvpnInstanceName name) {
        if (routeTables == null) {
            return null;
        }
        return routeTables.get(name).rd;
    }

}
