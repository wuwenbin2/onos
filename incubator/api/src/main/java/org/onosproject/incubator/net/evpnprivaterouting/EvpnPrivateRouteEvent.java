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

package org.onosproject.incubator.net.evpnprivaterouting;

import org.onosproject.event.AbstractEvent;

import java.util.Objects;

/**
 * Describes an event about a route.
 */
public class EvpnPrivateRouteEvent extends AbstractEvent<EvpnPrivateRouteEvent.Type, EvpnPrivateRoute> {

    /**
     * Route event type.
     */
    public enum Type {

        /**
         * Route is new.
         */
        ROUTE_ADDED,

        /**
         * Route has updated information.
         */
        ROUTE_UPDATED,

        /**
         * Route was removed.
         */
        ROUTE_REMOVED
    }

    /**
     * Creates a new route event.
     *
     * @param type event type
     * @param subject event subject
     */
    public EvpnPrivateRouteEvent(Type type, EvpnPrivateRoute subject) {
        super(type, subject);
    }

    /**
     * Creates a new route event.
     *
     * @param type event type
     * @param subject event subject
     * @param time event time
     */
    protected EvpnPrivateRouteEvent(Type type, EvpnPrivateRoute subject, long time) {
        super(type, subject, time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject(), type());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof EvpnPrivateRouteEvent)) {
            return false;
        }

        EvpnPrivateRouteEvent that = (EvpnPrivateRouteEvent) other;

        return Objects.equals(this.subject(), that.subject()) &&
                Objects.equals(this.type(), that.type());
    }
}
