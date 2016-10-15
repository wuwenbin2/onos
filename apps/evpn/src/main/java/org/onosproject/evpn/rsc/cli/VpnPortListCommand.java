package org.onosproject.evpn.rsc.cli;

import java.util.Collection;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.evpn.rsc.VpnPort;
import org.onosproject.evpn.rsc.vpnport.VpnPortService;

@Command(scope = "onos", name = "evpn-vpnports", description = "Supports for start etcd monitor")
public class VpnPortListCommand extends AbstractShellCommand {
    private static final String FORMAT_ROUTE = "   %-32s %-18s";

    @Override
    protected void execute() {
        VpnPortService portservice = get(VpnPortService.class);
        Collection<VpnPort> ports = portservice.getPorts();
        ports.forEach(p -> {
            print(FORMAT_ROUTE, p.id(), p.vpnInstanceId());
        });
    }

}
