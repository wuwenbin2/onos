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
package org.onosproject.evpn.rsc.vpninstance.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.groupedThreads;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.evpn.rsc.DefaultVpnInstance;
import org.onosproject.evpn.rsc.VpnInstance;
import org.onosproject.evpn.rsc.VpnInstanceId;
import org.onosproject.evpn.rsc.vpninstance.VpnInstanceService;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstance;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceName;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceNextHop;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstancePrefix;
import org.onosproject.incubator.net.evpnprivaterouting.EvpnInstanceRoute;
import org.onosproject.incubator.net.evpnrouting.Label;
import org.onosproject.incubator.net.evpnrouting.RouteDistinguisher;
import org.onosproject.incubator.net.evpnrouting.RouteTarget;
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
 * Provides implementation of the VpnInstance APIs.
 */
@Component(immediate = true)
@Service
public class VpnInstanceManager implements VpnInstanceService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String VPNINSTANCE = "evpn-vpn-instance-store";
    private static final String EVPN_APP = "org.onosproject.evpn";
    private static final String KEYPATH = "/net-l3vpn/proton/VpnInstance";
    private static final String CONFPATH = "../..//etcdMonitor.properties";
    private static String etcduri = "";
    private static final String VPNINSTANCE_ID_NOT_NULL = "VpnInstance ID cannot be null";
    private static final String VPNINSTANCE_NOT_NULL = "VpnInstance cannot be null";
    private static final String JSON_NOT_NULL = "JsonNode can not be null";
    private static final String RESPONSE_NOT_NULL = "JsonNode can not be null";

    protected EventuallyConsistentMap<VpnInstanceId, VpnInstance> vpnInstanceStore;
    protected ApplicationId appId;
    private EtcdClient etcdClient;
    private final ExecutorService executorService = Executors
            .newFixedThreadPool(5, groupedThreads("EVPN-VpnInstance",
                                                  "executor-%d", log));

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(EVPN_APP);
        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API).register(VpnInstance.class)
                .register(VpnInstanceId.class);
        vpnInstanceStore = storageService
                .<VpnInstanceId, VpnInstance>eventuallyConsistentMapBuilder()
                .withName(VPNINSTANCE).withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        log.info("Evpn Vpn Instance Started");
        initEtcdMonitor();
    }

    private void initEtcdMonitor() {
        String ip;
        String port;
        Properties prop = new Properties();
        InputStream in = Object.class.getResourceAsStream(CONFPATH);
        try {
            prop.load(in);
            ip = prop.getProperty("server.ip").trim();
            port = prop.getProperty("server.port").trim();
            etcduri = "http://" + ip + ":" + port;
        } catch (IOException e) {
            log.debug(e.getMessage());
        }
        if (!etcduri.equals("")) {
            etcdClient = new EtcdClient(URI.create(etcduri));
            etcdMonitor();
        }
    }

    @Deactivate
    public void deactivate() {
        vpnInstanceStore.destroy();
        log.info("Evpn Vpn Instance Stop");
    }

    @Override
    public boolean exists(VpnInstanceId vpnInstanceId) {
        checkNotNull(vpnInstanceId, VPNINSTANCE_ID_NOT_NULL);
        return vpnInstanceStore.containsKey(vpnInstanceId);
    }

    @Override
    public VpnInstance getInstance(VpnInstanceId vpnInstanceId) {
        checkNotNull(vpnInstanceId, VPNINSTANCE_ID_NOT_NULL);
        return vpnInstanceStore.get(vpnInstanceId);
    }

    @Override
    public Collection<VpnInstance> getInstances() {
        return Collections.unmodifiableCollection(vpnInstanceStore.values());
    }

    @Override
    public boolean createInstances(Iterable<VpnInstance> vpnInstances) {
        checkNotNull(vpnInstances, VPNINSTANCE_NOT_NULL);
        for (VpnInstance vpnInstance : vpnInstances) {
            log.debug("vpnInstanceId is  {} ", vpnInstance.id().toString());
            vpnInstanceStore.put(vpnInstance.id(), vpnInstance);
            if (!vpnInstanceStore.containsKey(vpnInstance.id())) {
                log.debug("The vpnInstance is created failed whose identifier is {} ",
                          vpnInstance.id().toString());
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean updateInstances(Iterable<VpnInstance> vpnInstances) {
        checkNotNull(vpnInstances, VPNINSTANCE_NOT_NULL);
        for (VpnInstance vpnInstance : vpnInstances) {
            vpnInstanceStore.put(vpnInstance.id(), vpnInstance);
            if (!vpnInstanceStore.containsKey(vpnInstance.id())) {
                log.debug("The vpnInstance is not exist whose identifier is {}",
                          vpnInstance.id().toString());
                return false;
            }

            vpnInstanceStore.put(vpnInstance.id(), vpnInstance);

            if (!vpnInstance.equals(vpnInstanceStore.get(vpnInstance.id()))) {
                log.debug("The vpnInstance is updated failed whose  identifier is {}",
                          vpnInstance.id().toString());
                return false;
            }
        }
        return true;

    }

    @Override
    public boolean removeInstances(Iterable<VpnInstanceId> vpnInstanceIds) {
        checkNotNull(vpnInstanceIds, VPNINSTANCE_ID_NOT_NULL);
        for (VpnInstanceId vpnInstanceId : vpnInstanceIds) {
            vpnInstanceStore.remove(vpnInstanceId);
            if (vpnInstanceStore.containsKey(vpnInstanceId)) {
                log.debug("The vpnInstance is removed failed whose identifier is {}",
                          vpnInstanceId.toString());
                return false;
            }
        }
        return true;
    }

    /**
     * Start Etcd monitor.
     */
    public void etcdMonitor() {
        executorService.execute(new Runnable() {
            public void run() {
                try {
                    log.info("Etcd monitor to url {} and keypath {}", etcduri,
                             KEYPATH);
                    ListenableFuture<EtcdResult> watchFuture = etcdClient
                            .watch(KEYPATH, null, true);
                    EtcdResult watchResult = watchFuture.get();
                    processEtcdResponse(watchResult);
                    etcdMonitor();
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
            VpnInstanceId vpnInstanceId = VpnInstanceId
                    .vpnInstanceId(list[list.length - 1]);
            Set<VpnInstanceId> vpnInstanceIds = Sets.newHashSet(vpnInstanceId);
            removeInstances(vpnInstanceIds);
        } else {
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode subnode = mapper.readTree(response.node.value);
                Collection<VpnInstance> vpnInstances = changeJsonToSub(subnode);
                createInstances(vpnInstances);
            } catch (IOException e) {
                log.debug("Json format errer {}", e.toString());
            }
        }
    }

    /**
     * Returns a collection of vpnInstances from subnetNodes.
     *
     * @param vpnInstanceNodes the vpnInstance json node
     * @return
     */
    private Collection<VpnInstance> changeJsonToSub(JsonNode vpnInstanceNodes) {
        checkNotNull(vpnInstanceNodes, JSON_NOT_NULL);
        Map<VpnInstanceId, VpnInstance> vpnInstanceMap = new HashMap<>();
        VpnInstanceId id = VpnInstanceId
                .vpnInstanceId(vpnInstanceNodes.get("id").asText());
        EvpnInstanceName name = EvpnInstanceName
                .evpnName(vpnInstanceNodes.get("vpn_instance_name").asText());
        String description = vpnInstanceNodes.get("description").asText();
        RouteDistinguisher routeDistinguisher = RouteDistinguisher
                .routeDistinguisher(vpnInstanceNodes.get("route_distinguishers")
                        .asText());
        RouteTarget routeTarget = RouteTarget
                .routeTarget(vpnInstanceNodes.get("ipv4_family").asText());
        VpnInstance vpnInstance = new DefaultVpnInstance(id, name, description,
                                                         routeDistinguisher,
                                                         routeTarget);
        EvpnInstanceRoute vpnInstanceRout = new EvpnInstanceRoute(name,
                                                                  routeDistinguisher,
                                                                  routeTarget,
                                                                  EvpnInstancePrefix
                                                                          .evpnPrefix(EvpnInstance
                                                                                  .evpnMessage(routeDistinguisher,
                                                                                               routeTarget,
                                                                                               name),
                                                                                      MacAddress.ZERO,
                                                                                      Ip4Address
                                                                                      .valueOf("0.0.0.0")),
                                                                  EvpnInstanceNextHop
                                                                          .evpnNextHop(IpAddress
                                                                                  .valueOf("127.0.0.1"),
                                                                                       Label.label(0)));
        vpnInstanceMap.put(id, vpnInstance);

        return Collections.unmodifiableCollection(vpnInstanceMap.values());
    }
}
