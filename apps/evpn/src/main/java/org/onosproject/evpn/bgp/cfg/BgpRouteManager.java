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
package org.onosproject.evpn.bgp.cfg;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.bgp.controller.BgpCfg;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.bgp.controller.BgpId;
import org.onosproject.bgp.controller.BgpPeer.OperationType;
import org.onosproject.bgp.controller.BgpPeerCfg;
import org.onosproject.bgp.controller.BgpRouteListener;
import org.onosproject.bgp.controller.BgpRouteService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.evpn.bgp.cfg.BgpAppConfig.BgpPeerConfig;
import org.onosproject.evpn.bgp.protocol.BgpEvpnNlri;
import org.onosproject.evpn.bgp.protocol.BgpUpdateMsg;
import org.onosproject.evpn.bgp.protocol.evpn.BgpEvpnNlriVer4;
import org.onosproject.evpn.bgp.protocol.evpn.BgpMacIpAdvNlriVer4;
import org.onosproject.evpn.bgp.protocol.evpn.RouteType;
import org.onosproject.evpn.bgp.types.BgpExtendedCommunity;
import org.onosproject.evpn.bgp.types.BgpValueType;
import org.onosproject.evpn.bgp.types.EthernetSegmentidentifier;
import org.onosproject.evpn.bgp.types.MpReachNlri;
import org.onosproject.evpn.bgp.types.MpUnReachNlri;
import org.onosproject.evpn.bgp.types.MplsLabel;
import org.onosproject.evpn.bgp.types.NlriDetailsType;
import org.onosproject.evpn.bgp.types.RouteDistinguisher;
import org.onosproject.evpn.bgp.types.RouteTarget;
import org.onosproject.incubator.net.routing.EvpnRoute;
import org.onosproject.incubator.net.routing.EvpnRoute.Source;
import org.onosproject.incubator.net.routing.RouteAdminService;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

/**
 * BGP config provider to validate and populate the configuration.
 */
@Service
@Component(immediate = true)
public class BgpRouteManager implements BgpRouteService {

    private static final Logger log = getLogger(BgpRouteManager.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected BgpController bgpController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouteAdminService routeAdminService;

    private final ConfigFactory configFactory = new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY,
                                                                  BgpAppConfig.class,
                                                                  "evpnbgpapp") {
        @Override
        public BgpAppConfig createConfig() {
            return new BgpAppConfig();
        }
    };

    private final NetworkConfigListener configListener = new InternalConfigListener();
    private final InternalBgpRouteListener bgpRouteListener = new InternalBgpRouteListener();

    private ApplicationId appId;

