/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.evpn.bgp.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.evpn.bgp.exceptions.BgpParseException;
import org.onosproject.evpn.bgp.types.BgpHeader;

/**
 * Abstraction of an entity providing BGP Message Reader.
 */
public interface BgpMessageReader<T> {

    /**
     * Reads the Objects in the BGP Message and Returns BGP Message.
     *
     * @param cb Channel Buffer
     * @param bgpHeader BGP message header
     * @return BGP Message
     * @throws BgpParseException while parsing BGP message.
     */
    T readFrom(ChannelBuffer cb, BgpHeader bgpHeader) throws BgpParseException;
}