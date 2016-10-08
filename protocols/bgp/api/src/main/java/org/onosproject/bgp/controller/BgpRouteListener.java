package org.onosproject.bgp.controller;

import org.onosproject.bgpio.protocol.BgpUpdateMsg;

public interface BgpRouteListener {

    /**
     * Notify that got an update message and add route .
     *
     * @param bgpId bgp identifier
     * @param msg BGP update message
     * @throws BgpParseException BGP parse exception
     */
    void addRoute(BgpId bgpId, BgpUpdateMsg msg);

}
