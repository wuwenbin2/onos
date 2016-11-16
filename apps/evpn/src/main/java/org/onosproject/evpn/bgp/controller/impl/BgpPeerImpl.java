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

package org.onosproject.evpn.bgp.controller.impl;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.netty.channel.Channel;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.bgp.controller.BgpPeer;
import org.onosproject.bgp.controller.BgpSessionInfo;
import org.onosproject.evpn.bgp.exceptions.BgpParseException;
import org.onosproject.evpn.bgp.protocol.BgpEvpnNlri;
import org.onosproject.evpn.bgp.protocol.BgpFactories;
import org.onosproject.evpn.bgp.protocol.BgpFactory;
import org.onosproject.evpn.bgp.protocol.BgpMessage;
import org.onosproject.evpn.bgp.types.As4Path;
import org.onosproject.evpn.bgp.types.AsPath;
import org.onosproject.evpn.bgp.types.BgpExtendedCommunity;
import org.onosproject.evpn.bgp.types.BgpValueType;
import org.onosproject.evpn.bgp.types.MpReachNlri;
import org.onosproject.evpn.bgp.types.MpUnReachNlri;
import org.onosproject.evpn.bgp.types.MultiProtocolExtnCapabilityTlv;
import org.onosproject.evpn.bgp.types.Origin;
import org.onosproject.evpn.bgp.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

/**
 * BGPPeerImpl implements BGPPeer, maintains peer information and store updates
 * in RIB .
 */
public class BgpPeerImpl implements BgpPeer {

    protected final Logger log = LoggerFactory.getLogger(BgpPeerImpl.class);

    private static final String SHUTDOWN_MSG = "Worker has already been shutdown";

    private BgpController bgpController;
    private Channel channel;
    protected String channelId;
    private boolean connected;
    protected boolean isHandShakeComplete = false;
    private BgpSessionInfo sessionInfo;
    private BgpPacketStatsImpl pktStats;


    @Override
    public BgpSessionInfo sessionInfo() {
        return sessionInfo;
    }

    /**
     * Initialize peer.
     *
     * @param bgpController controller instance
     * @param sessionInfo bgp session info
     * @param pktStats packet statistics
     */
    public BgpPeerImpl(BgpController bgpController, BgpSessionInfo sessionInfo,
                       BgpPacketStatsImpl pktStats) {
        this.bgpController = bgpController;
        this.sessionInfo = sessionInfo;
        this.pktStats = pktStats;
    }

