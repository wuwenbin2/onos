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

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * Default implementation of vpn port.
 */
public class DefaultVpnPort implements VpnPort {

    private final VpnPortId id;
    private final VpnInstanceId vpnInstanceId;

    /**
     * creates vpn port object.
     *
     * @param id
     * @param vpnInstanceId
     */
    public DefaultVpnPort(VpnPortId id, VpnInstanceId vpnInstanceId) {
        this.id = checkNotNull(id, "id cannot be null");
        this.vpnInstanceId = checkNotNull(vpnInstanceId,
                                          "vpnInstanceId cannot be null");
    }

    @Override
    public VpnPortId id() {
        return id;
    }

    @Override
    public VpnInstanceId vpnInstanceId() {
        return vpnInstanceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, vpnInstanceId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultVpnPort) {
            final DefaultVpnPort that = (DefaultVpnPort) obj;
            return Objects.equals(this.id, that.id)
                    && Objects.equals(this.vpnInstanceId, that.vpnInstanceId);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("id", id)
                .add("vpnInstanceId", vpnInstanceId).toString();
    }
}
