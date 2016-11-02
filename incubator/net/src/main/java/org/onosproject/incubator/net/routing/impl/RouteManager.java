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

package org.onosproject.incubator.net.routing.impl;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.concurrent.GuardedBy;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.event.ListenerService;
import org.onosproject.incubator.net.routing.EvpnInstanceName;
import org.onosproject.incubator.net.routing.EvpnInstanceNextHop;
import org.onosproject.incubator.net.routing.EvpnInstancePrefix;
import org.onosproject.incubator.net.routing.EvpnInstanceRoute;
import org.onosproject.incubator.net.routing.EvpnRoute;
import org.onosproject.incubator.net.routing.IpRoute;
import org.onosproject.incubator.net.routing.NextHop;
import org.onosproject.incubator.net.routing.ResolvedNextHop;
import org.onosproject.incubator.net.routing.ResolvedRoute;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteAdminService;
import org.onosproject.incubator.net.routing.RouteDistinguisher;
import org.onosproject.incubator.net.routing.RouteEvent;
import org.onosproject.incubator.net.routing.RouteListener;
import org.onosproject.incubator.net.routing.RouteService;
import org.onosproject.incubator.net.routing.RouteStore;
import org.onosproject.incubator.net.routing.RouteStoreDelegate;
import org.onosproject.incubator.net.routing.RouteTableType;
import org.onosproject.incubator.net.routing.RouteTarget;
import org.onosproject.incubator.net.routing.EvpnRoute.OperationType;
import org.onosproject.incubator.provider.BgpEvpnRouteProvider;
import org.onosproject.incubator.provider.BgpEvpnRouteProviderRegistry;
import org.onosproject.incubator.provider.BgpEvpnRouteProviderService;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.provider.AbstractListenerProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the unicast route service.
 */
