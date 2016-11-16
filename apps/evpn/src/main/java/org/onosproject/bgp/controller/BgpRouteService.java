package org.onosproject.bgp.controller;

import java.util.List;

import org.onosproject.evpn.bgp.cfg.BgpAppConfig.BgpPeerConfig;
import org.onosproject.incubator.net.routing.EvpnRoute;

public interface BgpRouteService {

    public static final String EVPN_ROUTE_ID = "org.onosproject.evpn.route";

    void sendEvpnRoute(EvpnRoute.OperationType type, EvpnRoute evpnRoute);

    BgpController getController();

    void readConfiguration(String routeId, int as, short holdTime,
                           int maxsession, boolean isLargeAs,
                           boolean evpnCapability,
                           List<BgpPeerConfig> bgpPeers);

    void updateConfiguration(String routeId, int as, short holdTime,
                             int maxsession, boolean isLargeAs,
                             boolean evpnCapability,
                             List<BgpPeerConfig> bgpPeers);

    void updateConfiguration(List<BgpPeerConfig> bgpPeers);

    void updateConfiguration(String routeId, int as, short holdTime,
                             int maxsession, boolean isLargeAs,
                             boolean evpnCapability);

    void removePeer(String peer);
}
