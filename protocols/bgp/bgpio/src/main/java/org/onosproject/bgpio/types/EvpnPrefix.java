package org.onosproject.bgpio.types;

import org.onlab.packet.Ip4Address;

public class EvpnPrefix {
    private Ip4Address nextHop;
    private RouteTarget rt;
    MplsLabel label;

    public EvpnPrefix(Ip4Address nextHop, RouteTarget rt, MplsLabel label) {
        this.nextHop = nextHop;
        this.rt = rt;
        this.label = label;
    }

    public EvpnPrefix(Ip4Address nextHop, MplsLabel label) {
        this.nextHop = nextHop;
        this.label = label;
    }

    public Ip4Address getNextHop() {
        return nextHop;
    }

    public RouteTarget getRT() {
        return rt;
    }

    public MplsLabel getLabel() {
        return label;

    }

    public void setNetHop(Ip4Address nextHop) {
        this.nextHop = nextHop;
    }

    public void setRouteTarget(RouteTarget rt) {
        this.rt = rt;
    }

    public void setLabel(MplsLabel label) {
        this.label = label;
    }
}