@Service
@Component
public class RouteManager
        extends
        AbstractListenerProviderRegistry<RouteEvent, RouteListener, BgpEvpnRouteProvider, BgpEvpnRouteProviderService>
        implements BgpEvpnRouteProviderRegistry,
        ListenerService<RouteEvent, RouteListener>, RouteService,
        RouteAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private RouteStoreDelegate delegate = new InternalRouteStoreDelegate();
    private InternalHostListener hostListener = new InternalHostListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouteStore routeStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @GuardedBy(value = "this")
    private Map<RouteListener, ListenerQueue> listeners = new HashMap<>();

    private ThreadFactory threadFactory;

    @Activate
    protected void activate() {
        threadFactory = groupedThreads("onos/route", "listener-%d", log);

        routeStore.setDelegate(delegate);
        hostService.addListener(hostListener);

    }

    @Deactivate
    protected void deactivate() {
        listeners.values().forEach(l -> l.stop());

        routeStore.unsetDelegate(delegate);
        hostService.removeListener(hostListener);
    }

    /**
     * {@inheritDoc}
     *
     * In a departure from other services in ONOS, calling addListener will
     * cause all current routes to be pushed to the listener before any new
     * events are sent. This allows a listener to easily get the exact set of
     * routes without worrying about missing any.
     *
     * @param listener listener to be added
     */
    @Override
    public void addListener(RouteListener listener) {
        synchronized (this) {
            log.debug("Synchronizing current routes to new listener");
            ListenerQueue l = createListenerQueue(listener);
            routeStore.getRouteTables().forEach(table -> {
                Collection<Route> routes = routeStore.getRoutes(table);
                if (routes != null) {
                    routes.forEach(route -> {
                        if (route instanceof IpRoute) {
                            IpRoute ipRoute = (IpRoute) route;
                            l.post(new RouteEvent(RouteEvent.Type.ROUTE_UPDATED,
                                                  new ResolvedRoute(ipRoute,
                                                                    routeStore
                                                                            .getNextHop(ipRoute
                                                                                    .ipNextHop()))));

                        } else if (route instanceof EvpnRoute
                                | route instanceof EvpnInstanceRoute) {
                            l.post(new RouteEvent(RouteEvent.Type.ROUTE_UPDATED,
                                                  route));
                        }
                    });

                    listeners.put(listener, l);

                    l.start();
                    log.debug("Route synchronization complete");
                }
            });
        }
    }

    @Override
    public void removeListener(RouteListener listener) {
        synchronized (this) {
            ListenerQueue l = listeners.remove(listener);
            if (l != null) {
                l.stop();
            }
        }
    }

    /**
     * Posts an event to all listeners.
     *
     * @param event event
     */
    protected void post(RouteEvent event) {
        log.debug("Sending event {}", event);
        synchronized (this) {
            listeners.values().forEach(l -> l.post(event));
        }
    }

    @Override
    public Map<RouteTableType, Collection<Route>> getAllRoutes() {
        return routeStore.getRouteTables().stream()
                .collect(Collectors
                        .toMap(Function.identity(),
                               table -> (table == null) ? Collections.emptySet()
                                                        : routeStore
                                                                .getRoutes(table)));
    }

    @Override
    public Map<EvpnInstancePrefix, EvpnInstanceNextHop> getRouteMapByInstanceName(EvpnInstanceName name) {
        return routeStore.getRouteMapByInstanceName(name);
    }

    @Override
    public RouteTarget getRtByInstanceName(EvpnInstanceName name) {
        return routeStore.getRtByInstanceName(name);
    }

    public IpRoute longestPrefixMatch(IpAddress ip) {
        return routeStore.longestPrefixMatch(ip);
    }

    @Override
    public Collection<Route> getRoutesForNextHop(RouteTableType id,
                                                 NextHop nextHop) {
        return routeStore.getRoutesForNextHop(id, nextHop);
    }

    @Override
    public Set<NextHop> getNextHops() {
        return routeStore.getNextHops().entrySet().stream()
                .map(entry -> new ResolvedNextHop(entry.getKey(),
                                                  entry.getValue()))
                .collect(Collectors.toSet());
    }

    @Override
    public void update(Collection<Route> routes) {
        synchronized (this) {
            routes.forEach(route -> {
                log.debug("Received update {}", route);
                routeStore.updateRoute(route);
                if (route instanceof IpRoute) {
                    resolve((IpRoute) route);
                }
            });
        }
    }

    @Override
    public void withdraw(Collection<Route> routes) {
        synchronized (this) {
            routes.forEach(route -> {
                log.debug("Received withdraw {}", routes);
                routeStore.removeRoute(route);
            });
        }
    }

    private void resolve(IpRoute route) {
        // Monitor the IP address for updates of the MAC address
        hostService.startMonitoringIp(route.ipNextHop());

        MacAddress nextHopMac = routeStore.getNextHop(route.ipNextHop());
        if (nextHopMac == null) {
            Set<Host> hosts = hostService.getHostsByIp(route.ipNextHop());
            Optional<Host> host = hosts.stream().findFirst();
            if (host.isPresent()) {
                nextHopMac = host.get().mac();
            }
        }

        if (nextHopMac != null) {
            routeStore.updateNextHop(route.ipNextHop(), nextHopMac);
        }
    }

    private void hostUpdated(Host host) {
        synchronized (this) {
            for (IpAddress ip : host.ipAddresses()) {
                routeStore.updateNextHop(ip, host.mac());
            }
        }
    }

    private void hostRemoved(Host host) {
        synchronized (this) {
            for (IpAddress ip : host.ipAddresses()) {
                routeStore.removeNextHop(ip, host.mac());
            }
        }
    }

    /**
     * Creates a new listener queue.
     *
     * @param listener route listener
     * @return listener queue
     */
    ListenerQueue createListenerQueue(RouteListener listener) {
        return new DefaultListenerQueue(listener);
    }

    /**
     * Default route listener queue.
     */
    private class DefaultListenerQueue implements ListenerQueue {

        private final ExecutorService executorService;
        private final BlockingQueue<RouteEvent> queue;
        private final RouteListener listener;

        /**
         * Creates a new listener queue.
         *
         * @param listener route listener to queue updates for
         */
        public DefaultListenerQueue(RouteListener listener) {
            this.listener = listener;
            queue = new LinkedBlockingQueue<>();
            executorService = newSingleThreadExecutor(threadFactory);
        }

        @Override
        public void post(RouteEvent event) {
            queue.add(event);
        }

        @Override
        public void start() {
            executorService.execute(this::poll);
        }

        @Override
        public void stop() {
            executorService.shutdown();
        }

        private void poll() {
            while (true) {
                try {
                    listener.event(queue.take());
                } catch (InterruptedException e) {
                    log.info("Route listener event thread shutting down: {}",
                             e.getMessage());
                    break;
                } catch (Exception e) {
                    log.warn("Exception during route event handler", e);
                }
            }
        }

    }

    /**
     * Delegate to receive events from the route store.
     */
    private class InternalRouteStoreDelegate implements RouteStoreDelegate {
        @Override
        public void notify(RouteEvent event) {
            post(event);
        }
    }

    /**
     * Internal listener for host events.
     */
    private class InternalHostListener implements HostListener {
        @Override
        public void event(HostEvent event) {
            switch (event.type()) {
            case HOST_ADDED:
            case HOST_UPDATED:
                hostUpdated(event.subject());
                break;
            case HOST_REMOVED:
                hostRemoved(event.subject());
                break;
            case HOST_MOVED:
                break;
            default:
                break;
            }
        }
    }

    @Override
    public void sendEvpnMessage(OperationType type, EvpnRoute evpnRoute) {
        if (evpnRoute == null) {
            return;
        }
        String scheme = "route";
        BgpEvpnRouteProvider provider = (BgpEvpnRouteProvider) getProvider(scheme);
        if (provider != null) {
            provider.sendEvpnRoute(type, evpnRoute);
            return;
        } else {
            log.error("Provider not found for {}", scheme);
            return;
        }
    }

    @Override
    protected BgpEvpnRouteProviderService createProviderService(BgpEvpnRouteProvider provider) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RouteDistinguisher getRdByInstanceName(EvpnInstanceName name) {
        return routeStore.getRdByInstanceName(name);
    }

}
