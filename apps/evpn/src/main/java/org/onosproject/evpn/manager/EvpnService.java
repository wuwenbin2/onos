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
package org.onosproject.evpn.manager;

import org.onosproject.incubator.net.routing.EvpnRoute;
import org.onosproject.net.Host;

public interface EvpnService {
    /**
     * Transfer remote route to private route and set mpls flows out when
     * BgpRoute update.
     *
     * @param route evpn route
     */
    void onBgpEvpnRouteUpdate(EvpnRoute route);

    /**
     * Transfer remote route to private route and delete mpls flows out when
     * BgpRoute delete.
     *
     * @param route
     */
    void onBgpEvpnRouteDelete(EvpnRoute route);

    /**
     * Get vpn info from Gluon shim and create route, set flows when host
     * detected.
     *
     * @param host
     */
    void onHostDetected(Host host);

    /**
     * Get vpn info from Gluon shim and delete route, set flows when host
     * vanished.
     *
     * @param host
     */
    void onHostVanished(Host host);
}
