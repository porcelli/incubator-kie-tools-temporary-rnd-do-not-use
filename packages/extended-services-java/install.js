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

const buildEnv = require("./env");
const { setup } = require("@kie-tools/maven-config-setup-helper");

setup(`
    -Drevision=${buildEnv.env.extendedServices.version}
    -Dip=${buildEnv.env.extendedServices.ip}
    -Dport=${buildEnv.env.extendedServices.port}
    -DkieSandboxUrl=${buildEnv.env.extendedServices.kieSandboxUrl}
    -Dquarkus.platform.version=${buildEnv.env.quarkusPlatform.version}
    -Dversion.org.kie.kogito=${buildEnv.env.kogitoRuntime.version}
`);