    /**
     * Check if peer support capability.
     *
     * @param type capability type
     * @param afi address family identifier
     * @param sAfi subsequent address family identifier
     * @return true if capability is supported, otherwise false
     */
    public final boolean isCapabilitySupported(short type, short afi,
                                               byte sAfi) {

        List<BgpValueType> capability = sessionInfo.remoteBgpCapability();
        ListIterator<BgpValueType> listIterator = capability.listIterator();

        while (listIterator.hasNext()) {
            BgpValueType tlv = listIterator.next();

            if (tlv.getType() == type) {
                if (tlv.getType() == MultiProtocolExtnCapabilityTlv.TYPE) {
                    MultiProtocolExtnCapabilityTlv temp = (MultiProtocolExtnCapabilityTlv) tlv;
                    if ((temp.getAfi() == afi) && (temp.getSafi() == sAfi)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void updateEvpn(OperationType operType, Ip4Address nextHop,
                           List<BgpValueType> extCommunit,
                           List<BgpEvpnNlri> eVpnComponents) {
        Preconditions.checkNotNull(operType, "Operation type cannot be null");
        Preconditions.checkNotNull(eVpnComponents, "Evpn nlri cannot be null");
        sendEvpnUpdateMessageToPeer(operType, nextHop, extCommunit, eVpnComponents);
    }

    private void sendEvpnUpdateMessageToPeer(OperationType operType,
                                             Ip4Address nextHop,
                                             List<BgpValueType> extCommunit,
                                             List<BgpEvpnNlri> eVpnComponents) {
        List<BgpValueType> attributesList = new LinkedList<>();
        byte sessionType = sessionInfo.isIbgpSession() ? (byte) 0 : (byte) 1;
        short afi = Constants.AFI_EVPN_VALUE;
        byte safi = Constants.SAFI_EVPN_VALUE;
        boolean isEvpnCapabilitySet = isCapabilitySupported(MultiProtocolExtnCapabilityTlv.TYPE,
                                                            afi, safi);

        if (!isEvpnCapabilitySet) {
            log.debug("Peer do not support BGP Evpn capability",
                      channel.getRemoteAddress());
            return;
        }
        attributesList.add(new Origin((byte) 0));

        if (sessionType != 0) {
            // EBGP
            if (!bgpController.getConfig().getLargeASCapability()) {
                List<Short> aspathSet = new ArrayList<>();
                List<Short> aspathSeq = new ArrayList<>();
                aspathSeq.add((short) bgpController.getConfig().getAsNumber());

                AsPath asPath = new AsPath(aspathSet, aspathSeq);
                attributesList.add(asPath);
            } else {
                List<Integer> aspathSet = new ArrayList<>();
                List<Integer> aspathSeq = new ArrayList<>();
                aspathSeq.add(bgpController.getConfig().getAsNumber());

                As4Path as4Path = new As4Path(aspathSet, aspathSeq);
                attributesList.add(as4Path);
            }
        } else {
            attributesList.add(new AsPath());
        }

        if (!extCommunit.isEmpty()) {
            attributesList.add(new BgpExtendedCommunity(extCommunit));
        }
        if (operType == OperationType.ADD || operType == OperationType.UPDATE) {
            attributesList
                    .add(new MpReachNlri(eVpnComponents, afi, safi, nextHop));
        } else if (operType == OperationType.DELETE) {
            attributesList.add(new MpUnReachNlri(eVpnComponents, afi, safi));
        }

        BgpMessage msg = Controller.getBgpMessageFactory4()
                .updateMessageBuilder().setBgpPathAttributes(attributesList)
                .build();
        channel.write(Collections.singletonList(msg));
    }

    @Override
    public void buildAdjRibIn(List<BgpValueType> pathAttr)
            throws BgpParseException {
        ListIterator<BgpValueType> iterator = pathAttr.listIterator();
        while (iterator.hasNext()) {
            BgpValueType attr = iterator.next();
            if (attr instanceof MpReachNlri) {
                MpReachNlri mpReachNlri = (MpReachNlri) attr;
                switch (mpReachNlri.getNlriDetailsType()) {
                case EVPN:
                    List<BgpEvpnNlri> evpnNlri = ((MpReachNlri) attr)
                            .bgpEvpnNlri();
                    break;
                default:
                    break;
                }

            }
            if (attr instanceof MpUnReachNlri) {
                MpReachNlri mpUnReachNlri = (MpReachNlri) attr;
                switch (mpUnReachNlri.getNlriDetailsType()) {
                case EVPN:
                    List<BgpEvpnNlri> evpnNlri = ((MpUnReachNlri) attr)
                            .bgpEvpnNlri();
                    break;
                default:
                    break;

                }
            }
        }
    }

    // ************************
    // Channel related
    // ************************

    @Override
    public final void disconnectPeer() {
        this.channel.close();
    }

    @Override
    public final void sendMessage(BgpMessage m) {
        log.debug("Sending message to {}", channel.getRemoteAddress());
        try {
            channel.write(Collections.singletonList(m));
            this.pktStats.addOutPacket();
        } catch (RejectedExecutionException e) {
            log.warn(e.getMessage());
            if (!e.getMessage().contains(SHUTDOWN_MSG)) {
                throw e;
            }
        }
    }

    @Override
    public final void sendMessage(List<BgpMessage> msgs) {
        try {
            channel.write(msgs);
            this.pktStats.addOutPacket(msgs.size());
        } catch (RejectedExecutionException e) {
            log.warn(e.getMessage());
            if (!e.getMessage().contains(SHUTDOWN_MSG)) {
                throw e;
            }
        }
    }

    @Override
    public final boolean isConnected() {
        return this.connected;
    }

    @Override
    public final void setConnected(boolean connected) {
        this.connected = connected;
    };

    @Override
    public final void setChannel(Channel channel) {
        this.channel = channel;
        final SocketAddress address = channel.getRemoteAddress();
        if (address instanceof InetSocketAddress) {
            final InetSocketAddress inetAddress = (InetSocketAddress) address;
            final IpAddress ipAddress = IpAddress
                    .valueOf(inetAddress.getAddress());
            if (ipAddress.isIp4()) {
                channelId = ipAddress.toString() + ':' + inetAddress.getPort();
            } else {
                channelId = '[' + ipAddress.toString() + "]:"
                        + inetAddress.getPort();
            }
        }
    };

    @Override
    public final Channel getChannel() {
        return this.channel;
    };

    @Override
    public String channelId() {
        return channelId;
    }

    @Override
    public BgpFactory factory() {
        return BgpFactories.getFactory(sessionInfo.remoteBgpVersion());
    }

    @Override
    public boolean isHandshakeComplete() {
        return isHandShakeComplete;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues()
                .add("channel", channelId())
                .add("BgpId", sessionInfo().remoteBgpId()).toString();
    }

}
