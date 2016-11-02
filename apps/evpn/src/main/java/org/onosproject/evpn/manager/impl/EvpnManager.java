/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.evpn.manager.impl;

import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.EthType.EtherType;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MplsLabel;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.evpn.manager.EvpnService;
import org.onosproject.evpn.rsc.VpnInstance;
import org.onosproject.evpn.rsc.VpnInstanceId;
import org.onosproject.evpn.rsc.VpnPort;
import org.onosproject.evpn.rsc.VpnPortId;
import org.onosproject.evpn.rsc.baseport.BasePortService;
import org.onosproject.evpn.rsc.vpninstance.VpnInstanceService;
import org.onosproject.evpn.rsc.vpnport.VpnPortEvent;
import org.onosproject.evpn.rsc.vpnport.VpnPortListener;
import org.onosproject.evpn.rsc.vpnport.VpnPortService;
import org.onosproject.incubator.net.resource.label.LabelResource;
import org.onosproject.incubator.net.resource.label.LabelResourceAdminService;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.resource.label.LabelResourceService;
import org.onosproject.incubator.net.routing.EvpnInstance;
import org.onosproject.incubator.net.routing.EvpnInstanceName;
import org.onosproject.incubator.net.routing.EvpnInstanceNextHop;
import org.onosproject.incubator.net.routing.EvpnInstancePrefix;
import org.onosproject.incubator.net.routing.EvpnInstanceRoute;
import org.onosproject.incubator.net.routing.EvpnRoute;
import org.onosproject.incubator.net.routing.EvpnRoute.Source;
import org.onosproject.incubator.net.routing.Label;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteAdminService;
import org.onosproject.incubator.net.routing.RouteDistinguisher;
import org.onosproject.incubator.net.routing.RouteEvent;
import org.onosproject.incubator.net.routing.RouteListener;
import org.onosproject.incubator.net.routing.RouteService;
import org.onosproject.incubator.net.routing.RouteTableType;
import org.onosproject.incubator.net.routing.RouteTarget;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment.Builder;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective.Flag;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.Objective.Operation;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.vtn.util.VtnData;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

@Component(immediate = true)
@Service
public class EvpnManager implements EvpnService {
    private final Logger log = getLogger(getClass());
    private static final String APP_ID = "org.onosproject.app.evpn";
    private static final int ARP_PRIORITY = 0xffff;
    private static final short ARP_RESPONSE = 0x2;
    private static final EtherType ARP_TYPE = EtherType.ARP;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouteService routeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RouteAdminService routeAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LabelResourceAdminService labelAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LabelResourceService labelService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VpnInstanceService vpnInstanceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VpnPortService vpnPortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VirtualPortService virtualPortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected BasePortService basePortService;

