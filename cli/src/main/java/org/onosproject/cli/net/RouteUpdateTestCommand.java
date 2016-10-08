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

package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.evpnrouting.EvpnRoute;
import org.onosproject.incubator.net.evpnrouting.EvpnRoute.Source;
import org.onosproject.incubator.net.evpnrouting.EvpnRouteAdminService;
import org.onosproject.incubator.net.evpnrouting.Label;
import org.onosproject.incubator.net.evpnrouting.RouteDistinguisher;
import org.onosproject.incubator.net.evpnrouting.RouteTarget;

/**
 * Command to add a route to the routing table.
 */
@Command(scope = "onos", name = "evpn-route-add", description = "Adds a route to the route table")
public class RouteUpdateTestCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        EvpnRouteAdminService service = AbstractShellCommand
                .get(EvpnRouteAdminService.class);

        Source source = Source.BGP;
        MacAddress macAddress = MacAddress.valueOf("e4:68:a3:4e:dc:01");
        Ip4Address nextHop = Ip4Address.valueOf("10.1.1.1");
        RouteDistinguisher rd = RouteDistinguisher.routeDistinguisher("100:1");
        RouteTarget rt = RouteTarget.routeTarget("100:1");
        Label label = Label.label(100);
        EvpnRoute route = new EvpnRoute(source, macAddress, nextHop, rd, rt,
                                        label);
        service.sendEvpnMessage(route);
    }

}
