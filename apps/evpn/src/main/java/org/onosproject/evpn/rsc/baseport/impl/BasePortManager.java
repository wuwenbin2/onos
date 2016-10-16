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

package org.onosproject.evpn.rsc.baseport.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.evpn.rsc.baseport.BasePortService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.MultiValuedTimestamp;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.vtnrsc.AllowedAddressPair;
import org.onosproject.vtnrsc.BindingHostId;
import org.onosproject.vtnrsc.DefaultFloatingIp;
import org.onosproject.vtnrsc.DefaultVirtualPort;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.FloatingIp;
import org.onosproject.vtnrsc.FloatingIpId;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.SecurityGroup;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetwork;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.TenantRouter;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPort.State;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.justinsb.etcd.EtcdResult;

/**
 * Provides implementation of the BasePort APIs.
 */
@Component(immediate = true)
@Service
public class BasePortManager implements BasePortService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String BASEPORT = "evpn-base-port-store";
    private static final String EVPN_APP = "org.onosproject.evpn";
    private static final String VIRTUALPORT_ID_NOT_NULL = "VirtualPort ID cannot be null";
    private static final String VIRTUALPORT_NOT_NULL = "VirtualPort  cannot be null";
    private static final String TENANTID_NOT_NULL = "TenantId  cannot be null";
    private static final String NETWORKID_NOT_NULL = "NetworkId  cannot be null";
    private static final String DEVICEID_NOT_NULL = "DeviceId  cannot be null";
    private static final String FIXEDIP_NOT_NULL = "FixedIp  cannot be null";
    private static final String MAC_NOT_NULL = "Mac address  cannot be null";
    private static final String IP_NOT_NULL = "Ip  cannot be null";
    private static final String JSON_NOT_NULL = "JsonNode can not be null";
    private static final String RESPONSE_NOT_NULL = "JsonNode can not be null";

    protected EventuallyConsistentMap<VirtualPortId, VirtualPort> basePortStore;
    protected ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected VirtualPortService virtualPortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(EVPN_APP);

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(MultiValuedTimestamp.class)
                .register(TenantNetworkId.class).register(Host.class)
                .register(TenantNetwork.class).register(TenantNetworkId.class)
                .register(TenantId.class).register(SubnetId.class)
                .register(VirtualPortId.class).register(VirtualPort.State.class)
                .register(AllowedAddressPair.class).register(FixedIp.class)
                .register(FloatingIp.class).register(FloatingIpId.class)
                .register(FloatingIp.Status.class).register(UUID.class)
                .register(DefaultFloatingIp.class).register(BindingHostId.class)
                .register(SecurityGroup.class).register(IpAddress.class)
                .register(DefaultVirtualPort.class).register(RouterId.class)
                .register(TenantRouter.class).register(VirtualPort.class);
        basePortStore = storageService
                .<VirtualPortId, VirtualPort>eventuallyConsistentMapBuilder()
                .withName(BASEPORT).withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        log.info("Evpn Base port Started");
    }

    @Deactivate
    public void deactivate() {
        basePortStore.destroy();
        log.info("Evpn Base port Stop");
    }

    @Override
    public boolean exists(VirtualPortId vPortId) {
        checkNotNull(vPortId, VIRTUALPORT_ID_NOT_NULL);
        return basePortStore.containsKey(vPortId);
    }

    @Override
    public VirtualPort getPort(VirtualPortId vPortId) {
        checkNotNull(vPortId, VIRTUALPORT_ID_NOT_NULL);
        return basePortStore.get(vPortId);
    }

    @Override
    public VirtualPort getPort(FixedIp fixedIP) {
        checkNotNull(fixedIP, FIXEDIP_NOT_NULL);
        List<VirtualPort> vPorts = new ArrayList<>();
        basePortStore.values().forEach(p -> {
            Iterator<FixedIp> fixedIps = p.fixedIps().iterator();
            while (fixedIps.hasNext()) {
                if (fixedIps.next().equals(fixedIP)) {
                    vPorts.add(p);
                    break;
                }
            }
        });
        if (vPorts.size() == 0) {
            return null;
        }
        return vPorts.get(0);
    }

    @Override
    public VirtualPort getPort(MacAddress mac) {
        checkNotNull(mac, MAC_NOT_NULL);
        List<VirtualPort> vPorts = new ArrayList<>();
        basePortStore.values().stream().forEach(p -> {
            if (p.macAddress().equals(mac)) {
                vPorts.add(p);
            }
        });
        if (vPorts.size() == 0) {
            return null;
        }
        return vPorts.get(0);
    }

    @Override
    public VirtualPort getPort(TenantNetworkId networkId, IpAddress ip) {
        checkNotNull(networkId, NETWORKID_NOT_NULL);
        checkNotNull(ip, IP_NOT_NULL);
        List<VirtualPort> vPorts = new ArrayList<>();
        basePortStore.values().stream()
                .filter(p -> p.networkId().equals(networkId)).forEach(p -> {
                    Iterator<FixedIp> fixedIps = p.fixedIps().iterator();
                    while (fixedIps.hasNext()) {
                        if (fixedIps.next().ip().equals(ip)) {
                            vPorts.add(p);
                            break;
                        }
                    }
                });
        if (vPorts.size() == 0) {
            return null;
        }
        return vPorts.get(0);
    }

    @Override
    public Collection<VirtualPort> getPorts() {
        return Collections.unmodifiableCollection(basePortStore.values());
    }

    @Override
    public Collection<VirtualPort> getPorts(TenantNetworkId networkId) {
        checkNotNull(networkId, NETWORKID_NOT_NULL);
        return basePortStore.values().stream()
                .filter(d -> d.networkId().equals(networkId))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<VirtualPort> getPorts(TenantId tenantId) {
        checkNotNull(tenantId, TENANTID_NOT_NULL);
        return basePortStore.values().stream()
                .filter(d -> d.tenantId().equals(tenantId))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<VirtualPort> getPorts(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICEID_NOT_NULL);
        return basePortStore.values().stream()
                .filter(d -> d.deviceId().equals(deviceId))
                .collect(Collectors.toList());
    }

    @Override
    public boolean createPorts(Iterable<VirtualPort> vPorts) {
        checkNotNull(vPorts, VIRTUALPORT_NOT_NULL);
        for (VirtualPort vPort : vPorts) {
            log.debug("vPortId is  {} ", vPort.portId().toString());
            basePortStore.put(vPort.portId(), vPort);
            if (!basePortStore.containsKey(vPort.portId())) {
                log.debug("The virtualPort is created failed whose identifier is {} ",
                          vPort.portId().toString());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean updatePorts(Iterable<VirtualPort> vPorts) {
        checkNotNull(vPorts, VIRTUALPORT_NOT_NULL);
        for (VirtualPort vPort : vPorts) {
            basePortStore.put(vPort.portId(), vPort);
            if (!basePortStore.containsKey(vPort.portId())) {
                log.debug("The virtualPort is not exist whose identifier is {}",
                          vPort.portId().toString());
                return false;
            }

            basePortStore.put(vPort.portId(), vPort);

            if (!vPort.equals(basePortStore.get(vPort.portId()))) {
                log.debug("The virtualPort is updated failed whose  identifier is {}",
                          vPort.portId().toString());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean removePorts(Iterable<VirtualPortId> vPortIds) {
        checkNotNull(vPortIds, VIRTUALPORT_ID_NOT_NULL);
        for (VirtualPortId vPortId : vPortIds) {
            basePortStore.remove(vPortId);
            if (basePortStore.containsKey(vPortId)) {
                log.debug("The virtualPort is removed failed whose identifier is {}",
                          vPortId.toString());
                return false;
            }
        }
        return true;
    }

    @Override
    public void processEtcdResponse(EtcdResult response) {
        checkNotNull(response, RESPONSE_NOT_NULL);
        if (response.action.equals("delete")) {
            String[] list = response.node.key.split("/");
            VirtualPortId basePortId = VirtualPortId
                    .portId(list[list.length - 1]);
            Set<VirtualPortId> basePortIds = Sets.newHashSet(basePortId);
            removePorts(basePortIds);
        } else {
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode subnode = mapper.readTree(response.node.value);
                Collection<VirtualPort> basePorts = changeJsonToSub(subnode);
                createPorts(basePorts);
                virtualPortService.createPorts(basePorts);
            } catch (IOException e) {
                log.debug("Json format errer {}", e.toString());
            }
        }
    }

    /**
     * Returns a collection of virtualPorts from subnetNodes.
     *
     * @param vPortNodes the basePort json node
     * @return
     */
    private Collection<VirtualPort> changeJsonToSub(JsonNode vPortNodes) {
        checkNotNull(vPortNodes, JSON_NOT_NULL);
        Map<VirtualPortId, VirtualPort> vportMap = new HashMap<>();
        Map<String, String> strMap = new HashMap<>();
        VirtualPortId id = VirtualPortId.portId(vPortNodes.get("id").asText());
        String name = vPortNodes.get("name").asText();
        TenantId tenantId = TenantId
                .tenantId(vPortNodes.get("tenant_id").asText());
        TenantNetworkId networkId = TenantNetworkId
                .networkId(vPortNodes.get("network_id").asText());
        Boolean adminStateUp = vPortNodes.get("admin_state_up").asBoolean();
        String state = vPortNodes.get("status").asText();
        MacAddress macAddress = MacAddress
                .valueOf(vPortNodes.get("mac_address").asText());
        DeviceId deviceId = DeviceId
                .deviceId(vPortNodes.get("device_id").asText());
        String deviceOwner = vPortNodes.get("device_owner").asText();
        Set<FixedIp> fixedIps = new HashSet<>();
        FixedIp fixedIp = FixedIp
                .fixedIp(SubnetId.subnetId(id.toString()), IpAddress
                        .valueOf(vPortNodes.get("ipaddress").asText()));
        fixedIps.add(fixedIp);

        BindingHostId bindingHostId = BindingHostId
                .bindingHostId(vPortNodes.get("host_id").asText());
        String bindingVnicType = vPortNodes.get("vnic_type").asText();
        String bindingVifType = vPortNodes.get("vif_type").asText();
        String bindingVifDetails = vPortNodes.get("vif_details").asText();
        strMap.put("name", name);
        strMap.put("deviceOwner", deviceOwner);
        strMap.put("bindingVnicType", bindingVnicType);
        strMap.put("bindingVifType", bindingVifType);
        strMap.put("bindingVifDetails", bindingVifDetails);
        VirtualPort vPort = new DefaultVirtualPort(id, networkId, adminStateUp,
                                                   strMap, isState(state),
                                                   macAddress, tenantId,
                                                   deviceId, fixedIps,
                                                   bindingHostId, null, null);
        vportMap.put(id, vPort);

        return Collections.unmodifiableCollection(vportMap.values());
    }

    /**
     * Returns VirtualPort State.
     *
     * @param state the virtualport state
     * @return the virtualPort state
     */
    private State isState(String state) {
        if (state.equals("ACTIVE")) {
            return VirtualPort.State.ACTIVE;
        } else {
            return VirtualPort.State.DOWN;
        }

    }
}
