package org.onosproject.bgpio.protocol.evpn;

import java.net.InetAddress;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.MacAddress;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.EthernetSegmentidentifier;
import org.onosproject.bgpio.types.MplsLabel;
import org.onosproject.bgpio.types.RouteDistinguisher;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

public class BgpMacIpAdvNlriVer4 implements RouteTypeSpec {

    /*
     * REFERENCE : RFC 7432 BGP MPLS-Based Ethernet VPN
         +---------------------------------------+
         | RD (8 octets) |
         +---------------------------------------+
         |Ethernet Segment Identifier (10 octets)|
         +---------------------------------------+
         | Ethernet Tag ID (4 octets) |
         +---------------------------------------+
         | MAC Address Length (1 octet) |
         +---------------------------------------+
         | MAC Address (6 octets) |
         +---------------------------------------+
         | IP Address Length (1 octet) |
         +---------------------------------------+
         | IP Address (0, 4, or 16 octets) |
         +---------------------------------------+
         | MPLS Label1 (3 octets) |
         +---------------------------------------+
         | MPLS Label2 (0 or 3 octets) |
         +---------------------------------------+

      Figure : A MAC/IP Advertisement route type specific EVPN NLRI

     */

    public static final short TYPE = 2;
    protected static final Logger log = LoggerFactory.getLogger(BgpMacIpAdvNlriVer4.class);
    // unit of length is bit
    public static final short IPV4_ADDRESS_LENGTH = 32;
    public static final short MAC_ADDRESS_LENGTH = 48;
    private RouteDistinguisher rd;
    private EthernetSegmentidentifier esi;
    private int ethernetTagID;
    private byte macAddressLength;
    private MacAddress macAddress;
    private byte ipAddressLength;
    private InetAddress ipAddress;
    private MplsLabel mplsLabel1;
    private MplsLabel mplsLabel2;

    /**
     * Resets parameters.
     */
    public BgpMacIpAdvNlriVer4() {
        this.rd = null;
        this.esi = null;
        this.ethernetTagID = 0;
        this.macAddressLength = 0;
        this.macAddress = null;
        this.ipAddressLength = 0;
        this.ipAddress = null;
        this.mplsLabel1 = null;
        this.mplsLabel2 = null;
    }

    public BgpMacIpAdvNlriVer4(RouteDistinguisher rd,
                               EthernetSegmentidentifier esi,
                               int ethernetTagID, MacAddress macAddress, byte ipAddressLength,
                               InetAddress ipAddress, MplsLabel mplsLabel1,
                               MplsLabel mplsLabel2) {
        this.rd = rd;
        this.esi = esi;
        this.ethernetTagID = ethernetTagID;
        this.macAddressLength = MAC_ADDRESS_LENGTH;
        this.macAddress = macAddress;
        this.ipAddressLength = ipAddressLength;
        this.ipAddress = ipAddress;
        this.mplsLabel1 = mplsLabel1;
        this.mplsLabel2 = mplsLabel2;
    }

    public static BgpMacIpAdvNlriVer4 read(ChannelBuffer cb) throws BgpParseException {
        if (cb.readableBytes() == 0) {
            return null;
        }
        RouteDistinguisher rd = RouteDistinguisher.read(cb);
        EthernetSegmentidentifier esi = EthernetSegmentidentifier.read(cb);
        int ethernetTagID = cb.readInt();
        byte macAddressLength = cb.readByte();
        MacAddress macAddress = Validation.toMacAddress(macAddressLength / 8, cb);
        byte ipAddressLength = cb.readByte();
        InetAddress ipAddress = null;
        if (ipAddressLength > 0) {
            ipAddress = Validation.toInetAddress(ipAddressLength / 8, cb);
        }
        MplsLabel mplsLabel1 = MplsLabel.read(cb);
        MplsLabel mplsLabel2 = null;
        if (cb.readableBytes() > 0) {
            mplsLabel2 = MplsLabel.read(cb);
        }

        return new BgpMacIpAdvNlriVer4(rd, esi, ethernetTagID, macAddress,
                                       ipAddressLength, ipAddress, mplsLabel1,
                                       mplsLabel2);
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeLong(rd.getRouteDistinguisher());
        esi.write(cb);
        cb.writeInt(ethernetTagID);
        cb.writeByte(macAddressLength);
        cb.writeBytes(macAddress.toBytes());
        cb.writeByte(ipAddressLength);
        if (ipAddressLength > 0) {
            cb.writeBytes(ipAddress.getAddress());
        }
        mplsLabel1.write(cb);
        if (mplsLabel2 != null) {
            mplsLabel2.write(cb);
        }
        return cb.writerIndex() - iLenStartIndex;
    }

    public RouteDistinguisher getRouteDistinguisher() {
        return rd;
    }

    public EthernetSegmentidentifier getEthernetSegmentidentifier() {
        return esi;
    }

    public int getEthernetTagID() {
        return ethernetTagID;
    }

    public MacAddress getMacAddress() {
        return macAddress;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public MplsLabel getMplsLable1() {
        return mplsLabel1;
    }

    public MplsLabel getMplsLable2() {
        return mplsLabel2;
    }

    public void setRouteDistinguisher(RouteDistinguisher rd) {
        this.rd = rd;
    }

    public void setEthernetSegmentidentifier(EthernetSegmentidentifier esi) {
        this.esi = esi;
    }

    public void setEthernetTagID(int ethernetTagID) {
        this.ethernetTagID = ethernetTagID;
    }

    public void setMacAddress(MacAddress macAddress) {
        this.macAddress = macAddress;
    }

    public void setIpAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setMplsLable1(MplsLabel mplsLabel1) {
        this.mplsLabel1 = mplsLabel1;
    }

    public void setMplsLable2(MplsLabel mplsLabel2) {
        this.mplsLabel2 = mplsLabel2;
    }

    @Override
    public RouteType getType() {
        return RouteType.MAC_IP_ADVERTISEMENT;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues()
                .add("rd ", rd)
                .add("esi", esi)
                .add("ethernetTagID", ethernetTagID)
                .add("macAddressLength", macAddressLength)
                .add("macAddress ", macAddress)
                .add("ipAddressLength", ipAddressLength)
                .add("ipAddress", ipAddress)
                .add("mplsLabel1 ", mplsLabel1)
                .add("mplsLabel2", mplsLabel2).toString();
    }

}
