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
import org.onosproject.evpn.rsc.vpninstance.VpnInstanceService;
import org.onosproject.evpn.rsc.vpnport.VpnPortService;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstance;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceName;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceNextHop;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstancePrefix;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceRoute;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceRouteAdminService;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceRouteService;
import org.onosproject.incubator.net.evpnrouting.EvpnRoute;
import org.onosproject.incubator.net.evpnrouting.EvpnRoute.Source;
import org.onosproject.incubator.net.evpnrouting.EvpnRouteAdminService;
import org.onosproject.incubator.net.evpnrouting.EvpnRouteEvent;
import org.onosproject.incubator.net.evpnrouting.EvpnRouteListener;
import org.onosproject.incubator.net.evpnrouting.EvpnRouteService;
import org.onosproject.incubator.net.evpnrouting.Label;
import org.onosproject.incubator.net.evpnrouting.RouteDistinguisher;
import org.onosproject.incubator.net.evpnrouting.RouteTarget;
import org.onosproject.incubator.net.resource.label.LabelResource;
import org.onosproject.incubator.net.resource.label.LabelResourceAdminService;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.resource.label.LabelResourceService;
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
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.vtn.util.VtnData;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

@Component(immediate = true)
@Service
public class EvpnManager implements EvpnService {
    private final Logger log = getLogger(getClass());
    private static final String APP_ID = "org.onosproject.app.evpn";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EvpnRouteService routeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EvpnInstanceRouteService privateRouteService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EvpnInstanceRouteAdminService privateRouteAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EvpnRouteAdminService routeAdminService;

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

