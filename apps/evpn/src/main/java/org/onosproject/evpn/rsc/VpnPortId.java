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

package org.onosproject.evpn.rsc;

import org.onlab.util.Identifier;

/**
 * Immutable representation of a VPN port identity.
 */
public final class VpnPortId extends Identifier<String> {

    private VpnPortId(String vpnPortId) {
        super(vpnPortId);
    }

    /**
     * Creates a VPN port identifier.
     *
     * @param vpnPortId VPN port identify string
     * @return VPN port identifier
     */
    public static VpnPortId vpnPortId(String vpnPortId) {
        return new VpnPortId(vpnPortId);
    }

    /**
     * Returns VPN port identifier.
     *
     * @return the VPN port identifier
     */
    public String vpnPortId() {
        return identifier;
    }
}
