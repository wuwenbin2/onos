package org.onosproject.evpn.rsc.cli;

import java.util.Collection;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.routing.EvpnInstanceRoute;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteService;
import org.onosproject.incubator.net.routing.RouteTableType;

@Command(scope = "onos", name = "evpn-private-routes", description = "Lists all routes in the route store")
public class EvpnPrivateRouteList extends AbstractShellCommand {
    private static final String FORMAT_ROUTE = "   %-18s %-18s %-15s";

    @Override
    protected void execute() {

        RouteService service = AbstractShellCommand.get(RouteService.class);

        Collection<Route> evpnRoutes = service.getAllRoutes()
                .get(RouteTableType.VPN_PRIVATE);
        evpnRoutes.forEach(r -> {
            EvpnInstanceRoute evpnInstanceRoute = (EvpnInstanceRoute) r;
            print(FORMAT_ROUTE, evpnInstanceRoute.evpnInstanceName(),
                  evpnInstanceRoute.prefix(), evpnInstanceRoute.nextHop());
        });

    }

}