    @Activate
    public void activate(ComponentContext context) {
        appId = coreService.registerApplication(BgpRouteService.EVPN_ROUTE_ID);
        configService.addListener(configListener);
        configRegistry.registerConfigFactory(configFactory);
        updateConfiguration();
        log.info("BGP cfg provider started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        configRegistry.unregisterConfigFactory(configFactory);
        configService.removeListener(configListener);
    }

    void setBgpController(BgpController bgpController) {
        this.bgpController = bgpController;
    }

    /**
     * Reads the configuration and set it to the BGP-LS south bound protocol.
     */
    private void readConfiguration() {
        BgpCfg bgpConfig = null;
        List<BgpAppConfig.BgpPeerConfig> nodes;
        bgpConfig = bgpController.getConfig();
        BgpAppConfig config = configRegistry.getConfig(appId,
                                                       BgpAppConfig.class);

        if (config == null) {
            log.warn("No configuration found");
            return;
        }

        /* Set the configuration */
        bgpConfig.setRouterId(config.routerId());
        bgpConfig.setAsNumber(config.localAs());
        bgpConfig.setHoldTime(config.holdTime());
        bgpConfig.setMaxSession(config.maxSession());
        bgpConfig.setLargeASCapability(config.largeAsCapability());
        bgpConfig.setEvpnCapability(config.evpnCapability());

        nodes = config.bgpPeer();
        for (int i = 0; i < nodes.size(); i++) {
            String connectMode = nodes.get(i).connectMode();
            bgpConfig.addPeer(nodes.get(i).hostname(), nodes.get(i).asNumber(),
                              nodes.get(i).holdTime());
            if (connectMode.equals(BgpAppConfig.PEER_CONNECT_ACTIVE)) {
                bgpConfig.connectPeer(nodes.get(i).hostname());
            }
        }
    }

    /**
     * Read the configuration and update it to the BGP-LS south bound protocol.
     */
    private void updateConfiguration() {
        BgpCfg bgpConfig = null;
        List<BgpAppConfig.BgpPeerConfig> nodes;
        TreeMap<String, BgpPeerCfg> bgpPeerTree;
        bgpConfig = bgpController.getConfig();
        BgpPeerCfg peer = null;
        BgpAppConfig config = configRegistry.getConfig(appId,
                                                       BgpAppConfig.class);

        if (config == null) {
            log.warn("No configuration found");
            return;
        }

        /* Update the self configuration */
        if (bgpController.connectedPeerCount() != 0) {
            // TODO: If connections already exist, disconnect
            bgpController.closeConnectedPeers();
        }
        bgpConfig.setRouterId(config.routerId());
        bgpConfig.setAsNumber(config.localAs());
        bgpConfig.setHoldTime(config.holdTime());
        bgpConfig.setMaxSession(config.maxSession());
        bgpConfig.setLargeASCapability(config.largeAsCapability());
        bgpConfig.setEvpnCapability(config.evpnCapability());

        /* update the peer configuration */
        bgpPeerTree = bgpConfig.getPeerTree();
        if (bgpPeerTree.isEmpty()) {
            log.info("There are no BGP peers to iterate");
        } else {
            Set set = bgpPeerTree.entrySet();
            Iterator i = set.iterator();
            List<BgpPeerCfg> absPeerList = new ArrayList<BgpPeerCfg>();

            boolean exists = false;

            while (i.hasNext()) {
                Map.Entry me = (Map.Entry) i.next();
                peer = (BgpPeerCfg) me.getValue();

                nodes = config.bgpPeer();
                for (int j = 0; j < nodes.size(); j++) {
                    String peerIp = nodes.get(j).hostname();
                    if (peerIp.equals(peer.getPeerRouterId())) {

                        if (bgpConfig
                                .isPeerConnectable(peer.getPeerRouterId())) {
                            peer.setAsNumber(nodes.get(j).asNumber());
                            peer.setHoldtime(nodes.get(j).holdTime());
                            log.debug("Peer neighbor IP successfully modified :"
                                    + peer.getPeerRouterId());
                        } else {
                            log.debug("Peer neighbor IP cannot be modified :"
                                    + peer.getPeerRouterId());
                        }

                        nodes.remove(j);
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    absPeerList.add(peer);
                    exists = false;
                }

                if (peer.connectPeer() != null) {
                    peer.connectPeer().disconnectPeer();
                    peer.setConnectPeer(null);
                }
            }

            /* Remove the absent nodes. */
            for (int j = 0; j < absPeerList.size(); j++) {
                bgpConfig.removePeer(absPeerList.get(j).getPeerRouterId());
            }
        }

        nodes = config.bgpPeer();
        for (int i = 0; i < nodes.size(); i++) {
            String connectMode = nodes.get(i).connectMode();
            bgpConfig.addPeer(nodes.get(i).hostname(), nodes.get(i).asNumber(),
                              nodes.get(i).holdTime());
            if (connectMode.equals(BgpAppConfig.PEER_CONNECT_ACTIVE)) {
                bgpConfig.connectPeer(nodes.get(i).hostname());
            }
        }
    }

    /**
     * Reads the configuration and set it to the BGP-LS south bound protocol.
     */
    @Override
    public void readConfiguration(String routeId, int as, short holdTime,
                                  int maxsession, boolean isLargeAs,
                                  boolean evpnCapability,
                                  List<BgpPeerConfig> bgpPeers) {
        checkNotNull(routeId);
        checkNotNull(as);
        checkNotNull(holdTime);
        checkNotNull(maxsession);
        checkNotNull(isLargeAs);
        checkNotNull(evpnCapability);
        checkNotNull(bgpPeers);

        BgpCfg bgpConfig = bgpController.getConfig();

        /* Set the configuration */
        bgpConfig.setRouterId(routeId);
        bgpConfig.setAsNumber(as);
        bgpConfig.setHoldTime(holdTime);
        bgpConfig.setMaxSession(maxsession);
        bgpConfig.setLargeASCapability(isLargeAs);
        bgpConfig.setEvpnCapability(evpnCapability);

        bgpPeers.forEach(peerConfig -> {
            String connectMode = peerConfig.connectMode();
            bgpConfig.addPeer(peerConfig.hostname(), peerConfig.asNumber(),
                              peerConfig.holdTime());
            if (connectMode.equals(BgpAppConfig.PEER_CONNECT_ACTIVE)) {
                bgpConfig.connectPeer(peerConfig.hostname());
            }
        });
    }

    @Override
    public void updateConfiguration(String routeId, int as, short holdTime,
                                    int maxsession, boolean isLargeAs,
                                    boolean evpnCapability,
                                    List<BgpPeerConfig> bgpPeers) {

        checkNotNull(routeId);
        checkNotNull(as);
        checkNotNull(holdTime);
        checkNotNull(maxsession);
        checkNotNull(isLargeAs);
        checkNotNull(evpnCapability);
        checkNotNull(bgpPeers);


        /* Update the self configuration */
        if (bgpController.connectedPeerCount() != 0) {
            // TODO: If connections already exist, disconnect
            bgpController.closeConnectedPeers();
        }

        updateConfiguration(routeId, as, holdTime, maxsession, isLargeAs,
                            evpnCapability);
        updateConfiguration(bgpPeers);

    }

    @Override
    public void updateConfiguration(List<BgpPeerConfig> bgpPeers) {

        BgpCfg bgpConfig = bgpController.getConfig();
        TreeMap<String, BgpPeerCfg> bgpPeerTree = bgpConfig.getPeerTree();

        if (bgpPeerTree.isEmpty()) {
            log.info("There are no BGP peers to iterate");
        } else {
            List<BgpPeerCfg> absPeerList = new ArrayList<BgpPeerCfg>();

            bgpPeerTree.entrySet().forEach(item -> {
                BgpPeerCfg peer = (BgpPeerCfg) item.getValue();

                boolean exists = false;

                for (BgpPeerConfig peerConfig : bgpPeers) {
                    if (peerConfig.hostname().equals(peer.getPeerRouterId())) {

                        if (bgpConfig
                                .isPeerConnectable(peer.getPeerRouterId())) {
                            peer.setAsNumber(peerConfig.asNumber());
                            peer.setHoldtime(peerConfig.holdTime());
                            log.debug("Peer neighbor IP successfully modified :"
                                    + peer.getPeerRouterId());
                        } else {
                            log.debug("Peer neighbor IP cannot be modified :"
                                    + peer.getPeerRouterId());
                        }

                        bgpPeers.remove(peerConfig);
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    absPeerList.add(peer);
                    exists = false;
                }

                if (peer.connectPeer() != null) {
                    peer.connectPeer().disconnectPeer();
                    peer.setConnectPeer(null);
                }
            });

            /* Remove the absent nodes. */
            for (int j = 0; j < absPeerList.size(); j++) {
                bgpConfig.removePeer(absPeerList.get(j).getPeerRouterId());
            }
        }

        bgpPeers.forEach(peerConfig -> {
            String connectMode = peerConfig.connectMode();
            bgpConfig.addPeer(peerConfig.hostname(), peerConfig.asNumber(),
                              peerConfig.holdTime());
            if (connectMode.equals(BgpAppConfig.PEER_CONNECT_ACTIVE)) {
                bgpConfig.connectPeer(peerConfig.hostname());
            }
        });
    }

    @Override
    public void updateConfiguration(String routeId, int as, short holdTime,
                                    int maxsession, boolean isLargeAs,
                                    boolean evpnCapability) {
        BgpCfg bgpConfig = bgpController.getConfig();
        bgpConfig.setRouterId(routeId);
        bgpConfig.setAsNumber(as);
        bgpConfig.setHoldTime(holdTime);
        bgpConfig.setMaxSession(maxsession);
        bgpConfig.setLargeASCapability(isLargeAs);
        bgpConfig.setEvpnCapability(evpnCapability);
    }

    @Override
    public void removePeer(String peer) {
        checkNotNull(peer);
        bgpController.getConfig().disconnectPeer(peer);
    }

    /**
     * BGP config listener to populate the configuration.
     */
    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (!event.configClass().equals(BgpAppConfig.class)) {
                return;
            }

            switch (event.type()) {
            case CONFIG_ADDED:
                readConfiguration();
                break;
            case CONFIG_UPDATED:
                updateConfiguration();
                break;
            case CONFIG_REMOVED:
            default:
                break;
            }
        }
    }

    private void sendUpdateMessage(OperationType operationType, String rdString,
                                   String rtString, Ip4Address nextHop,
                                   MacAddress macAddress, InetAddress ipAddress,
                                   int labelInt) {

        List<BgpEvpnNlri> eVpnComponents = new ArrayList<BgpEvpnNlri>();
        RouteDistinguisher rd = stringToRD(rdString);
        EthernetSegmentidentifier esi = new EthernetSegmentidentifier(new byte[10]);
        int ethernetTagID = 0;
        MplsLabel mplsLabel1 = intToLabel(labelInt);
        MplsLabel mplsLabel2 = null;

        List<BgpValueType> extCom = new ArrayList<BgpValueType>();
        if (operationType == OperationType.UPDATE && rtString != null) {
            RouteTarget rTarget = stringToRT(rtString);
            extCom.add(rTarget);
        }
        BgpMacIpAdvNlriVer4 routeTypeSpec = new BgpMacIpAdvNlriVer4(rd, esi,
                                                                    ethernetTagID,
                                                                    macAddress,
                                                                    ipAddress,
                                                                    mplsLabel1,
                                                                    mplsLabel2);
        BgpEvpnNlri nlri = new BgpEvpnNlriVer4(RouteType.MAC_IP_ADVERTISEMENT
                .getType(), routeTypeSpec);
        eVpnComponents.add(nlri);

        bgpController.getPeers().forEach(peer -> {
            log.info("Send route update eVpnComponents {} to peer {}",
                     eVpnComponents, peer);
            peer.updateEvpn(operationType, nextHop, extCom, eVpnComponents);
        });

    }

    private static RouteDistinguisher stringToRD(String rdString) {
        if (rdString.contains(":")) {
            if ((rdString.indexOf(":") != 0)
                    && (rdString.indexOf(":") != rdString.length() - 1)) {
                String[] tem = rdString.split(":");
                short as = (short) Integer.parseInt(tem[0]);
                int assignednum = Integer.parseInt(tem[1]);
                long rd = ((long) assignednum & 0xFFFFFFFFL)
                        | (((long) as << 32) & 0xFFFFFFFF00000000L);
                return new RouteDistinguisher(rd);
            }
        }
        return null;

    }

    private static String rdToString(RouteDistinguisher rd) {
        long rdLong = rd.getRouteDistinguisher();
        int as = (int) ((rdLong & 0xFFFFFFFF00000000L) >> 32);
        int assignednum = (int) (rdLong & 0xFFFFFFFFL);
        String result = as + ":" + assignednum;
        return result;
    }

    private static RouteTarget stringToRT(String rdString) {
        if (rdString.contains(":")) {
            if ((rdString.indexOf(":") != 0)
                    && (rdString.indexOf(":") != rdString.length() - 1)) {
                String[] tem = rdString.split(":");
                short as = Short.parseShort(tem[0]);
                int assignednum = Integer.parseInt(tem[1]);

                byte[] rt = new byte[] {(byte) ((as >> 8) & 0xFF),
                                        (byte) (as & 0xFF),
                                        (byte) ((assignednum >> 24) & 0xFF),
                                        (byte) ((assignednum >> 16) & 0xFF),
                                        (byte) ((assignednum >> 8) & 0xFF),
                                        (byte) (assignednum & 0xFF) };
                short type = 0x02;
                return new RouteTarget(type, rt);
            }
        }
        return null;

    }

    private static String rtToString(RouteTarget rt) {
        byte[] b = rt.getRouteTarget();

        int assignednum = b[5] & 0xFF | (b[4] & 0xFF) << 8 | (b[3] & 0xFF) << 16
                | (b[2] & 0xFF) << 24;
        short as = (short) (b[1] & 0xFF | (b[0] & 0xFF) << 8);
        String result = as + ":" + assignednum;
        return result;
    }

    private static MplsLabel intToLabel(int labelInt) {
        byte[] label = new byte[] {(byte) ((labelInt >> 16) & 0xFF),
                                   (byte) ((labelInt >> 8) & 0xFF),
                                   (byte) (labelInt & 0xFF) };

        return new MplsLabel(label);
    }

    private static int labelToInt(MplsLabel label) {
        byte[] b = label.getMplsLabel();
        return b[2] & 0xFF | (b[1] & 0xFF) << 8 | (b[0] & 0xFF) << 16;

    }

    private class InternalBgpRouteListener implements BgpRouteListener {

        @Override
        public void processRoute(BgpId bgpId, BgpUpdateMsg updateMsg) {
            List<BgpValueType> pathAttr = updateMsg.bgpPathAttributes()
                    .pathAttributes();
            Iterator<BgpValueType> iterator = pathAttr.iterator();
            RouteTarget rt = null;
            List<BgpEvpnNlri> evpnReachNlri = new LinkedList<>();
            List<BgpEvpnNlri> evpnUnreachNlri = new LinkedList<>();

            Ip4Address ipNextHop = null;
            while (iterator.hasNext()) {
                BgpValueType attr = iterator.next();
                if (attr instanceof MpReachNlri) {
                    MpReachNlri mpReachNlri = (MpReachNlri) attr;
                    ipNextHop = mpReachNlri.nexthop4();
                    if (mpReachNlri
                            .getNlriDetailsType() == NlriDetailsType.EVPN) {
                        evpnReachNlri.addAll(mpReachNlri.bgpEvpnNlri());
                    }

                }
                if (attr instanceof MpUnReachNlri) {
                    MpUnReachNlri mpUnReachNlri = (MpUnReachNlri) attr;
                    if (mpUnReachNlri
                            .getNlriDetailsType() == NlriDetailsType.EVPN) {
                        evpnUnreachNlri.addAll(mpUnReachNlri.bgpEvpnNlri());
                    }
                }

                if (attr instanceof BgpExtendedCommunity) {
                    BgpExtendedCommunity extCom = (BgpExtendedCommunity) attr;
                    Iterator<BgpValueType> extIte = extCom.fsActionTlv()
                            .iterator();
                    while (extIte.hasNext()) {
                        BgpValueType extAttr = extIte.next();
                        if (extAttr instanceof RouteTarget) {
                            rt = (RouteTarget) extAttr;
                            break;
                        }
                    }
                }
            }

            if ((rt != null) && (!evpnReachNlri.isEmpty())) {
                for (BgpEvpnNlri nlri : evpnReachNlri) {
                    if (nlri.getRouteType() == RouteType.MAC_IP_ADVERTISEMENT) {
                        BgpMacIpAdvNlriVer4 macIpAdvNlri = (BgpMacIpAdvNlriVer4) nlri
                                .getRouteTypeSpec();
                        MacAddress macAddress = macIpAdvNlri.getMacAddress();
                        Ip4Address ipAddress = Ip4Address
                                .valueOf(macIpAdvNlri.getIpAddress());
                        RouteDistinguisher rd = macIpAdvNlri
                                .getRouteDistinguisher();
                        MplsLabel label = macIpAdvNlri.getMplsLable1();
                        log.info("Route Provider received bgp packet {} to route system.",
                                 macIpAdvNlri.toString());
                        // Add route to route system
                        Source source = Source.REMOTE;
                        EvpnRoute evpnRoute = new EvpnRoute(source, macAddress,
                                                            ipAddress,
                                                            ipNextHop,
                                                            rdToString(rd),
                                                            rtToString(rt),
                                                            labelToInt(label));
                        routeAdminService
                                .update(Collections.singleton(evpnRoute));
                    }
                }
            }

            if (!evpnUnreachNlri.isEmpty()) {
                for (BgpEvpnNlri nlri : evpnUnreachNlri) {
                    if (nlri.getRouteType() == RouteType.MAC_IP_ADVERTISEMENT) {
                        BgpMacIpAdvNlriVer4 macIpAdvNlri = (BgpMacIpAdvNlriVer4) nlri
                                .getRouteTypeSpec();
                        MacAddress macAddress = macIpAdvNlri.getMacAddress();
                        Ip4Address ipAddress = Ip4Address
                                .valueOf(macIpAdvNlri.getIpAddress());
                        RouteDistinguisher rd = macIpAdvNlri
                                .getRouteDistinguisher();
                        MplsLabel label = macIpAdvNlri.getMplsLable1();
                        log.info("Route Provider received bgp packet {} and remove from route system.",
                                 macIpAdvNlri.toString());
                        // Delete route from route system
                        Source source = Source.REMOTE;
                        // For mpUnreachNlri, nexthop and rt is null
                        EvpnRoute evpnRoute = new EvpnRoute(source, macAddress,
                                                            ipAddress, null,
                                                            rdToString(rd),
                                                            null,
                                                            labelToInt(label));
                        routeAdminService
                                .withdraw(Collections.singleton(evpnRoute));
                    }
                }
            }
        }
    }

    @Override
    public void sendEvpnRoute(EvpnRoute.OperationType type,
                              EvpnRoute evpnRoute) {
        OperationType operationType = null;
        switch (type) {
        case UPDATE:
            operationType = OperationType.UPDATE;
            break;
        case REMOVE:
            operationType = OperationType.DELETE;
            break;
        default:
            break;
        }
        String rdString = evpnRoute.routeDistinguisher()
                .getRouteDistinguisher();
        MacAddress macAddress = evpnRoute.prefixMac();
        InetAddress inetAddress = evpnRoute.prefixIp().toInetAddress();
        Ip4Address nextHop = evpnRoute.ipNextHop();
        String rtString = evpnRoute.routeTarget().getRouteTarget();
        int labelInt = evpnRoute.label().getLabel();
        sendUpdateMessage(operationType, rdString, rtString, nextHop,
                          macAddress, inetAddress, labelInt);
    }

    @Override
    public BgpController getController() {
        return bgpController;
    }

}
