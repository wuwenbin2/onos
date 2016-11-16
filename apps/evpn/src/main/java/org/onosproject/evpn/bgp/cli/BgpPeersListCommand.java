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
package org.onosproject.evpn.bgp.cli;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.karaf.shell.commands.Command;
import org.jboss.netty.channel.Channel;
import org.onlab.packet.IpAddress;
import org.onosproject.bgp.controller.BgpCfg;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.bgp.controller.BgpRouteService;
import org.onosproject.cli.AbstractShellCommand;

/**
 * Command to show the BGP neighbors.
 */
@Command(scope = "onos", name = "bgp-peers", description = "Lists the BGP peers")
public class BgpPeersListCommand extends AbstractShellCommand {

    private static final String FORMAT_HEADER_1 = "BGP peers of local node: = %s";
    private static final String FORMAT_HEADER_2 = "   Ip            IsConnected";
    private static final String FORMAT_CONTENT = "  %-18s %-15s";

    @Override
    protected void execute() {
        BgpRouteService service = AbstractShellCommand
                .get(BgpRouteService.class);
        BgpController controller = service.getController();
        BgpCfg cfg = controller.getConfig();
        String routerid = controller.getConfig().getRouterId();
        print(FORMAT_HEADER_1, routerid);
        print(FORMAT_HEADER_2);
        controller.getPeers().forEach(peer -> {
            print(FORMAT_CONTENT, getIpAddress(peer.getChannel()),
                  cfg.isPeerConnected(routerid));
        });

    }

    public IpAddress getIpAddress(Channel channel) {

        final SocketAddress address = channel.getRemoteAddress();
        if (address instanceof InetSocketAddress) {
            final InetSocketAddress inetAddress = (InetSocketAddress) address;
            return IpAddress.valueOf(inetAddress.getAddress());

        }
        return null;
    }

}
