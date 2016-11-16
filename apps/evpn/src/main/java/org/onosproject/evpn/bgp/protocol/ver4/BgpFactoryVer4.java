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

package org.onosproject.evpn.bgp.protocol.ver4;

import org.onosproject.evpn.bgp.protocol.BgpFactory;
import org.onosproject.evpn.bgp.protocol.BgpKeepaliveMsg;
import org.onosproject.evpn.bgp.protocol.BgpMessage;
import org.onosproject.evpn.bgp.protocol.BgpMessageReader;
import org.onosproject.evpn.bgp.protocol.BgpNotificationMsg;
import org.onosproject.evpn.bgp.protocol.BgpOpenMsg;
import org.onosproject.evpn.bgp.protocol.BgpUpdateMsg;
import org.onosproject.evpn.bgp.protocol.BgpVersion;

/**
 * Provides BGP Factory and returns builder classes for all objects and messages.
 */
public class BgpFactoryVer4 implements BgpFactory {

    public static final BgpFactoryVer4 INSTANCE = new BgpFactoryVer4();

    @Override
    public BgpOpenMsg.Builder openMessageBuilder() {
        return new BgpOpenMsgVer4.Builder();
    }

    @Override
    public BgpKeepaliveMsg.Builder keepaliveMessageBuilder() {
        return new BgpKeepaliveMsgVer4.Builder();
    }

    @Override
    public BgpNotificationMsg.Builder notificationMessageBuilder() {
        return new BgpNotificationMsgVer4.Builder();
    }

    @Override
    public BgpUpdateMsg.Builder updateMessageBuilder() {
        return new BgpUpdateMsgVer4.Builder();
    }

    @Override
    public BgpMessageReader<BgpMessage> getReader() {
        return BgpMessageVer4.READER;
    }

    @Override
    public BgpVersion getVersion() {
        return BgpVersion.BGP_4;
    }
}