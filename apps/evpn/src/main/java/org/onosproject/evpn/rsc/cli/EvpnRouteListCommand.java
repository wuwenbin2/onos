package org.onosproject.evpn.rsc.cli;

import java.util.Collection;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.routing.EvpnRoute;
import org.onosproject.incubator.net.routing.Route;
import org.onosproject.incubator.net.routing.RouteService;
import org.onosproject.incubator.net.routing.RouteTableType;

@Command(scope = "onos", name = "evpn-routes", description = "Lists all routes in the route store")
public class EvpnRouteListCommand extends AbstractShellCommand {
    private static final String FORMAT_ROUTE = "   %-18s %-18s %-18s";

    @Override
    protected void execute() {
        RouteService service = AbstractShellCommand.get(RouteService.class);
        Collection<Route> evpnRoutes = service.getAllRoutes()
                .get(RouteTableType.VPN_PUBLIC);
        evpnRoutes.forEach(r -> {
            EvpnRoute evpnRoute = (EvpnRoute) r;
            print(FORMAT_ROUTE, evpnRoute.prefixMac(), evpnRoute.prefixIp(),
                  evpnRoute.ipNextHop());
        });
    }

}