    private final HostListener hostListener = new InnerHostListener();
    private final RouteListener routeListener = new InnerRouteListener();
    private final VpnPortListener vpnPortListner = new InnerVpnPortListener();
    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_ID);
        hostService.addListener(hostListener);
        routeService.addListener(routeListener);
        vpnPortService.addListener(vpnPortListner);
        labelAdminService
                .createGlobalPool(LabelResourceId.labelResourceId(1),
                                  LabelResourceId.labelResourceId(1000));
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        hostService.removeListener(hostListener);
        routeService.removeListener(routeListener);
        log.info("Stopped");
    }

    @Override
    public void onBgpEvpnRouteUpdate(EvpnRoute route) {
        log.info("bgp route update start {}", route);
        if (EvpnRoute.Source.LOCAL.equals(route.source())) {
            return;
        }
        // deal with public route and transfer to private route
        if (vpnInstanceService.getInstances().isEmpty()) {
            return;
        }
        vpnInstanceService.getInstances().forEach(vpnInstance -> {
            RouteTarget rt = routeService
                    .getRtByInstanceName(vpnInstance.vpnInstanceName());
            if (route.routeTarget().equals(rt)) {
                EvpnInstancePrefix evpnPrefix = EvpnInstancePrefix
                        .evpnPrefix(EvpnInstance.evpnMessage(
                                                             route.routeDistinguisher(),
                                                             route.routeTarget(),
                                                             vpnInstance
                                                                     .vpnInstanceName()),
                                    route.prefixMac(), route.prefixIp());
                EvpnInstanceNextHop evpnNextHop = EvpnInstanceNextHop
                        .evpnNextHop(route.ipNextHop(), route.label());
                EvpnInstanceRoute evpnPrivateRoute = new EvpnInstanceRoute(vpnInstance
                        .vpnInstanceName(), route.routeDistinguisher(), route
                                .routeTarget(), evpnPrefix, evpnNextHop);
                routeAdminService.update(Sets.newHashSet(evpnPrivateRoute));

            }
        });
        deviceService.getAvailableDevices(Device.Type.SWITCH)
                .forEach(device -> {
                    Set<Host> hosts = getHostsByVpn(device, route);
                    for (Host h : hosts) {
                        addArpFlows(device.id(), route, Objective.Operation.ADD,
                                    h);
                        ForwardingObjective.Builder objective = getMplsOutBuilder(device
                                .id(), route, h);
                        log.info("mpls out flows --> {}", h);
                        flowObjectiveService.forward(device.id(),
                                                     objective.add());
                    }
                });
    }

    private void addArpFlows(DeviceId deviceId, EvpnRoute route, Operation type,
                             Host host) {
        DriverHandler handler = driverService.createHandler(deviceId);
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(ARP_TYPE.ethType().toShort())
                .matchArpTpa(route.prefixIp())
                .matchInPort(host.location().port()).build();

        ExtensionTreatmentResolver resolver = handler
                .behaviour(ExtensionTreatmentResolver.class);
        ExtensionTreatment ethSrcToDst = resolver
                .getExtensionInstruction(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ETH_SRC_TO_DST
                        .type());
        ExtensionTreatment arpShaToTha = resolver
                .getExtensionInstruction(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ARP_SHA_TO_THA
                        .type());
        ExtensionTreatment arpSpaToTpa = resolver
                .getExtensionInstruction(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ARP_SPA_TO_TPA
                        .type());
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .extension(ethSrcToDst, deviceId).setEthSrc(route.prefixMac())
                .setArpOp(ARP_RESPONSE).extension(arpShaToTha, deviceId)
                .extension(arpSpaToTpa, deviceId).setArpSha(route.prefixMac())
                .setArpSpa(route.prefixIp()).setOutput(PortNumber.IN_PORT)
                .build();

        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(Flag.SPECIFIC)
                .withPriority(ARP_PRIORITY);
        if (type.equals(Objective.Operation.ADD)) {
            log.info("Route ARP Rules-->ADD");
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            log.info("Route ARP Rules-->REMOVE");
            flowObjectiveService.forward(deviceId, objective.remove());
        }
    }

    private Set<Host> getHostsByVpn(Device device, EvpnRoute route) {
        Set<Host> vpnHosts = Sets.newHashSet();
        Set<Host> hosts = hostService.getConnectedHosts(device.id());
        for (Host h : hosts) {
            String ifaceId = h.annotations().value("ifaceid");
            if (!vpnPortService.exists(VpnPortId.vpnPortId(ifaceId))) {
                continue;
            }
            VpnPort vpnPort = vpnPortService
                    .getPort(VpnPortId.vpnPortId(ifaceId));
            VpnInstanceId vpnInstanceId = vpnPort.vpnInstanceId();
            VpnInstance vpnInstance = vpnInstanceService
                    .getInstance(vpnInstanceId);
            if (route.routeDistinguisher()
                    .equals(vpnInstance.routeDistinguishers())) {
                vpnHosts.add(h);
            }
        }
        return vpnHosts;
    }

    @Override
    public void onBgpEvpnRouteDelete(EvpnRoute route) {
        log.info("bgp route delete start {}", route);
        if (EvpnRoute.Source.LOCAL.equals(route.source())) {
            return;
        }
        // deal with public route deleted and transfer to private route
        vpnInstanceService.getInstances().forEach(vpnInstance -> {
            RouteDistinguisher rd = routeService
                    .getRdByInstanceName(vpnInstance.vpnInstanceName());
            if (route.routeDistinguisher().equals(rd)) {
                EvpnInstancePrefix evpnPrefix = EvpnInstancePrefix
                        .evpnPrefix(EvpnInstance.evpnMessage(
                                                             route.routeDistinguisher(),
                                                             route.routeTarget(),
                                                             vpnInstance
                                                                     .vpnInstanceName()),
                                    route.prefixMac(), route.prefixIp());
                EvpnInstanceNextHop evpnNextHop = EvpnInstanceNextHop
                        .evpnNextHop(route.ipNextHop(), route.label());
                EvpnInstanceRoute evpnPrivateRoute = new EvpnInstanceRoute(vpnInstance
                        .vpnInstanceName(), route.routeDistinguisher(), route
                                .routeTarget(), evpnPrefix, evpnNextHop);
                routeAdminService.withdraw(Sets.newHashSet(evpnPrivateRoute));

            }
        });
        deviceService.getAvailableDevices(Device.Type.SWITCH)
                .forEach(device -> {
                    Set<Host> hosts = getHostsByVpn(device, route);
                    for (Host h : hosts) {
                        addArpFlows(device.id(), route,
                                    Objective.Operation.REMOVE, h);
                        ForwardingObjective.Builder objective = getMplsOutBuilder(device
                                .id(), route, h);
                        flowObjectiveService.forward(device.id(),
                                                     objective.remove());
                    }
                });
    }

    private ForwardingObjective.Builder getMplsOutBuilder(DeviceId deviceId,
                                                          EvpnRoute route,
                                                          Host h) {
        DriverHandler handler = driverService.createHandler(deviceId);
        ExtensionTreatmentResolver resolver = handler
                .behaviour(ExtensionTreatmentResolver.class);
        ExtensionTreatment treatment = resolver
                .getExtensionInstruction(NICIRA_SET_TUNNEL_DST.type());
        try {
            treatment.setPropertyValue("tunnelDst", route.ipNextHop());
        } catch (Exception e) {
            log.error("Failed to get extension instruction to set tunnel dst {}",
                      deviceId);
        }
        Builder builder = DefaultTrafficTreatment.builder();
        builder.extension(treatment, deviceId);
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(h.location().port()).matchEthSrc(h.mac())
                .matchEthDst(route.prefixMac()).build();

        TrafficTreatment build = builder.pushMpls()
                .setMpls(MplsLabel.mplsLabel(route.label().getLabel()))
                .setOutput(getTunnlePort(deviceId)).build();

        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(build).withSelector(selector)
                .fromApp(appId).withFlag(ForwardingObjective.Flag.SPECIFIC)
                .withPriority(60000);
        return objective;

    }

    private PortNumber getTunnlePort(DeviceId deviceId) {
        Iterable<Port> ports = deviceService.getPorts(deviceId);
        Collection<PortNumber> localTunnelPorts = VtnData
                .getLocalTunnelPorts(ports);
        if (localTunnelPorts.isEmpty()) {
            log.error("Can't find tunnel port in device {}", deviceId);
        }
        return localTunnelPorts.iterator().next();
    }

    @Override
    public void onHostDetected(Host host) {
        log.info("Host detected start {}", host);
        DeviceId deviceId = host.location().deviceId();
        if (!mastershipService.isLocalMaster(deviceId)) {
            return;
        }

        String ifaceId = host.annotations().value("ifaceid");
        if (ifaceId == null) {
            log.error("The ifaceId of Host is null");
            return;
        }
        // Get info from Gluon Shim
        if (!vpnPortService.exists(VpnPortId.vpnPortId(ifaceId))) {
            log.error("can't find vpnport {}", ifaceId);
            return;
        }
        VpnPort vpnPort = vpnPortService.getPort(VpnPortId.vpnPortId(ifaceId));
        VpnInstanceId vpnInstanceId = vpnPort.vpnInstanceId();
        if (!vpnInstanceService.exists(vpnInstanceId)) {
            log.error("Vpn Instance {} is not exist", vpnInstanceId);
            return;
        }
        VpnInstance vpnInstance = vpnInstanceService.getInstance(vpnInstanceId);
        RouteTarget rt = vpnInstance.routeTarget();
        Label privatelabel = applyLabel();
        // create private route and get label
        setPrivateRoute(host, vpnPort, privatelabel, Objective.Operation.ADD);
        // download flows
        setFlows(deviceId, host, privatelabel, rt, Objective.Operation.ADD);
    }

    private void setFlows(DeviceId deviceId, Host host, Label label,
                          RouteTarget rt, Operation type) {
        ForwardingObjective.Builder objective = getMplsInBuilder(deviceId, host,
                                                                 label);
        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(deviceId, objective.add());
        } else {
            flowObjectiveService.forward(deviceId, objective.remove());
        }
        // download remote flows
        Collection<Route> routes = routeService.getAllRoutes()
                .get(RouteTableType.VPN_PUBLIC);
        for (Route route : routes) {
            EvpnRoute evpnRouteTem = (EvpnRoute) route;
            Set<Host> macs = hostService
                    .getHostsByMac(evpnRouteTem.prefixMac());
            if (!macs.isEmpty() || !rt.equals(evpnRouteTem.routeTarget())) {
                continue;
            }
            addArpFlows(deviceId, evpnRouteTem, type, host);
            ForwardingObjective.Builder build = getMplsOutBuilder(deviceId,
                                                                  evpnRouteTem,
                                                                  host);
            if (type.equals(Objective.Operation.ADD)) {
                flowObjectiveService.forward(deviceId, build.add());
            } else {
                flowObjectiveService.forward(deviceId, build.remove());
            }
        }
    }

    private void setPrivateRoute(Host host, VpnPort vpnPort, Label privatelabel,
                                 Operation type) {
        DeviceId deviceId = host.location().deviceId();
        Device device = deviceService.getDevice(deviceId);
        VpnInstanceId vpnInstanceId = vpnPort.vpnInstanceId();
        VpnInstance vpnInstance = vpnInstanceService.getInstance(vpnInstanceId);
        RouteDistinguisher rd = vpnInstance.routeDistinguishers();
        RouteTarget rt = vpnInstance.routeTarget();
        EvpnInstanceName instanceName = vpnInstance.vpnInstanceName();
        String controllerIp = VtnData.getControllerIpOfSwitch(device);
        if (controllerIp == null) {
            log.error("Can't find controller of device: {}",
                      device.id().toString());
            return;
        }
        IpAddress ipAddress = IpAddress.valueOf(controllerIp);
        // create private route
        EvpnInstanceNextHop evpnNextHop = EvpnInstanceNextHop
                .evpnNextHop(ipAddress, privatelabel);
        EvpnInstancePrefix evpnPrefix = EvpnInstancePrefix
                .evpnPrefix(EvpnInstance.evpnMessage(rd, rt, instanceName),
                            host.mac(), host.ipAddresses().iterator().next()
                                    .getIp4Address());
        EvpnInstanceRoute evpnPrivateRoute = new EvpnInstanceRoute(instanceName,
                                                                   rd, rt,
                                                                   evpnPrefix,
                                                                   evpnNextHop);

        // change to public route
        EvpnRoute evpnRoute = new EvpnRoute(Source.LOCAL, host.mac(), host
                .ipAddresses().iterator().next()
                .getIp4Address(), Ip4Address.valueOf(ipAddress.toString()), rd,
                                            rt, privatelabel);
        if (type.equals(Objective.Operation.ADD)) {
            routeAdminService.update(Sets.newHashSet(evpnPrivateRoute));
            routeAdminService.update(Sets.newHashSet(evpnRoute));
            routeAdminService.sendEvpnMessage(EvpnRoute.OperationType.UPDATE,
                                              evpnRoute);
        } else {
            routeAdminService.withdraw(Sets.newHashSet(evpnPrivateRoute));
            routeAdminService.withdraw(Sets.newHashSet(evpnRoute));
            routeAdminService.sendEvpnMessage(EvpnRoute.OperationType.REMOVE,
                                              evpnRoute);
        }
    }

    private Label applyLabel() {
        Collection<LabelResource> privatelabels = labelService
                .applyFromGlobalPool(1);
        Label privatelabel = Label.label(0);
        if (!privatelabels.isEmpty()) {
            privatelabel = Label.label(Integer.parseInt(privatelabels.iterator()
                    .next().labelResourceId().toString()));
        }
        log.info("get private label {}", privatelabel);
        return privatelabel;
    }

    @Override
    public void onHostVanished(Host host) {
        log.info("Host vanished start {}", host);
        DeviceId deviceId = host.location().deviceId();
        if (!mastershipService.isLocalMaster(deviceId)) {
            return;
        }
        String ifaceId = host.annotations().value("ifaceid");
        if (ifaceId == null) {
            log.error("The ifaceId of Host is null");
            return;
        }
        // Get info from Gluon Shim
        VpnPort vpnPort = vpnPortService.getPort(VpnPortId.vpnPortId(ifaceId));
        VpnInstanceId vpnInstanceId = vpnPort.vpnInstanceId();
        if (!vpnInstanceService.exists(vpnInstanceId)) {
            log.error("Vpn Instance {} is not exist", vpnInstanceId);
            return;
        }
        VpnInstance vpnInstance = vpnInstanceService.getInstance(vpnInstanceId);

        Label label = releaseLabel(vpnInstance, host);
        // create private route and get label
        setPrivateRoute(host, vpnPort, label, Objective.Operation.REMOVE);
        // download flows
        setFlows(deviceId, host, label, vpnInstance.routeTarget(),
                 Objective.Operation.REMOVE);
    }

    private Label releaseLabel(VpnInstance vpnInstance, Host host) {
        RouteDistinguisher rd = vpnInstance.routeDistinguishers();
        RouteTarget rt = vpnInstance.routeTarget();
        EvpnInstanceName instanceName = vpnInstance.vpnInstanceName();
        EvpnInstancePrefix evpnPrefix = EvpnInstancePrefix
                .evpnPrefix(EvpnInstance.evpnMessage(rd, rt, instanceName),
                            host.mac(), host.ipAddresses().iterator().next()
                                    .getIp4Address());
        Map<EvpnInstancePrefix, EvpnInstanceNextHop> routeMap = routeService
                .getRouteMapByInstanceName(instanceName);
        EvpnInstanceNextHop evpnInstanceNextHop = routeMap.get(evpnPrefix);
        Label label = evpnInstanceNextHop.label();
        // delete private route and get label ,change to public route
        boolean isRelease = labelService.releaseToGlobalPool(Sets
                .newHashSet(LabelResourceId.labelResourceId(label.getLabel())));
        if (!isRelease) {
            log.error("Release resoure label {} failed", label.getLabel());
        }
        return label;
    }

    private ForwardingObjective.Builder getMplsInBuilder(DeviceId deviceId,
                                                         Host host,
                                                         Label label) {
        Builder builder = DefaultTrafficTreatment.builder();
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(getTunnlePort(deviceId))
                .matchEthType(EtherType.MPLS_UNICAST.ethType().toShort())
                .matchMplsBos(true)
                .matchMplsLabel(MplsLabel.mplsLabel(label.getLabel())).build();
        TrafficTreatment treatment = builder.popMpls(EtherType.IPV4.ethType())
                .setOutput(host.location().port()).build();

        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(treatment).withSelector(selector)
                .fromApp(appId).withFlag(ForwardingObjective.Flag.SPECIFIC)
                .withPriority(60000);
        return objective;

    }

    private class InnerHostListener implements HostListener {

        @Override
        public void event(HostEvent event) {
            Host host = event.subject();
            if (HostEvent.Type.HOST_ADDED == event.type()) {
                onHostDetected(host);
            } else if (HostEvent.Type.HOST_REMOVED == event.type()) {
                onHostVanished(host);
            }
        }

    }

    private class InnerRouteListener implements RouteListener {

        @Override
        public void event(RouteEvent event) {
            if (!(event.subject() instanceof EvpnRoute)) {
                return;
            }
            EvpnRoute route = (EvpnRoute) event.subject();
            if (RouteEvent.Type.ROUTE_ADDED == event.type()) {
                onBgpEvpnRouteUpdate(route);
            } else if (RouteEvent.Type.ROUTE_REMOVED == event.type()) {
                onBgpEvpnRouteDelete(route);
            }
        }
    }

    private class InnerVpnPortListener implements VpnPortListener {

        @Override
        public void event(VpnPortEvent event) {
            VpnPort vpnPort = event.subject();
            if (VpnPortEvent.Type.VPNPORT_DELETE == event.type()) {
                onVpnPortDelete(vpnPort);
            } else if (VpnPortEvent.Type.VPNPORT_SET == event.type()) {
                onVpnPortSet(vpnPort);
            }
        }

    }

    private void onVpnPortDelete(VpnPort vpnPort) {
        // delete the flows of this vpn
        hostService.getHosts().forEach(host -> {
            VpnPortId vpnPortId = vpnPort.id();
            VpnInstanceId vpnInstanceId = vpnPort.vpnInstanceId();
            if (!vpnInstanceService.exists(vpnInstanceId)) {
                log.error("Vpn Instance {} is not exist", vpnInstanceId);
                return;
            }
            VpnInstance vpnInstance = vpnInstanceService
                    .getInstance(vpnInstanceId);
            RouteTarget rt = vpnInstance.routeTarget();
            if (vpnPortId.vpnPortId()
                    .equals(host.annotations().value("ifaceid"))) {
                log.info("on vpn port unbind");
                DeviceId deviceId = host.location().deviceId();
                Label label = releaseLabel(vpnInstance, host);
                // create private route and get label
                setPrivateRoute(host, vpnPort, label,
                                Objective.Operation.REMOVE);
                // download flows
                setFlows(deviceId, host, label, rt, Objective.Operation.REMOVE);
            }
        });
    }

    private void onVpnPortSet(VpnPort vpnPort) {
        // delete the flows of this vpn
        hostService.getHosts().forEach(host -> {
            VpnPortId vpnPortId = vpnPort.id();
            VpnInstanceId vpnInstanceId = vpnPort.vpnInstanceId();
            if (!vpnInstanceService.exists(vpnInstanceId)) {
                log.error("Vpn Instance {} is not exist", vpnInstanceId);
                return;
            }
            VpnInstance vpnInstance = vpnInstanceService
                    .getInstance(vpnInstanceId);
            RouteTarget rt = vpnInstance.routeTarget();
            if (vpnPortId.vpnPortId()
                    .equals(host.annotations().value("ifaceid"))) {
                log.info("on vpn port bind");
                DeviceId deviceId = host.location().deviceId();
                Label privatelabel = applyLabel();
                // create private route and get label
                setPrivateRoute(host, vpnPort, privatelabel,
                                Objective.Operation.ADD);
                // download flows
                setFlows(deviceId, host, privatelabel, rt,
                         Objective.Operation.ADD);
            }
        });
    }
}
