package org.onosproject.evpn.rsc.cli;

import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.evpn.rsc.DefaultVpnInstance;
import org.onosproject.evpn.rsc.VpnInstanceId;
import org.onosproject.evpn.rsc.vpninstance.VpnInstanceService;
import org.onosproject.evpn.rsc.vpnport.VpnPortService;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstance;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceName;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceNextHop;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstancePrefix;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceRoute;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceRouteAdminService;
import org.onosproject.incubator.net.evpnrouting.Label;
import org.onosproject.incubator.net.evpnrouting.RouteDistinguisher;
import org.onosproject.incubator.net.evpnrouting.RouteTarget;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;

import com.google.common.collect.Sets;

@Command(scope = "onos", name = "evpn-instance-add", description = "Supports for start etcd monitor")
public class EvpnInstanceAddCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        VpnInstanceService service = get(VpnInstanceService.class);
        VpnPortService portservice = get(VpnPortService.class);
        VirtualPortService vportervice = get(VirtualPortService.class);
        VpnInstanceId instanceId = VpnInstanceId.vpnInstanceId("1001");
        EvpnInstanceName evpnName = EvpnInstanceName.evpnName("vpn1");
        RouteDistinguisher rd = RouteDistinguisher.routeDistinguisher("100:1");
        RouteTarget rt = RouteTarget.routeTarget("100:1");
        DefaultVpnInstance vpnInstance = new DefaultVpnInstance(instanceId,
                                                                evpnName,
                                                                "descripstion",
                                                                rd, rt);
        VpnInstanceId instanceId2 = VpnInstanceId.vpnInstanceId("1002");
        EvpnInstanceName evpnName2 = EvpnInstanceName.evpnName("vpn2");
        RouteDistinguisher rd2 = RouteDistinguisher.routeDistinguisher("100:2");
        RouteTarget rt2 = RouteTarget.routeTarget("100:2");
        DefaultVpnInstance vpnInstance2 = new DefaultVpnInstance(instanceId2,
                                                                evpnName2,
                                                                "descripstion",
                                                                rd2, rt2);
        service.createInstances(Sets.newHashSet(vpnInstance, vpnInstance2));
        EvpnInstanceRouteAdminService routeService = get(EvpnInstanceRouteAdminService.class);
        EvpnInstanceRoute evpnInstanceRoute = new EvpnInstanceRoute(evpnName,
                                                                    rd, rt,
                                                                    EvpnInstancePrefix
                                                                            .evpnPrefix(EvpnInstance
                                                                                    .evpnMessage(rd,
                                                                                                 rt,
                                                                                                 evpnName),
                                                                                        MacAddress.ZERO,
                                                                                        Ip4Address
                                                                                                .valueOf("0.0.0.0")),
                                                                    EvpnInstanceNextHop
                                                                            .evpnNextHop(IpAddress
                                                                                    .valueOf("127.0.0.1"),
                                                                                         Label.label(0)));
        routeService.updateEvpnRoute(Sets.newHashSet(evpnInstanceRoute));
    }

}
