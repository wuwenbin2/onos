package org.onosproject.incubator.net.routing;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

import org.onlab.packet.IpAddress;

public final class IpNextHop implements NextHop {
    private final IpAddress ipAddress;

    /**
     * Constructor to initialize parameters.
     *
     * @param routeDistinguisher route distinguisher
     */
    private IpNextHop(IpAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    public static IpNextHop ipAddress(IpAddress ipAddress) {
        return new IpNextHop(ipAddress);
    }

    public IpAddress getIpAddress() {
        return ipAddress;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof IpNextHop) {
            IpNextHop other = (IpNextHop) obj;
            return Objects.equals(ipAddress, other.ipAddress);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("ipAddress", ipAddress).toString();
    }

}
