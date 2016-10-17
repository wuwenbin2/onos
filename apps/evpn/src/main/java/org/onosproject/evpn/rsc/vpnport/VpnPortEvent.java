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

package org.onosproject.evpn.rsc.vpnport;

import org.onosproject.event.AbstractEvent;
import org.onosproject.evpn.rsc.VpnPort;

/**
 * Describes network Vpn Port event.
 */
public class VpnPortEvent extends AbstractEvent<VpnPortEvent.Type, VpnPort> {

    /**
     * Type of Vpn Port events.
     */
    public enum Type {
        /**
         * Signifies that Vpn Port has been set.
         */
        VPNPORT_SET,
        /**
         * Signifies that Vpn Port has been deleted.
         */
        VPNPORT_DELETE,
        /**
         * Signifies that Vpn Port has been updated.
         */
        VPNPORT_UPDATE
    }

    /**
     * Creates an event of a given type and for the specified Vpn Port.
     *
     * @param type Vpn Port type
     * @param vpnPort Vpn Port subject
     */
    public VpnPortEvent(Type type, VpnPort vpnPort) {
        super(type, vpnPort);
    }

    /**
     * Creates an event of a given type and for the specified Vpn Port.
     *
     * @param type Vpn Port type
     * @param vpnPort Vpn Port subject
     * @param time occurrence time
     */
    public VpnPortEvent(Type type, VpnPort vpnPort, long time) {
        super(type, vpnPort, time);
    }
}
