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
package org.onosproject.evpn.rsc.cli;

import java.util.Collection;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.evpn.rsc.EtcdMonitor;
import org.onosproject.evpn.rsc.VpnInstance;
import org.onosproject.evpn.rsc.VpnPort;
import org.onosproject.evpn.rsc.baseport.BasePortService;
import org.onosproject.evpn.rsc.vpninstance.VpnInstanceService;
import org.onosproject.evpn.rsc.vpnport.VpnPortService;
import org.onosproject.vtnrsc.VirtualPort;

/**
 * Supports for create a floating IP.
 */
@Command(scope = "onos", name = "etcd-monitor", description = "Supports for start etcd monitor")
public class EtcdMonitorStartCommand extends AbstractShellCommand {

    @Option(name = "-i", aliases = "--ip", description = "Etcd server ip address",
            required = false, multiValued = false)
    String ipAddress = null;

    @Option(name = "-t", aliases = "--target", description = "Etcd monitor process : baseport; vpninstance; vpnport",
            required = false, multiValued = false)
    String target = null;

    @Option(name = "-q", aliases = "--query", description = "query base port data : true; false",
            required = false, multiValued = false)
    String query = null;

    private static final String BASEPORTFMT = "virtualPortId=%s, networkId=%s, name=%s,"
            + " tenantId=%s, deviceId=%s, adminStateUp=%s, state=%s,"
            + " macAddress=%s, deviceOwner=%s, fixedIp=%s, bindingHostId=%s,"
            + " bindingvnicType=%s, bindingvifType=%s, bindingvnicDetails=%s,"
            + " allowedAddress=%s, securityGroups=%s";
    private static final String VPNINSTANCEFMT = "Id=%s, description=%s,"
            + " name=%s, routeDistinguisher=%s, routeTarget=%s";
    private static final String VPNPORTFMT = "Id=%s, vpnInstanceId=%s";

    @Override
    protected void execute() {
        try {
            if (ipAddress != null) {
                String url = "http://" + ipAddress + ":2379";
                EtcdMonitor etcdMonitor = new EtcdMonitor(url);
                etcdMonitor.etcdMonitor();
            }
            if (query != null) {
                if (target.equals("baseport")) {
                    BasePortService service = get(BasePortService.class);
                    Collection<VirtualPort> ports = service.getPorts();
                    printBasePorts(ports);
                } else if (target.equals("vpninstance")) {
                    VpnInstanceService service = get(VpnInstanceService.class);
                    Collection<VpnInstance> vpnInstances = service
                            .getInstances();
                    printVpnInstances(vpnInstances);
                } else if (target.equals("vpnport")) {
                    VpnPortService service = get(VpnPortService.class);
                    Collection<VpnPort> vpnPorts = service.getPorts();
                    printVpnPorts(vpnPorts);
                }

            }
        } catch (Exception e) {
            print(null, e.getMessage());
        }
    }

    private void printBasePorts(Collection<VirtualPort> ports) {
        for (VirtualPort port : ports) {
            printBasePort(port);
        }
    }

    private void printVpnInstances(Collection<VpnInstance> vpnInstances) {
        for (VpnInstance vpnInstance : vpnInstances) {
            printVpnInstance(vpnInstance);
        }
    }

    private void printVpnPorts(Collection<VpnPort> vpnPorts) {
        for (VpnPort vpnport : vpnPorts) {
            printVpnPort(vpnport);
        }
    }

    private void printBasePort(VirtualPort port) {
        print(BASEPORTFMT, port.portId(), port.networkId(), port.name(),
              port.tenantId(), port.deviceId(), port.adminStateUp(),
              port.state(), port.macAddress(), port.deviceOwner(),
              port.fixedIps(), port.bindingHostId(), port.bindingVnicType(),
              port.bindingVifType(), port.bindingVifDetails(),
              port.allowedAddressPairs(), port.securityGroups());
    }

    private void printVpnInstance(VpnInstance vpnInstance) {
        print(VPNINSTANCEFMT, vpnInstance.id(), vpnInstance.description(),
              vpnInstance.vpnInstanceName(), vpnInstance.routeDistinguishers(),
              vpnInstance.routeTarget());
    }

    private void printVpnPort(VpnPort vpnPort) {
        print(VPNPORTFMT, vpnPort.id(), vpnPort.vpnInstanceId());
    }
}
