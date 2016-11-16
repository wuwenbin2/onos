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

package org.onosproject.evpn.bgp.types;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.Ip4Address;
import org.onosproject.evpn.bgp.exceptions.BgpParseException;
import org.onosproject.evpn.bgp.protocol.BgpEvpnNlri;
import org.onosproject.evpn.bgp.protocol.evpn.BgpEvpnNlriVer4;
import org.onosproject.evpn.bgp.protocol.evpn.BgpMacIpAdvNlriVer4;
import org.onosproject.evpn.bgp.util.Constants;
import org.onosproject.evpn.bgp.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/*
 * Provides Implementation of MpReach Nlri BGP Path Attribute.
 */
public class MpReachNlri implements BgpValueType {

    private static final Logger log = LoggerFactory
            .getLogger(MpReachNlri.class);
    public static final byte MPREACHNLRI_TYPE = 14;
    public static final byte LINK_NLRITYPE = 2;
    public static final byte FLAGS = (byte) 0x90;
    private boolean isMpReachNlri = false;
    private final int length;
    private final short afi;
    private final byte safi;
    private final Ip4Address ipNextHop;
    private List<BgpEvpnNlri> evpnNlri;

    /**
     * Constructor to initialize parameters.
     *
     * @param mpReachNlri MpReach Nlri attribute
     * @param afi address family identifier
     * @param safi subsequent address family identifier
     * @param ipNextHop nexthop IpAddress
     * @param length of MpReachNlri
     */

    public MpReachNlri(List<BgpEvpnNlri> evpnNlri, short afi, byte safi,
                       Ip4Address ipNextHop) {
        this.length = 42;
        this.ipNextHop = ipNextHop;
        this.isMpReachNlri = true;
        this.evpnNlri = evpnNlri;
        this.afi = afi;
        this.safi = safi;
    }

    /**
     * Returns whether MpReachNlri is present.
     *
     * @return whether MpReachNlri is present
     */
    public boolean isMpReachNlriSet() {
        return this.isMpReachNlri;
    }

    /**
     * Returns length of MpReachNlri.
     *
     * @return length of MpReachNlri
     */
    public int mpReachNlriLen() {
        return this.length;
    }

    /**
     * Returns BGP Evpn info.
     *
     * @return BGP Evpn info
     */
    public List<BgpEvpnNlri> bgpEvpnNlri() {
        return this.evpnNlri;
    }

    /**
     * Returns afi.
     *
     * @return afi
     */
    public short getAfi() {
        return this.afi;
    }

    /**
     * Returns safi.
     *
     * @return safi
     */
    public byte getSafi() {
        return this.safi();
    }

    /**
     * Returns mpReachNlri details type.
     *
     * @return type
     */
    public NlriDetailsType getNlriDetailsType() {
        if ((afi == Constants.AFI_EVPN_VALUE)
                && (safi == Constants.SAFI_EVPN_VALUE)) {
            return NlriDetailsType.EVPN;
        }

        return null;
    }

