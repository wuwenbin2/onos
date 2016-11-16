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

import java.util.LinkedList;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.evpn.bgp.exceptions.BgpParseException;
import org.onosproject.evpn.bgp.protocol.BgpEvpnNlri;
import org.onosproject.evpn.bgp.protocol.evpn.BgpEvpnNlriVer4;
import org.onosproject.evpn.bgp.protocol.evpn.BgpMacIpAdvNlriVer4;
import org.onosproject.evpn.bgp.util.Constants;
import org.onosproject.evpn.bgp.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides Implementation of MpUnReach Nlri BGP Path Attribute.
 */
public class MpUnReachNlri implements BgpValueType {

    private static final Logger log = LoggerFactory
            .getLogger(MpUnReachNlri.class);
    public static final byte MPUNREACHNLRI_TYPE = 15;
    public static final byte LINK_NLRITYPE = 2;
    public static final byte FLAGS = (byte) 0x90;
    private boolean isMpUnReachNlri = false;
    private final short afi;
    private final byte safi;
    private final int length;
    private List<BgpEvpnNlri> evpnNlri;

    /**
     * Constructor to initialize parameters.
     *
     * @param mpUnReachNlri MpUnReach Nlri attribute
     * @param afi address family identifier
     * @param safi subsequent address family identifier
     * @param length of MpUnReachNlri
     */

    public MpUnReachNlri(List<BgpEvpnNlri> evpnNlri, short afi, byte safi) {
        this.isMpUnReachNlri = true;
        this.length = 0;
        this.evpnNlri = evpnNlri;
        this.afi = afi;
        this.safi = safi;
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
     * Reads from ChannelBuffer and parses MpUnReachNlri.
     *
     * @param cb ChannelBuffer
     * @return object of MpUnReachNlri
     * @throws BgpParseException while parsing MpUnReachNlri
     */
    public static MpUnReachNlri read(ChannelBuffer cb)
            throws BgpParseException {
        ChannelBuffer tempBuf = cb.copy();
        Validation parseFlags = Validation.parseAttributeHeader(cb);
        int len = parseFlags.isShort()
                                       ? parseFlags.getLength()
                                               + Constants.TYPE_AND_LEN_AS_SHORT
                                       : parseFlags.getLength()
                                               + Constants.TYPE_AND_LEN_AS_BYTE;
        ChannelBuffer data = tempBuf.readBytes(len);

        if (!parseFlags.getFirstBit() && parseFlags.getSecondBit()
                && parseFlags.getThirdBit()) {
            throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                        BgpErrorType.ATTRIBUTE_FLAGS_ERROR,
                                        data);
        }

        if (cb.readableBytes() < parseFlags.getLength()) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   parseFlags.getLength());
        }

        short afi = 0;
        byte safi = 0;
        List<BgpEvpnNlri> eVpnComponents = new LinkedList<>();
        ChannelBuffer tempCb = cb.readBytes(parseFlags.getLength());
        while (tempCb.readableBytes() > 0) {
            afi = tempCb.readShort();
            safi = tempCb.readByte();

            // Supporting only for AFI 16388 / SAFI 71
            if ((afi == Constants.AFI_EVPN_VALUE)
                    && (safi == Constants.SAFI_EVPN_VALUE)) {
                while (tempCb.readableBytes() > 0) {
                    BgpEvpnNlri eVpnComponent = BgpEvpnNlriVer4.read(tempCb);
                    eVpnComponents.add(eVpnComponent);
                    log.info("=====evpn Component is {} ======", eVpnComponent);
                }

            } else {
                // TODO: check with the values got from capability
                throw new BgpParseException("Not Supporting afi " + afi
                        + "safi " + safi);
            }
        }
        return new MpUnReachNlri(eVpnComponents, afi, safi);
    }

    @Override
    public short getType() {
        return MPUNREACHNLRI_TYPE;
    }

    /**
     * Returns SAFI.
     *
     * @return SAFI
     */
    public byte safi() {
        return this.safi;
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
     * Returns whether MpReachNlri is present.
     *
     * @return whether MpReachNlri is present
     */
    public boolean isMpUnReachNlriSet() {
        return this.isMpUnReachNlri;
    }

    /**
     * Returns length of MpUnReach.
     *
     * @return length of MpUnReach
     */
    public int mpUnReachNlriLen() {
        return this.length;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        if ((afi == Constants.AFI_EVPN_VALUE)
                && (safi == Constants.SAFI_EVPN_VALUE)) {

            cb.writeByte(FLAGS);
            cb.writeByte(MPUNREACHNLRI_TYPE);

            int mpUnReachDataIndex = cb.writerIndex();
            cb.writeShort(0);
            cb.writeShort(afi);
            cb.writeByte(safi);

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
                    cb.setByte(iSpecStartIndex, (short) (cb.writerIndex()
                            - iSpecStartIndex - 1));
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

            int evpnNlriLen = cb.writerIndex() - mpUnReachDataIndex;
            cb.setShort(mpUnReachDataIndex, (short) (evpnNlriLen - 2));
        }
        return cb.writerIndex() - iLenStartIndex;
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues()
                .add("afi", afi).add("safi", safi).add("length", length)
                .toString();
    }
}
