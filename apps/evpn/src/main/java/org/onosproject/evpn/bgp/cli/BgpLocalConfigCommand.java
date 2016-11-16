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

package org.onosproject.evpn.bgp.cli;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.bgp.controller.BgpRouteService;
import org.onosproject.cli.AbstractShellCommand;

/**
 * Command to add new BGP peer to existing internal speaker.
 */
@Command(scope = "onos", name = "bgp-local-config", description = "BGP local config")
public class BgpLocalConfigCommand extends AbstractShellCommand {

    @Option(name = "-i", aliases = "--id",
            description = "id of local node",
            required = true, multiValued = false)
    String id = null;

    @Option(name = "-a", aliases = "--as",
            description = "as of local node",
            required = true, multiValued = false)
    int as = 0;

    @Option(name = "-t", aliases = "--holdTime",
            description = "holdTime of local node",
            required = false, multiValued = false)
    short holdTime = 90;

    @Option(name = "-e", aliases = "--evpnCapability",
            description = "evpnCapability of local node",
            required = false, multiValued = false)
    boolean evpnCapability = false;

    @Override
    protected void execute() {

        BgpRouteService service = AbstractShellCommand
                .get(BgpRouteService.class);
        int maxsession = 20;
        boolean isLargeAs = false;
        service.updateConfiguration(id, as, holdTime, maxsession, isLargeAs,
                                    evpnCapability);
    }
}