    /**
     * Reads from ChannelBuffer and parses MpReachNlri.
     *
     * @param cb channelBuffer
     * @return object of MpReachNlri
     * @throws BgpParseException while parsing MpReachNlri
     */
    public static MpReachNlri read(ChannelBuffer cb) throws BgpParseException {
        ChannelBuffer tempBuf = cb.copy();
        Validation parseFlags = Validation.parseAttributeHeader(cb);
        int len = parseFlags.isShort()
                                       ? parseFlags.getLength()
                                               + Constants.TYPE_AND_LEN_AS_SHORT
                                       : parseFlags.getLength()
                                               + Constants.TYPE_AND_LEN_AS_BYTE;
        ChannelBuffer data = tempBuf.readBytes(len);

        if (cb.readableBytes() < parseFlags.getLength()) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   parseFlags.getLength());
        }
        if (!parseFlags.getFirstBit() && parseFlags.getSecondBit()
                && parseFlags.getThirdBit()) {
            throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                        BgpErrorType.ATTRIBUTE_FLAGS_ERROR,
                                        data);
        }

        ChannelBuffer tempCb = cb.readBytes(parseFlags.getLength());
        short afi = 0;
        byte safi = 0;
        List<BgpEvpnNlri> eVpnComponents = new LinkedList<>();
        Ip4Address ipNextHop = null;
        while (tempCb.readableBytes() > 0) {
            afi = tempCb.readShort();
            safi = tempCb.readByte();

            // Supporting for AFI 16388 / SAFI 71 and VPN AFI 16388 / SAFI 128
            if ((afi == Constants.AFI_EVPN_VALUE)
                    && (safi == Constants.SAFI_EVPN_VALUE)) {

                byte nextHopLen = tempCb.readByte();
                InetAddress ipAddress = Validation.toInetAddress(nextHopLen,
                                                                 tempCb);
                if (ipAddress.isMulticastAddress()) {
                    throw new BgpParseException("Multicast not supported");
                }
                ipNextHop = Ip4Address.valueOf(ipAddress);
                byte reserved = tempCb.readByte();
                while (tempCb.readableBytes() > 0) {
                    BgpEvpnNlri eVpnComponent = BgpEvpnNlriVer4.read(tempCb);
                    eVpnComponents.add(eVpnComponent);
                }

            } else {
                throw new BgpParseException("Not Supporting afi " + afi
                        + "safi " + safi);
            }

        }

        return new MpReachNlri(eVpnComponents, afi, safi, ipNextHop);
    }

    @Override
    public short getType() {
        return MPREACHNLRI_TYPE;
    }

    /**
     * Returns AFI.
     *
     * @return AFI
     */
    public short afi() {
        return this.afi;
    }

    /**
     * Returns Nexthop IpAddress.
     *
     * @return Nexthop IpAddress
     */
    public Ip4Address nexthop4() {
        return this.ipNextHop;
    }

    /**
     * Returns SAFI.
     *
     * @return SAFI
     */
    public byte safi() {
        return this.safi;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();

        if ((afi == Constants.AFI_EVPN_VALUE)
                && (safi == Constants.SAFI_EVPN_VALUE)) {

            cb.writeByte(FLAGS);
            cb.writeByte(MPREACHNLRI_TYPE);

            int mpReachDataIndex = cb.writerIndex();
            cb.writeShort(0);
            cb.writeShort(afi);
            cb.writeByte(safi);
            // ip address length
            cb.writeByte(0x04);
            cb.writeInt(ipNextHop.toInt());
            // sub network points of attachment
            cb.writeByte(0);

            for (BgpEvpnNlri element : evpnNlri) {
                short routeType = element.getType();
                switch (routeType) {
                case Constants.BGP_EVPN_MAC_IP_ADVERTISEMENT:
                    cb.writeByte(element.getType());
                    int iSpecStartIndex = cb.writerIndex();
                    cb.writeByte(0);
                    BgpMacIpAdvNlriVer4 macIpAdvNlri = (BgpMacIpAdvNlriVer4) element
                            .getRouteTypeSpec();
                    macIpAdvNlri.write(cb);
                    cb.setByte(iSpecStartIndex,
                               (byte) (cb.writerIndex() - iSpecStartIndex - 1));
                    ChannelBuffer temcb = cb.copy();
                    break;
                case Constants.BGP_EVPN_ETHERNET_AUTO_DISCOVERY:
                    break;
                case Constants.BGP_EVPN_INCLUSIVE_MULTICASE_ETHERNET:
                    break;
                case Constants.BGP_EVPN_ETHERNET_SEGMENT:
                    break;
                default:
                    break;
                }
            }
            int evpnNlriLen = cb.writerIndex() - mpReachDataIndex;
            cb.setShort(mpReachDataIndex, (short) (evpnNlriLen - 2));
        }

        return cb.writerIndex() - iLenStartIndex;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues()
                .add("afi", afi).add("safi", safi).add("ipNextHop", ipNextHop)
                .add("length", length).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }

}