    private final HostListener hostListener = new InnerHostListener();
    private final EvpnRouteListener routeListener = new InnerRouteListener();

    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_ID);
        hostService.addListener(hostListener);
        routeService.addListener(routeListener);
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
        if (EvpnRoute.Source.LOCAL.equals(route.source())) {
            return;
        }
        // deal with public route and transfer to private route
        if (vpnInstanceService.getInstances().isEmpty()) {
            return;
        }
        vpnInstanceService.getInstances().forEach(vpnInstance -> {
            RouteTarget rt = privateRouteService
                    .getRtByInstanceName(vpnInstance.vpnInstanceName());
            if (route.routeTarget().equals(rt)) {
                EvpnInstancePrefix evpnPrefix = EvpnInstancePrefix
                        .evpnPrefix(EvpnInstance.evpnMessage(
                                                             route.routeDistinguisher(),
                                                             route.routeTarget(),
                                                             vpnInstance
                                                                     .vpnInstanceName()),
                                    route.prefix());
                EvpnInstanceNextHop evpnNextHop = EvpnInstanceNextHop
                        .evpnNextHop(route.nextHop(), route.label());
                EvpnInstanceRoute evpnPrivateRoute = new EvpnInstanceRoute(vpnInstance
                        .vpnInstanceName(), route.routeDistinguisher(), route
                                .routeTarget(), evpnPrefix, evpnNextHop);
                privateRouteAdminService
                        .updateEvpnRoute(Sets.newHashSet(evpnPrivateRoute));

            }
        });
        deviceService.getAvailableDevices(Device.Type.SWITCH)
                .forEach(device -> {
                    ForwardingObjective.Builder objective = getMplsOutBuilder(device,
                                                                              route);
                    flowObjectiveService.forward(device.id(), objective.add());
                });
    }

    @Override
    public void onBgpEvpnRouteDelete(EvpnRoute route) {
        if (EvpnRoute.Source.LOCAL.equals(route.source())) {
            return;
        }
        // deal with public route deleted and transfer to private route
        vpnInstanceService.getInstances().forEach(vpnInstance -> {
            RouteTarget rt = privateRouteService
                    .getRtByInstanceName(vpnInstance.vpnInstanceName());
            if (route.routeTarget().equals(rt)) {
                EvpnInstancePrefix evpnPrefix = EvpnInstancePrefix
                        .evpnPrefix(EvpnInstance.evpnMessage(
                                                             route.routeDistinguisher(),
                                                             route.routeTarget(),
                                                             vpnInstance
                                                                     .vpnInstanceName()),
                                    route.prefix());
                EvpnInstanceNextHop evpnNextHop = EvpnInstanceNextHop
                        .evpnNextHop(route.nextHop(), route.label());
                EvpnInstanceRoute evpnPrivateRoute = new EvpnInstanceRoute(vpnInstance
                        .vpnInstanceName(), route.routeDistinguisher(), route
                                .routeTarget(), evpnPrefix, evpnNextHop);
                privateRouteAdminService
                        .withdrawEvpnRoute(Sets.newHashSet(evpnPrivateRoute));

            }
        });
        deviceService.getAvailableDevices(Device.Type.SWITCH)
                .forEach(device -> {
                    ForwardingObjective.Builder objective = getMplsOutBuilder(device,
                                                                              route);
                    flowObjectiveService.forward(device.id(),
                                                 objective.remove());
                });
    }

    private ForwardingObjective.Builder getMplsOutBuilder(Device device,
                                                          EvpnRoute route) {
        DeviceId deviceId = device.id();
        DriverHandler handler = driverService.createHandler(deviceId);
        ExtensionTreatmentResolver resolver = handler
                .behaviour(ExtensionTreatmentResolver.class);
        ExtensionTreatment treatment = resolver
                .getExtensionInstruction(NICIRA_SET_TUNNEL_DST.type());
        try {
            treatment.setPropertyValue("tunnelDst", route.nextHop());
        } catch (Exception e) {
            log.error("Failed to get extension instruction to set tunnel dst {}",
                      deviceId);
        }
        Builder builder = DefaultTrafficTreatment.builder();
        builder.extension(treatment, deviceId);
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(EtherType.IPV4.ethType().toShort())
                .matchEthDst(route.prefix()).build();

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
        DeviceId deviceId = host.location().deviceId();
        Device device = deviceService.getDevice(deviceId);
        if (!mastershipService.isLocalMaster(deviceId)) {
            return;
        }
        String controllerIp = VtnData.getControllerIpOfSwitch(device);
        if (controllerIp == null) {
            log.error("Can't find controller of device: {}",
                      device.id().toString());
            return;
        }
        IpAddress ipAddress = IpAddress.valueOf(controllerIp);
        String ifaceId = host.annotations().value("ifaceid");
        if (ifaceId == null) {
            log.error("The ifaceId of Host is null");
            return;
        }
        // Get info from Gluon Shim
        VpnPort vpnPort = vpnPortService.getPort(VpnPortId.vpnPortId(ifaceId));
        VpnInstanceId vpnInstanceId = vpnPort.vpnInstanceId();
        VpnInstance vpnInstance = vpnInstanceService.getInstance(vpnInstanceId);
        RouteDistinguisher rd = vpnInstance.routeDistinguishers();
        RouteTarget rt = vpnInstance.routeTarget();
        EvpnInstanceName instanceName = vpnInstance.vpnInstanceName();

        // create private route and get label
        Collection<LabelResource> privatelabels = labelService
                .applyFromGlobalPool(1);
        Label privatelabel = Label.label(0);
        if (!privatelabels.isEmpty()) {
            privatelabel = Label.label(Integer.parseInt(privatelabels.iterator()
                    .next().labelResourceId().toString()));
        }
        // create private route
        EvpnInstanceNextHop evpnNextHop = EvpnInstanceNextHop
                .evpnNextHop(ipAddress, privatelabel);
        EvpnInstancePrefix evpnPrefix = EvpnInstancePrefix
                .evpnPrefix(EvpnInstance.evpnMessage(rd, rt, instanceName),
                            host.mac());
        EvpnInstanceRoute evpnPrivateRoute = new EvpnInstanceRoute(instanceName,
                                                                   rd, rt,
                                                                   evpnPrefix,
                                                                   evpnNextHop);
        privateRouteAdminService
                .updateEvpnRoute(Sets.newHashSet(evpnPrivateRoute));
        // change to public route
        EvpnRoute evpnRoute = new EvpnRoute(Source.LOCAL, host.mac(),
                                            Ip4Address.valueOf(ipAddress
                                                    .toString()),
                                            rd, rt, privatelabel);
        routeAdminService.updateEvpnRoute(Sets.newHashSet(evpnRoute));
        // download flows
        ForwardingObjective.Builder objective = getMplsInBuilder(device, host,
                                                                 privatelabel);
        flowObjectiveService.forward(device.id(), objective.add());
    }

    @Override
    public void onHostVanished(Host host) {
        DeviceId deviceId = host.location().deviceId();
        Device device = deviceService.getDevice(deviceId);
        if (!mastershipService.isLocalMaster(deviceId)) {
            return;
        }
        String controllerIp = VtnData.getControllerIpOfSwitch(device);
        if (controllerIp == null) {
            log.error("Can't find controller of device: {}",
                      device.id().toString());
            return;
        }
        IpAddress ipAddress = IpAddress.valueOf(controllerIp);
        String ifaceId = host.annotations().value("ifaceid");
        if (ifaceId == null) {
            log.error("The ifaceId of Host is null");
            return;
        }
        // Get info from Gluon Shim
        VpnPort vpnPort = vpnPortService.getPort(VpnPortId.vpnPortId(ifaceId));
        VpnInstanceId vpnInstanceId = vpnPort.vpnInstanceId();
        VpnInstance vpnInstance = vpnInstanceService.getInstance(vpnInstanceId);
        RouteDistinguisher rd = vpnInstance.routeDistinguishers();
        RouteTarget rt = vpnInstance.routeTarget();
        EvpnInstanceName instanceName = vpnInstance.vpnInstanceName();
        Map<EvpnInstancePrefix, EvpnInstanceNextHop> routeMap = privateRouteService
                .getRouteMapByInstanceName(instanceName);
        EvpnInstancePrefix evpnPrefix = EvpnInstancePrefix
                .evpnPrefix(EvpnInstance.evpnMessage(rd, rt, instanceName),
                            host.mac());
        EvpnInstanceNextHop evpnInstanceNextHop = routeMap.get(evpnPrefix);
        Label label = evpnInstanceNextHop.label();
        // delete private route and get label ,change to public route
        boolean isRelease = labelService.releaseToGlobalPool(Sets
                .newHashSet(LabelResourceId.labelResourceId(label.getLabel())));
        if (!isRelease) {
            log.error("Release resoure label {} failed", label.getLabel());
        }
        EvpnInstanceRoute evpnPrivateRoute = new EvpnInstanceRoute(instanceName,
                                                                   rd, rt,
                                                                   evpnPrefix,
                                                                   evpnInstanceNextHop);
        privateRouteAdminService
                .withdrawEvpnRoute(Sets.newHashSet(evpnPrivateRoute));
        EvpnRoute evpnRoute = new EvpnRoute(Source.LOCAL, host.mac(),
                                            Ip4Address.valueOf(ipAddress
                                                    .toString()),
                                            rd, rt, label);
        routeAdminService.withdrawEvpnRoute(Sets.newHashSet(evpnRoute));
        // download flows
        ForwardingObjective.Builder objective = getMplsInBuilder(device, host,
                                                                 label);
        flowObjectiveService.forward(device.id(), objective.remove());
    }

    private ForwardingObjective.Builder getMplsInBuilder(Device device,
                                                         Host host,
                                                         Label label) {
        DeviceId deviceId = device.id();
        Builder builder = DefaultTrafficTreatment.builder();
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(getTunnlePort(deviceId))
                .matchEthType(EtherType.MPLS_UNICAST.ethType().toShort())
                .matchMplsBos(true)
                .matchMplsLabel(MplsLabel.mplsLabel(label.getLabel())).build();
        TrafficTreatment treatment = builder.popMpls()
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

    private class InnerRouteListener implements EvpnRouteListener {

        @Override
        public void event(EvpnRouteEvent event) {
            EvpnRoute route = event.subject();
            if (EvpnRouteEvent.Type.ROUTE_ADDED == event.type()) {
                onBgpEvpnRouteUpdate(route);
            } else if (EvpnRouteEvent.Type.ROUTE_REMOVED == event.type()) {
                onBgpEvpnRouteDelete(route);
            }
        }
    }
}
