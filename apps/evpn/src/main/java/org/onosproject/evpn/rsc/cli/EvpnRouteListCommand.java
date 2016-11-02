package org.onosproject.evpn.rsc.cli;

import java.util.Collection;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.evpnrouting.EvpnRoute;
import org.onosproject.incubator.net.evpnrouting.EvpnRouteService;

@Command(scope = "onos", name = "evpn-routes", description = "Lists all routes in the route store")
public class EvpnRouteListCommand extends AbstractShellCommand {
    private static final String FORMAT_ROUTE = "   %-18s %-18s %-18s";

    @Override
    protected void execute() {
        EvpnRouteService service = AbstractShellCommand
                .get(EvpnRouteService.class);
        Collection<EvpnRoute> routes = service.getAllRoutes();
        routes.forEach(r -> print(FORMAT_ROUTE, r.prefixMac(), r.prefixIp(),  r.nextHop()));
    }

}
