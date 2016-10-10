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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.EthType.EtherType;
import org.onlab.packet.MplsLabel;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.evpn.manager.EvpnService;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstance;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceNextHop;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstancePrefix;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceRoute;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceRouteAdminService;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceRouteService;
import org.onosproject.incubator.net.evpnrouting.EvpnRoute;
import org.onosproject.incubator.net.evpnrouting.EvpnRouteEvent;
import org.onosproject.incubator.net.evpnrouting.EvpnRouteListener;
import org.onosproject.incubator.net.evpnrouting.EvpnRouteService;
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
import org.onosproject.vtnrsc.VirtualPortId;
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
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LabelResourceAdminService labelAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LabelResourceService labelService;

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
        // deal when private route add getEvpnInstances()
        privateRouteService.getEvpnInstanceNames().forEach(evpnName -> {
            RouteTarget rt = privateRouteService.getRtByInstanceName(evpnName);
            if (route.routeTarget().equals(rt)) {
                EvpnInstanceRoute evpnPrivateRoute = new EvpnInstanceRoute(evpnName,
                                                                           route.routeDistinguisher(),
                                                                           route.routeTarget(),
                                                                           EvpnInstancePrefix
                                                                                   .evpnPrefix(EvpnInstance
                                                                                           .evpnMessage(route
                                                                                                   .routeDistinguisher(),
                                                                                                        route.routeTarget(),
                                                                                                        evpnName),
                                                                                               route.prefix()),
                                                                           EvpnInstanceNextHop
                                                                                   .evpnNextHop(route
                                                                                           .nextHop(),
                                                                                                route.label()));
                privateRouteAdminService
                        .updateEvpnRoute(Sets.newHashSet(evpnPrivateRoute));

            }
        });
        deviceService.getAvailableDevices(Device.Type.SWITCH)
                .forEach(device -> {
                    ForwardingObjective.Builder objective = getMplsBuilder(device,
                                                                           route);
                    flowObjectiveService.forward(device.id(), objective.add());
                });
    }

    @Override
    public void onBgpEvpnRouteDelete(EvpnRoute route) {
        deviceService.getAvailableDevices(Device.Type.SWITCH)
                .forEach(device -> {
                    ForwardingObjective.Builder objective = getMplsBuilder(device,
                                                                           route);
                    flowObjectiveService.forward(device.id(),
                                                 objective.remove());
                });
    }

    private ForwardingObjective.Builder getMplsBuilder(Device device,
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
        Iterable<Port> ports = deviceService.getPorts(deviceId);
        Collection<PortNumber> localTunnelPorts = VtnData
                .getLocalTunnelPorts(ports);
        TrafficTreatment build = builder.pushMpls()
                .setMpls(MplsLabel.mplsLabel(route.label().getLabel()))
                .setOutput(localTunnelPorts.iterator().next()).build();

        ForwardingObjective.Builder objective = DefaultForwardingObjective
                .builder().withTreatment(build).withSelector(selector)
                .fromApp(appId).withFlag(ForwardingObjective.Flag.SPECIFIC)
                .withPriority(60000);
        return objective;

    }

    @Override
    public void onHostDetected(Host host) {
        DeviceId deviceId = host.location().deviceId();
        if (!mastershipService.isLocalMaster(deviceId)) {
            return;
        }
        String ifaceId = host.annotations().value("ifaceid");
        if (ifaceId == null) {
            log.error("The ifaceId of Host is null");
            return;
        }
        VirtualPortId virtualPortId = VirtualPortId.portId(ifaceId);
        // Get info from Gluon Shim

        // create private route and get label ,change to public route
        Collection<LabelResource> privatelabel = labelService
                .applyFromGlobalPool(1);
        EvpnInstanceRoute evpnInstanceRoute = new EvpnInstanceRoute(null, null,
                                                                    null, null,
                                                                    null);
        // download flows
    }

    @Override
    public void onHostVanished(Host host) {
        // TODO Auto-generated method stub
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
