package org.onosproject.bgp.controller;

import org.onosproject.bgpio.protocol.BgpUpdateMsg;

public interface BgpRouteListener {

    /**
     * Notify that got an update message and operate route .
     *
     * @param bgpId bgp identifier
     * @param msg BGP update message
     */
    void processRoute(BgpId bgpId, BgpUpdateMsg msg);

}
