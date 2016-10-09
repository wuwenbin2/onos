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
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.evpn.rsc.baseport.BasePortService;
import org.onosproject.vtnrsc.VirtualPort;

/**
 * Supports for create a floating IP.
 */
@Command(scope = "onos", name = "etcd-baseport-monitor", description = "Supports for start etcd monitor")
public class EtcdMonitorStartCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "ipAddress", description = "The ip address of etcd server",
            required = false, multiValued = false)
    String ipAddress = null;

    @Option(name = "-t", aliases = "--start", description = "Start monitor of base port",
            required = false, multiValued = false)
    boolean start = false;

    @Option(name = "-o", aliases = "--stop", description = "Stop monitor of base port",
            required = false, multiValued = false)
    boolean stop = false;

    @Option(name = "-q", aliases = "--query", description = "query base port data",
            required = false, multiValued = false)
    boolean query = false;

    private static final String FMT = "virtualPortId=%s, networkId=%s, name=%s,"
            + " tenantId=%s, deviceId=%s, adminStateUp=%s, state=%s,"
            + " macAddress=%s, deviceOwner=%s, fixedIp=%s, bindingHostId=%s,"
            + " bindingvnicType=%s, bindingvifType=%s, bindingvnicDetails=%s,"
            + " allowedAddress=%s, securityGroups=%s";

    @Override
    protected void execute() {
        BasePortService service = get(BasePortService.class);
        try {
            if (ipAddress != null) {
                String url = "http://" + ipAddress + ":2379";
                service.updateEtcdUrl(url);
            }
            if (start) {
                service.etcdMonitor();
            }

            if (query) {
                Collection<VirtualPort> ports = service.getPorts();
                printPorts(ports);
            }
        } catch (Exception e) {
            print(null, e.getMessage());
        }
    }

    private void printPorts(Collection<VirtualPort> ports) {
        for (VirtualPort port : ports) {
            printPort(port);
        }
    }

    private void printPort(VirtualPort port) {
        print(FMT, port.portId(), port.networkId(), port.name(),
              port.tenantId(), port.deviceId(), port.adminStateUp(),
              port.state(), port.macAddress(), port.deviceOwner(),
              port.fixedIps(), port.bindingHostId(), port.bindingVnicType(),
              port.bindingVifType(), port.bindingVifDetails(),
              port.allowedAddressPairs(), port.securityGroups());
    }
}
