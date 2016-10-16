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
package org.onosproject.evpn.rsc;

import static org.onlab.util.Tools.groupedThreads;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.evpn.rsc.baseport.BasePortService;
import org.onosproject.evpn.rsc.vpninstance.VpnInstanceService;
import org.onosproject.evpn.rsc.vpnport.VpnPortService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.justinsb.etcd.EtcdClient;
import com.justinsb.etcd.EtcdClientException;
import com.justinsb.etcd.EtcdResult;

public class EtcdMonitor {
    private static final String KEYPATH = "/net-l3vpn/proton";
    private static String etcduri;
    private EtcdClient etcdClient;

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ExecutorService executorService = Executors
            .newFixedThreadPool(5, groupedThreads("EVPN-EtcdMonitor",
                                                  "executor-%d", log));

    public EtcdMonitor(String etcduri) {
        this.etcduri = etcduri;
        etcdClient = new EtcdClient(URI.create(etcduri));
    }

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
                    log.info("Etcd monitor data is url {} and value {}",
                             watchResult.node.key, watchResult.node.value);
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

    private void processEtcdResponse(EtcdResult watchResult) {
        String[] list = watchResult.node.key.split("/");
        String target = list[list.length - 2];
        if (target.equals("ProtonBasePort")) {
            BasePortService basePortService = DefaultServiceDirectory.getService(BasePortService.class);
            basePortService.processEtcdResponse(watchResult);
        } else if (target.equals("VpnInstance")) {
            VpnInstanceService vpnInstanceService = DefaultServiceDirectory.getService(VpnInstanceService.class);
            vpnInstanceService.processEtcdResponse(watchResult);
        } else if (target.equals("VPNPort")) {
            VpnPortService vpnPortService = DefaultServiceDirectory.getService(VpnPortService.class);
            vpnPortService.processEtcdResponse(watchResult);
        }
    }
}
