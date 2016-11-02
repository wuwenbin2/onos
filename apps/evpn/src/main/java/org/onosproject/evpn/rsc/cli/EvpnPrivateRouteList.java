package org.onosproject.evpn.rsc.cli;

import java.util.Collection;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceRoute;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceRouteService;

@Command(scope = "onos", name = "evpn-private-routes", description = "Lists all routes in the route store")
public class EvpnPrivateRouteList extends AbstractShellCommand {
    private static final String FORMAT_ROUTE = "   %-18s %-18s %-15s";

    @Override
    protected void execute() {
        EvpnInstanceRouteService service = AbstractShellCommand
                .get(EvpnInstanceRouteService.class);
        Collection<EvpnInstanceRoute> routes = service.getAllRoutes();
        routes.forEach(r -> print(FORMAT_ROUTE, r.evpnInstanceName(),
                                  r.prefix(), r.nextHop()));

    }

}
