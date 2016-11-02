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

package org.onosproject.incubator.store.routing.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.incubator.net.routing.EvpnInstanceName;
import org.onosproject.incubator.net.routing.EvpnInstanceNextHop;
import org.onosproject.incubator.net.routing.EvpnInstancePrefix;
import org.onosproject.incubator.net.routing.EvpnInstanceRoute;
import org.onosproject.incubator.net.routing.EvpnNextHop;
import org.onosproject.incubator.net.routing.EvpnPrefix;
import org.onosproject.incubator.net.routing.EvpnRoute;
import org.onosproject.incubator.net.routing.IpNextHop;
import org.onosproject.incubator.net.routing.IpRoute;
import org.onosproject.incubator.net.routing.NextHop;
import org.onosproject.incubator.net.routing.ResolvedRoute;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteDistinguisher;
import org.onosproject.incubator.net.routing.RouteEvent;
import org.onosproject.incubator.net.routing.RouteStore;
import org.onosproject.incubator.net.routing.RouteStoreDelegate;
import org.onosproject.incubator.net.routing.RouteTable;
import org.onosproject.incubator.net.routing.RouteTableType;
import org.onosproject.incubator.net.routing.RouteTarget;
import org.onosproject.store.AbstractStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.googlecode.concurrenttrees.common.KeyValuePair;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;

/**
 * Route store based on in-memory storage.
 */
