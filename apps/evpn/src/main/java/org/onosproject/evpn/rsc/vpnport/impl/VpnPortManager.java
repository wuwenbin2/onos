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
package org.onosproject.evpn.rsc.vpnport.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.groupedThreads;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.evpn.rsc.DefaultVpnPort;
import org.onosproject.evpn.rsc.VpnInstanceId;
import org.onosproject.evpn.rsc.VpnPort;
import org.onosproject.evpn.rsc.VpnPortId;
import org.onosproject.evpn.rsc.vpnport.VpnPortService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.justinsb.etcd.EtcdClient;
import com.justinsb.etcd.EtcdClientException;
import com.justinsb.etcd.EtcdResult;

/**
 * Provides implementation of the VpnPort APIs.
 */
@Component(immediate = true)
@Service
public class VpnPortManager implements VpnPortService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String VPNPORT = "evpn-vpn-port-store";
    private static final String EVPN_APP = "org.onosproject.evpn";
    private static final String KEYPATH = "/net-l3vpn/proton/VPNPort";
    private static final String VPNPORT_ID_NOT_NULL = "VpnPort ID cannot be null";
    private static final String VPNPORT_NOT_NULL = "VpnPort cannot be null";
    private static final String JSON_NOT_NULL = "JsonNode can not be null";
    private static final String RESPONSE_NOT_NULL = "JsonNode can not be null";

    protected EventuallyConsistentMap<VpnPortId, VpnPort> vpnPortStore;
    protected ApplicationId appId;
    private EtcdClient etcdClient;
    private final ExecutorService executorService = Executors
            .newFixedThreadPool(5, groupedThreads("EVPN-VpnPort", "executor-%d",
                                                  log));

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(EVPN_APP);
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API).register(VpnPort.class)
                .register(VpnPortId.class);
        vpnPortStore = storageService
                .<VpnPortId, VpnPort>eventuallyConsistentMapBuilder()
                .withName(VPNPORT).withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        log.info("Evpn Vpn Port Started");
    }

    @Override
    public void initEtcdMonitor(String etcduri) {
            etcdClient = new EtcdClient(URI.create(etcduri));
            etcdMonitor(etcduri);
    }

    @Deactivate
    public void deactivate() {
        vpnPortStore.destroy();
        log.info("Evpn Vpn Port Stop");
    }

    @Override
    public boolean exists(VpnPortId vpnPortId) {
        checkNotNull(vpnPortId, VPNPORT_ID_NOT_NULL);
        return vpnPortStore.containsKey(vpnPortId);
    }

    @Override
    public VpnPort getPort(VpnPortId vpnPortId) {
        checkNotNull(vpnPortId, VPNPORT_ID_NOT_NULL);
        return vpnPortStore.get(vpnPortId);
    }

    @Override
    public Collection<VpnPort> getPorts() {
        return Collections.unmodifiableCollection(vpnPortStore.values());
    }

    @Override
    public boolean createPorts(Iterable<VpnPort> vpnPorts) {
        checkNotNull(vpnPorts, VPNPORT_NOT_NULL);
        for (VpnPort vpnPort : vpnPorts) {
            log.debug("vpnPortId is  {} ", vpnPort.id().toString());
            vpnPortStore.put(vpnPort.id(), vpnPort);
            if (!vpnPortStore.containsKey(vpnPort.id())) {
                log.debug("The vpnPort is created failed whose identifier is {} ",
                          vpnPort.id().toString());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean updatePorts(Iterable<VpnPort> vpnPorts) {
        checkNotNull(vpnPorts, VPNPORT_NOT_NULL);
        for (VpnPort vpnPort : vpnPorts) {
            vpnPortStore.put(vpnPort.id(), vpnPort);
            if (!vpnPortStore.containsKey(vpnPort.id())) {
                log.debug("The vpnPort is not exist whose identifier is {}",
                          vpnPort.id().toString());
                return false;
            }

            vpnPortStore.put(vpnPort.id(), vpnPort);

            if (!vpnPort.equals(vpnPortStore.get(vpnPort.id()))) {
                log.debug("The vpnPort is updated failed whose  identifier is {}",
                          vpnPort.id().toString());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean removePorts(Iterable<VpnPortId> vpnPortIds) {
        checkNotNull(vpnPortIds, VPNPORT_NOT_NULL);
        for (VpnPortId vpnPortid : vpnPortIds) {
            vpnPortStore.remove(vpnPortid);
            if (vpnPortStore.containsKey(vpnPortid)) {
                log.debug("The vpnPort is removed failed whose identifier is {}",
                          vpnPortid.toString());
                return false;
            }
        }
        return true;
    }

    /**
     * Start Etcd monitor.
     */
    private void etcdMonitor(String etcduri) {
        executorService.execute(new Runnable() {
            public void run() {
                try {
                    log.info("Etcd monitor to url {} and keypath {}", etcduri,
                             KEYPATH);
                    ListenableFuture<EtcdResult> watchFuture = etcdClient
                            .watch(KEYPATH, null, true);
                    EtcdResult watchResult = watchFuture.get();
                    processEtcdResponse(watchResult);
                    etcdMonitor(etcduri);
                } catch (InterruptedException e) {
                    log.debug("Etcd monitor with error {}", e.getMessage());
                } catch (ExecutionException e) {
                    log.debug("Etcd monitor with error {}", e.getMessage());
                } catch (EtcdClientException e) {
                    log.debug("Etcd monitor with error {}", e.getMessage());
                }
            }
        });
    }

    /**
     * process Etcd response.
     *
     * @param response Etcd response
     */
    private void processEtcdResponse(EtcdResult response) {
        checkNotNull(response, RESPONSE_NOT_NULL);
        if (response.action.equals("delete")) {
            String[] list = response.node.key.split("/");
            VpnPortId vpnPortId = VpnPortId.vpnPortId(list[list.length - 1]);
            Set<VpnPortId> vpnPortIds = Sets.newHashSet(vpnPortId);
            removePorts(vpnPortIds);
        } else {
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode subnode = mapper.readTree(response.node.value);
                Collection<VpnPort> vpnPorts = changeJsonToSub(subnode);
                createPorts(vpnPorts);
            } catch (IOException e) {
                log.debug("Json format errer {}", e.toString());
            }
        }
    }

    /**
     * Returns a collection of vpnPort from subnetNodes.
     *
     * @param vpnPortNodes the vpnPort json node
     * @return
     */
    private Collection<VpnPort> changeJsonToSub(JsonNode vpnPortNodes) {
        checkNotNull(vpnPortNodes, JSON_NOT_NULL);
        Map<VpnPortId, VpnPort> vpnPortMap = new HashMap<>();
        VpnPortId id = VpnPortId.vpnPortId(vpnPortNodes.get("id").asText());
        VpnInstanceId vpnInstanceId = VpnInstanceId
                .vpnInstanceId(vpnPortNodes.get("vpn_instance").asText());
        VpnPort vpnPort = new DefaultVpnPort(id, vpnInstanceId);
        vpnPortMap.put(id, vpnPort);

        return Collections.unmodifiableCollection(vpnPortMap.values());
    }
}
