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
import java.util.HashMap;
import java.util.Map;

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
import org.onosproject.evpn.rsc.EvpnInstance;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnMessage;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnName;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnPrivateNextHop;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnPrivatePrefix;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnPrivateRoute;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnPrivateRouteAdminService;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnPrivateRouteService;
import org.onosproject.incubator.net.evpnrouting.EvpnRoute;
import org.onosproject.incubator.net.evpnrouting.EvpnRouteEvent;
import org.onosproject.incubator.net.evpnrouting.EvpnRouteListener;
import org.onosproject.incubator.net.evpnrouting.EvpnRouteService;
import org.onosproject.mastership.MastershipService;
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
    protected EvpnPrivateRouteService privateRouteService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EvpnPrivateRouteAdminService privateRouteAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;
    private final HostListener hostListener = new InnerHostListener();
    private final EvpnRouteListener routeListener = new InnerRouteListener();

    private Map<String, EvpnInstance> evpnStore;
    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_ID);
        hostService.addListener(hostListener);
        routeService.addListener(routeListener);
        evpnStore = new HashMap<String, EvpnInstance>();
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
        // deal when private route add
        EvpnPrivateRoute evpnPrivateRoute = new EvpnPrivateRoute(EvpnPrivatePrefix
                .evpnPrefix(EvpnMessage.evpnMessage(route.routeDistinguisher(),
                                                    route.routeTarget(),
                                                    EvpnName.evpnName("vpn1")),
                            route.prefix()), EvpnPrivateNextHop
                                    .evpnNextHop(route.nextHop(),
                                                 route.label()));
        privateRouteAdminService
                .updateEvpnRoute(Sets.newHashSet(evpnPrivateRoute));
       // download flows
        deviceService.getAvailableDevices().forEach(device -> {
            DeviceId deviceId = device.id();
            DriverHandler handler = driverService.createHandler(deviceId);
            ExtensionTreatmentResolver resolver = handler
                    .behaviour(ExtensionTreatmentResolver.class);
            ExtensionTreatment treatment = resolver
                    .getExtensionInstruction(NICIRA_SET_TUNNEL_DST.type());
            try {
                treatment.setPropertyValue("tunnelDst", "10.0.0.2");
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
            Collection<PortNumber> localTunnelPorts = VtnData.getLocalTunnelPorts(ports);
            TrafficTreatment build = builder
                    .pushMpls()
                    .setMpls(MplsLabel.mplsLabel(route.label().getLabel()))
                    .setOutput(localTunnelPorts.iterator().next()).build();

            ForwardingObjective.Builder objective = DefaultForwardingObjective
                    .builder().withTreatment(build).withSelector(selector)
                    .fromApp(appId).withFlag(ForwardingObjective.Flag.SPECIFIC)
                    .withPriority(6000);
            flowObjectiveService.forward(device.id(), objective.add());
        });
    }

    @Override
    public void onBgpEvpnRouteDelete(EvpnRoute route) {
        // TODO Auto-generated method stub
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
        EvpnInstance evpnInstance = new EvpnInstance("TestVPN", "123", null,
                                                     null, null, null);
        evpnStore.put("123", evpnInstance);
        // create private route and get label ,change to public route
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
