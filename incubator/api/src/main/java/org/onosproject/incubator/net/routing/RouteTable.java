package org.onosproject.incubator.net.routing;

import java.util.Collection;

public interface RouteTable {

    void update(Route route);

    void remove(Route route);

    Collection<Route> getRoutes();

    Collection<Route> getRoutesForNextHop(NextHop nextHop);
}
