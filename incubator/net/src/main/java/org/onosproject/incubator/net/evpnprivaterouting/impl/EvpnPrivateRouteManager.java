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

package org.onosproject.incubator.net.evpnprivaterouting.impl;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import javax.annotation.concurrent.GuardedBy;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.event.ListenerService;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnPrivateRoute;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnPrivateRouteAdminService;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnPrivateRouteEvent;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnPrivateRouteListener;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnPrivateRouteService;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnPrivateRouteStore;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnPrivateRouteStoreDelegate;
import org.onosproject.net.host.HostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the unicast route service.
 */
@Service
@Component
public class EvpnPrivateRouteManager
        implements
        ListenerService<EvpnPrivateRouteEvent, EvpnPrivateRouteListener>,
        EvpnPrivateRouteService, EvpnPrivateRouteAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private EvpnPrivateRouteStoreDelegate delegate = new InternalRouteStoreDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EvpnPrivateRouteStore routeStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @GuardedBy(value = "this")
    private Map<EvpnPrivateRouteListener, ListenerQueue> listeners = new HashMap<>();

    private ThreadFactory threadFactory;

    @Activate
    protected void activate() {
        threadFactory = groupedThreads("onos/evpnroute", "listener-%d", log);

        routeStore.setDelegate(delegate);

    }

    @Deactivate
    protected void deactivate() {
        listeners.values().forEach(l -> l.stop());

        routeStore.unsetDelegate(delegate);
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
    public void addListener(EvpnPrivateRouteListener listener) {
        synchronized (this) {
            log.debug("Synchronizing current routes to new listener");
            ListenerQueue l = createListenerQueue(listener);
            Collection<EvpnPrivateRoute> routes = routeStore.getEvpnRoutes();
            if (routes != null) {
                routes.forEach(route -> l
                        .post(new EvpnPrivateRouteEvent(EvpnPrivateRouteEvent.Type.ROUTE_UPDATED,
                                                        route)));
            }

            listeners.put(listener, l);

            l.start();
            log.debug("Route synchronization complete");
        }
    }

    @Override
    public void removeListener(EvpnPrivateRouteListener listener) {
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
    private void post(EvpnPrivateRouteEvent event) {
        log.debug("Sending event {}", event);
        synchronized (this) {
            listeners.values().forEach(l -> l.post(event));
        }
    }

    @Override
    public Collection<EvpnPrivateRoute> getAllRoutes() {
        return routeStore.getEvpnRoutes();
    }

    @Override
    public void updateEvpnRoute(Collection<EvpnPrivateRoute> routes) {
        synchronized (this) {
            routes.forEach(route -> {
                routeStore.updateEvpnRoute(route);
            });
        }
    }

    @Override
    public void withdrawEvpnRoute(Collection<EvpnPrivateRoute> routes) {
        synchronized (this) {
            routes.forEach(route -> {
                log.debug("Received withdraw {}", routes);
                routeStore.removeEvpnRoute(route);
            });
        }
    }

    /**
     * Creates a new listener queue.
     *
     * @param listener route listener
     * @return listener queue
     */
    ListenerQueue createListenerQueue(EvpnPrivateRouteListener listener) {
        return new DefaultListenerQueue(listener);
    }

    /**
     * Default route listener queue.
     */
    private class DefaultListenerQueue implements ListenerQueue {

        private final ExecutorService executorService;
        private final BlockingQueue<EvpnPrivateRouteEvent> queue;
        private final EvpnPrivateRouteListener listener;

        /**
         * Creates a new listener queue.
         *
         * @param listener route listener to queue updates for
         */
        public DefaultListenerQueue(EvpnPrivateRouteListener listener) {
            this.listener = listener;
            queue = new LinkedBlockingQueue<>();
            executorService = newSingleThreadExecutor(threadFactory);
        }

        @Override
        public void post(EvpnPrivateRouteEvent event) {
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
    private class InternalRouteStoreDelegate
            implements EvpnPrivateRouteStoreDelegate {
        @Override
        public void notify(EvpnPrivateRouteEvent event) {
            post(event);
        }
    }
}
