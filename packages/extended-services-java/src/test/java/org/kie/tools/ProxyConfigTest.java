/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kie.tools;

import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import javax.inject.Inject;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class ProxyConfigTest {

    @ConfigProperty(name = "quarkus.http.host")
    String expectedIp;
    @ConfigProperty(name = "quarkus.http.port")
    String expectedPort;
    boolean expectedInsecureSkipVerify = false;
    
    @Inject
    ProxyConfig proxyConfig;

    @Test
    public void testProxyConfig() {
        assertEquals(expectedIp, proxyConfig.getIP());
        assertEquals(expectedPort, proxyConfig.getPort());
        assertEquals(expectedInsecureSkipVerify, proxyConfig.getInsecureSkipVerify());
    }
}