@Service
@Component
public class LocalRouteStore
        extends AbstractStore<RouteEvent, RouteStoreDelegate>
        implements RouteStore {

    private Logger log = LoggerFactory.getLogger(getClass());

    private Map<RouteTableType, RouteTable> routeTables;

    private Map<IpAddress, MacAddress> nextHops = new ConcurrentHashMap<>();

    @Activate
    public void activate() {
        routeTables = new ConcurrentHashMap<>();
        routeTables.put(RouteTableType.IPV4, new IpRouteTable());
        routeTables.put(RouteTableType.IPV6, new IpRouteTable());
        routeTables.put(RouteTableType.VPN_PUBLIC, new EvpnRouteTable());
        routeTables.put(RouteTableType.VPN_PRIVATE,
                        new EvpnInstanceRouteTables());
    }

    @Override
    public void updateRoute(Route route) {
        getDefaultRouteTable(route).update(route);
    }

    @Override
    public void removeRoute(Route route) {
        RouteTable table = getDefaultRouteTable(route);
        table.remove(route);
        Collection<Route> routes = table.getRoutesForNextHop(route.nextHop());

        if (routes.isEmpty()) {
            nextHops.remove(route.nextHop());
        }
    }

    @Override
    public Set<RouteTableType> getRouteTables() {
        return routeTables.keySet();
    }

    @Override
    public Collection<Route> getRoutes(RouteTableType table) {
        RouteTable routeTable = routeTables.get(table);
        if (routeTable == null) {
            return Collections.emptySet();
        }
        return routeTable.getRoutes();
    }

    private RouteTable getDefaultRouteTable(Route route) {
        RouteTableType tableType = null;
        if (route instanceof IpRoute) {
            IpAddress ip = ((IpRoute) route).prefix().address();
            tableType = (ip.isIp4()) ? RouteTableType.IPV4
                                     : RouteTableType.IPV6;
        } else if (route instanceof EvpnRoute) {
            tableType = RouteTableType.VPN_PUBLIC;

        } else if (route instanceof EvpnInstanceRoute) {
            tableType = RouteTableType.VPN_PRIVATE;
        }

        return routeTables.get(tableType);

    }

    @Override
    public Collection<Route> getRoutesForNextHop(RouteTableType table,
                                                 NextHop nextHop) {
        List<Route> routes = new LinkedList<>();
        routes.addAll(routeTables.get(table).getRoutesForNextHop(nextHop));
        return routes;
    }

    @Override
    public void updateNextHop(IpAddress ip, MacAddress mac) {
        Collection<Route> routes = getDefaultRouteTable(ip)
                .getRoutesForNextHop(IpNextHop.ipAddress(ip));

        if (!routes.isEmpty() && !mac.equals(nextHops.get(ip))) {
            MacAddress oldMac = nextHops.put(ip, mac);

            for (Route route : routes) {
                if (route instanceof IpRoute) {
                    IpRoute ipRoute = (IpRoute) route;
                    if (oldMac == null) {
                        notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_ADDED,
                                                      new ResolvedRoute(ipRoute,
                                                                        mac)));
                    } else {
                        notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_UPDATED,
                                                      new ResolvedRoute(ipRoute,
                                                                        mac)));
                    }
                }
            }
        }
    }

    @Override
    public void removeNextHop(IpAddress ip, MacAddress mac) {
        if (nextHops.remove(ip, mac)) {
            Collection<Route> routes = getDefaultRouteTable(ip)
                    .getRoutesForNextHop(IpNextHop.ipAddress(ip));
            for (Route route : routes) {
                if (route instanceof IpRoute) {
                    IpRoute ipRoute = (IpRoute) route;
                    notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED,
                                                  new ResolvedRoute(ipRoute,
                                                                    null)));
                }
            }
        }
    }

    @Override
    public MacAddress getNextHop(IpAddress ip) {
        return nextHops.get(ip);
    }

    @Override
    public IpRoute longestPrefixMatch(IpAddress ip) {
        return getDefaultRouteTable(ip).longestPrefixMatch(ip);
    }

    private IpRouteTable getDefaultRouteTable(IpAddress ip) {
        RouteTableType routeTableId = (ip.isIp4()) ? RouteTableType.IPV4
                                                   : RouteTableType.IPV6;
        return (IpRouteTable) routeTables.get(routeTableId);
    }

    @Override
    public Map<IpAddress, MacAddress> getNextHops() {
        return ImmutableMap.copyOf(nextHops);
    }

    @Override
    public Collection<EvpnInstanceRoute> getEvpnRoutes(EvpnInstanceName evpnName) {
        if (routeTables == null
                || routeTables.get(RouteTableType.VPN_PRIVATE) == null) {
            return Collections.emptySet();
        }

        EvpnInstanceRouteTables routeTable = (EvpnInstanceRouteTables) routeTables
                .get(RouteTableType.VPN_PRIVATE);
        return routeTable.getEvpnRoutes(evpnName);
    }

    @Override
    public Map<EvpnInstancePrefix, EvpnInstanceNextHop> getRouteMapByInstanceName(EvpnInstanceName name) {
        if (routeTables == null
                || routeTables.get(RouteTableType.VPN_PRIVATE) == null) {
            return null;
        }

        EvpnInstanceRouteTables routeTable = (EvpnInstanceRouteTables) routeTables
                .get(RouteTableType.VPN_PRIVATE);
        return routeTable.getRouteMapByInstanceName(name);
    }

    @Override
    public RouteTarget getRtByInstanceName(EvpnInstanceName name) {
        if (routeTables == null
                || routeTables.get(RouteTableType.VPN_PRIVATE) == null) {
            return null;
        }
        EvpnInstanceRouteTables routeTable = (EvpnInstanceRouteTables) routeTables
                .get(RouteTableType.VPN_PRIVATE);
        return routeTable.getRtByInstanceName(name);
    }

    @Override
    public RouteDistinguisher getRdByInstanceName(EvpnInstanceName name) {
        if (routeTables == null
                || routeTables.get(RouteTableType.VPN_PRIVATE) == null) {
            return null;
        }
        EvpnInstanceRouteTables routeTable = (EvpnInstanceRouteTables) routeTables
                .get(RouteTableType.VPN_PRIVATE);
        return routeTable.getRdByInstanceName(name);
    }

    private class IpRouteTable implements RouteTable {

        private final InvertedRadixTree<IpRoute> routeTable;

        private final Map<IpPrefix, IpRoute> routes = new ConcurrentHashMap<>();
        private final Multimap<IpAddress, IpRoute> reverseIndex = Multimaps
                .synchronizedMultimap(HashMultimap.create());

        /**
         * Creates a new route table.
         */
        public IpRouteTable() {
            routeTable = new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());
        }

        @Override
        public void update(Route route) {
            synchronized (this) {
                IpRoute ipRoute = (IpRoute) route;
                IpRoute oldRoute = routes.put(ipRoute.prefix(), ipRoute);
                routeTable.put(createBinaryString(ipRoute.prefix()), ipRoute);
                reverseIndex.put(ipRoute.ipNextHop(), ipRoute);

                if (oldRoute != null) {
                    reverseIndex.remove(oldRoute.ipNextHop(), oldRoute);

                    if (reverseIndex.get(oldRoute.ipNextHop()).isEmpty()) {
                        nextHops.remove(oldRoute.ipNextHop());
                    }
                }

                if (ipRoute.equals(oldRoute)) {
                    // No need to send events if the new route is the same
                    return;
                }

                MacAddress nextHopMac = nextHops.get(ipRoute.ipNextHop());

                if (oldRoute != null
                        && !oldRoute.ipNextHop().equals(ipRoute.ipNextHop())) {
                    if (nextHopMac == null) {
                        // We don't know the new MAC address yet so delete the
                        // route
                        notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED,
                                                      new ResolvedRoute(oldRoute,
                                                                        null)));
                    } else {
                        // We know the new MAC address so update the route
                        notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_UPDATED,
                                                      new ResolvedRoute(ipRoute,
                                                                        nextHopMac)));
                    }
                    return;
                }

                if (nextHopMac != null) {
                    notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_ADDED,
                                                  new ResolvedRoute(ipRoute,
                                                                    nextHopMac)));
                }
            }
        }

        @Override
        public void remove(Route route) {
            synchronized (this) {
                IpRoute ipRoute = (IpRoute) route;
                IpRoute removed = routes.remove(ipRoute.prefix());
                routeTable.remove(createBinaryString(ipRoute.prefix()));

                if (removed != null) {
                    reverseIndex.remove(removed.nextHop(), removed);
                    notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED,
                                                  new ResolvedRoute(ipRoute,
                                                                    null)));
                }
            }
        }

        @Override
        public Collection<Route> getRoutesForNextHop(NextHop nextHop) {
            List<Route> routes = new LinkedList<Route>();
            IpNextHop ipNextHop = (IpNextHop) nextHop;
            routes.addAll(reverseIndex.get(ipNextHop.getIpAddress()));
            return routes;
        }

        @Override
        public Collection<Route> getRoutes() {
            Iterator<KeyValuePair<IpRoute>> it = routeTable
                    .getKeyValuePairsForKeysStartingWith("").iterator();

            List<Route> routes = new LinkedList<Route>();

            while (it.hasNext()) {
                KeyValuePair<IpRoute> entry = it.next();
                routes.add(entry.getValue());
            }

            return routes;
        }

        /**
         * Performs a longest prefix match with the given IP in the route table.
         *
         * @param ip IP address to look up
         * @return most specific prefix containing the given
         */
        public IpRoute longestPrefixMatch(IpAddress ip) {
            Iterable<IpRoute> prefixes = routeTable
                    .getValuesForKeysPrefixing(createBinaryString(ip
                            .toIpPrefix()));

            Iterator<IpRoute> it = prefixes.iterator();

            IpRoute route = null;
            while (it.hasNext()) {
                route = it.next();
            }

            return route;
        }

        private String createBinaryString(IpPrefix ipPrefix) {
            byte[] octets = ipPrefix.address().toOctets();
            StringBuilder result = new StringBuilder(ipPrefix.prefixLength());
            result.append("0");
            for (int i = 0; i < ipPrefix.prefixLength(); i++) {
                int byteOffset = i / Byte.SIZE;
                int bitOffset = i % Byte.SIZE;
                int mask = 1 << (Byte.SIZE - 1 - bitOffset);
                byte value = octets[byteOffset];
                boolean isSet = ((value & mask) != 0);
                result.append(isSet ? "1" : "0");
            }

            return result.toString();
        }

    }

    private class EvpnRouteTable implements RouteTable {
        private final Map<EvpnPrefix, EvpnRoute> routesMap = new ConcurrentHashMap<>();
        private final Multimap<EvpnNextHop, EvpnRoute> reverseIndex = Multimaps
                .synchronizedMultimap(HashMultimap.create());

        @Override
        public void update(Route route) {
            synchronized (this) {
                if (route instanceof EvpnRoute) {
                    EvpnRoute evpnRoute = (EvpnRoute) route;
                    EvpnPrefix prefix = EvpnPrefix
                            .evpnPrefix(evpnRoute.routeDistinguisher(),
                                        evpnRoute.prefixMac(),
                                        evpnRoute.prefixIp());
                    EvpnNextHop nextHop = EvpnNextHop
                            .evpnNextHop(evpnRoute.ipNextHop(),
                                         evpnRoute.routeTarget(),
                                         evpnRoute.label());
                    EvpnRoute oldRoute = routesMap.put(prefix, evpnRoute);
                    reverseIndex.put(nextHop, evpnRoute);

                    if (oldRoute != null) {
                        EvpnNextHop odlNextHop = EvpnNextHop
                                .evpnNextHop(oldRoute.ipNextHop(),
                                             oldRoute.routeTarget(),
                                             oldRoute.label());
                        reverseIndex.remove(odlNextHop, oldRoute);
                    }

                    if (evpnRoute.equals(oldRoute)) {
                        // No need to send events if the new route is the same
                        return;
                    }

                    if (oldRoute != null) {
                        notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED,
                                                      oldRoute));
                        log.info("Notify route remove event {}", evpnRoute);
                        // notifyDelegate(new
                        // EvpnRouteEvent(EvpnRouteEvent.Type.ROUTE_UPDATED,
                        // route));
                    }
                    notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_ADDED,
                                                  evpnRoute));
                    log.info("Notify route add event {}", evpnRoute);
                    return;
                }
            }
        }

        @Override
        public void remove(Route route) {
            synchronized (this) {
                if (route instanceof EvpnRoute) {
                    EvpnRoute evpnRoute = (EvpnRoute) route;
                    EvpnPrefix prefix = EvpnPrefix
                            .evpnPrefix(evpnRoute.routeDistinguisher(),
                                        evpnRoute.prefixMac(),
                                        evpnRoute.prefixIp());
                    EvpnRoute removedRoute = routesMap.remove(prefix);

                    if (removedRoute != null) {
                        reverseIndex.remove(removedRoute.nextHop(),
                                            removedRoute);
                        notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED,
                                                      removedRoute));
                    }
                }
            }
        }

        @Override
        public Collection<Route> getRoutes() {
            List<Route> routes = new LinkedList<Route>();
            for (Map.Entry<EvpnPrefix, EvpnRoute> e : routesMap.entrySet()) {
                routes.add(e.getValue());
            }
            return routes;
        }

        @Override
        public Collection<Route> getRoutesForNextHop(NextHop nextHop) {
            List<Route> routes = new LinkedList<Route>();
            EvpnNextHop evpnNextHop = (EvpnNextHop) nextHop;
            routes.addAll(reverseIndex.get(evpnNextHop));
            return routes;
        }

    }

    private class EvpnInstanceRouteTables implements RouteTable {
        private Map<EvpnInstanceName, EvpnInstanceRouteTable> privateRouteTables = new HashMap<>();

        @Override
        public void update(Route route) {
            EvpnInstanceRoute evpnInstanceRoute = (EvpnInstanceRoute) route;
            if (privateRouteTables
                    .get(evpnInstanceRoute.evpnInstanceName()) == null) {
                EvpnInstanceRouteTable evpnInstanceRouteTable = new EvpnInstanceRouteTable(evpnInstanceRoute
                        .evpnInstanceName(), evpnInstanceRoute
                                .routeDistinguisher(), evpnInstanceRoute
                                        .routeTarget());
                privateRouteTables.put(evpnInstanceRoute.evpnInstanceName(),
                                       evpnInstanceRouteTable);
            }
            privateRouteTables.get(evpnInstanceRoute.evpnInstanceName())
                    .update(evpnInstanceRoute);
        }

        @Override
        public void remove(Route route) {
            EvpnInstanceRoute evpnInstanceRoute = (EvpnInstanceRoute) route;
            if (privateRouteTables
                    .get(evpnInstanceRoute.evpnInstanceName()) == null) {
                return;
            }
            privateRouteTables.get(evpnInstanceRoute.evpnInstanceName())
                    .remove(evpnInstanceRoute);
        }

        public Collection<EvpnInstanceRoute> getEvpnRoutes(EvpnInstanceName evpnName) {
            if (privateRouteTables == null) {
                return Collections.emptySet();
            }
            return privateRouteTables.get(evpnName).getRoutes();
        }

        @Override
        public Collection<Route> getRoutes() {
            if (privateRouteTables == null) {
                return Collections.emptySet();
            }
            Collection<Route> list = Lists.newLinkedList();
            privateRouteTables.keySet().forEach(evpnName -> {
                list.addAll(privateRouteTables.get(evpnName).getRoutes());
            });
            return list;
        }

        @Override
        public Collection<Route> getRoutesForNextHop(NextHop nextHop) {
            if (privateRouteTables == null) {
                return Collections.emptySet();
            }
            Collection<Route> list = Lists.newLinkedList();
            privateRouteTables.keySet().forEach(evpnName -> {
                list.addAll(privateRouteTables.get(evpnName)
                        .getRoutesForNextHop(nextHop));
            });
            return list;
        }

        public Map<EvpnInstancePrefix, EvpnInstanceNextHop> getRouteMapByInstanceName(EvpnInstanceName name) {
            if (privateRouteTables == null) {
                return null;
            }
            return privateRouteTables.get(name).getRouteMap();
        }

        public RouteTarget getRtByInstanceName(EvpnInstanceName name) {
            if (privateRouteTables == null) {
                return null;
            }
            return privateRouteTables.get(name).getRt();
        }

        public RouteDistinguisher getRdByInstanceName(EvpnInstanceName name) {
            if (privateRouteTables == null) {
                return null;
            }
            return privateRouteTables.get(name).getRd();
        }

        private class EvpnInstanceRouteTable {

            private final EvpnInstanceName evpnName;
            private final RouteDistinguisher rd;
            private final RouteTarget rt;
            private final Map<EvpnInstancePrefix, EvpnInstanceNextHop> routesMap = new ConcurrentHashMap<>();
            private final Multimap<EvpnInstanceNextHop, EvpnInstanceRoute> reverseIndex = Multimaps
                    .synchronizedMultimap(HashMultimap.create());

            public EvpnInstanceRouteTable(EvpnInstanceName evpnName,
                                          RouteDistinguisher rd,
                                          RouteTarget rt) {
                this.evpnName = evpnName;
                this.rd = rd;
                this.rt = rt;
            }

            public void update(Route route) {
                synchronized (this) {
                    if (route instanceof EvpnInstanceRoute) {
                        EvpnInstanceRoute evpnInstanceRoute = (EvpnInstanceRoute) route;
                        if (evpnInstanceRoute.evpnInstanceName()
                                .equals(evpnName)) {
                            EvpnInstanceNextHop oldNextHop = routesMap
                                    .put(evpnInstanceRoute.prefix(),
                                         evpnInstanceRoute.nextHop());

                            notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_ADDED,
                                                          evpnInstanceRoute));
                        }
                    }

                }
            }

            public void remove(Route route) {
                synchronized (this) {
                    if (route instanceof EvpnInstanceRoute) {
                        EvpnInstanceRoute evpnInstanceRoute = (EvpnInstanceRoute) route;
                        if (evpnInstanceRoute.evpnInstanceName()
                                .equals(evpnName)) {
                            routesMap.remove(evpnInstanceRoute.prefix());
                            // reverseIndex.remove(removedRoute.nextHop(),
                            // removedRoute);
                            notifyDelegate(new RouteEvent(RouteEvent.Type.ROUTE_REMOVED,
                                                          evpnInstanceRoute));
                        }
                    }
                }
            }

            public Collection<EvpnInstanceRoute> getRoutes() {
                List<EvpnInstanceRoute> routes = new LinkedList<EvpnInstanceRoute>();
                for (Map.Entry<EvpnInstancePrefix, EvpnInstanceNextHop> e : routesMap
                        .entrySet()) {
                    EvpnInstanceRoute route = new EvpnInstanceRoute(evpnName,
                                                                    rd, rt,
                                                                    e.getKey(),
                                                                    e.getValue());
                    routes.add(route);
                }
                return routes;
            }

            public Collection<Route> getRoutesForNextHop(NextHop nextHop) {
                List<Route> routes = new LinkedList<Route>();
                EvpnInstanceNextHop evpnInstancenextHop = (EvpnInstanceNextHop) nextHop;
                routes.addAll(reverseIndex.get(evpnInstancenextHop));
                return routes;
            }

            public RouteTarget getRt() {
                return rt;
            }

            public RouteDistinguisher getRd() {
                return rd;
            }

            public Map<EvpnInstancePrefix, EvpnInstanceNextHop> getRouteMap() {
                return routesMap;
            }
        }
    }
}
