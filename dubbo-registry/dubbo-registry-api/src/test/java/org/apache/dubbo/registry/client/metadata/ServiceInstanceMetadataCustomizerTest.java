/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.registry.client.metadata;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.beans.factory.ScopeBeanFactory;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.MetadataService;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.rpc.model.ApplicationModel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_LABELS;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;
import static org.apache.dubbo.common.constants.CommonConstants.SIDE_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class ServiceInstanceMetadataCustomizerTest {
    public DefaultServiceInstance instance;
    private static MetadataService mockedMetadataService;
    private static ApplicationModel mockedApplicationModel;
    private static ScopeBeanFactory mockedBeanFactory;

    public static DefaultServiceInstance createInstance() {
        return new DefaultServiceInstance("A", "127.0.0.1", 20880, ApplicationModel.defaultModel());
    }

    @BeforeAll
    public static void setUp() {
        ApplicationConfig applicationConfig = new ApplicationConfig("test");
        ApplicationModel.defaultModel().getApplicationConfigManager().setApplication(applicationConfig);

        mockedMetadataService = Mockito.mock(MetadataService.class);

        mockedApplicationModel = Mockito.mock(ApplicationModel.class);
        Mockito.when(mockedApplicationModel.getBeanFactory()).thenReturn(mockedBeanFactory);
        mockedBeanFactory = Mockito.mock(ScopeBeanFactory.class);
        Mockito.when(mockedBeanFactory.getBean(MetadataService.class)).thenReturn(mockedMetadataService);
    }

    @AfterAll
    public static void clearUp() {
        ApplicationModel.reset();
    }

    @BeforeEach
    public void init() {
        instance = createInstance();
        mockedMetadataService = mock(MetadataService.class);

        URL url = URL.valueOf("dubbo://30.10.104.63:20880/org.apache.dubbo.demo.GreetingService?" + "params-filter=-default&" +
            "REGISTRY_CLUSTER=registry1&anyhost=true&application=demo-provider2&delay=5000&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&group=greeting&interface=org.apache.dubbo.demo.GreetingService&metadata-type=remote&methods=hello&pid=55805&release=&revision=1.0.0&service-name-mapping=true&side=provider&timeout=5000&timestamp=1630229110058&version=1.0.0");
        MetadataInfo metadataInfo = new MetadataInfo();
        metadataInfo.addService(url);
        instance.setServiceMetadata(metadataInfo);
    }

    @Test
    public void test() {
        ServiceInstanceMetadataCustomizer customizer = new ServiceInstanceMetadataCustomizer();
        try (MockedStatic<ConfigurationUtils> mockedUtils = Mockito.mockStatic(ConfigurationUtils.class)) {
                mockedUtils.when(() -> ConfigurationUtils.getProperty(ApplicationModel.defaultModel(), DUBBO_LABELS)).thenReturn("k1=v1;k2=v2");

                // check parameters loaded from infra adapters.
                customizer.customize(instance, mockedApplicationModel);
                assertEquals(2, instance.getMetadata().size());
                assertEquals("v1", instance.getMetadata().get("k1"));
                assertEquals("v2", instance.getMetadata().get("k2"));

                // check filters
                resetInstanceAndMock("excluded,-customized");
                customizer.customize(instance, mockedApplicationModel);
                assertEquals(2, instance.getMetadata().size());
                assertEquals("v1", instance.getMetadata().get("k1"));
                assertEquals("v2", instance.getMetadata().get("k2"));

                // check filters
                resetInstanceAndMock("-excluded,customized");
                customizer.customize(instance, mockedApplicationModel);
                assertEquals(3, instance.getMetadata().size());
                assertEquals("v1", instance.getMetadata().get("k1"));
                assertEquals("v2", instance.getMetadata().get("k2"));
                assertEquals(PROVIDER, instance.getMetadata().get(SIDE_KEY));
        }

    }

    private void resetInstanceAndMock(String filters) {
        instance.setMetadata(new HashMap<>());

        URL url = URL.valueOf("dubbo://30.10.104.63:20880/org.apache.dubbo.demo.GreetingService?" + "params-filter=" + filters + "&" +
            "REGISTRY_CLUSTER=registry1&anyhost=true&application=demo-provider2&delay=5000&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&group=greeting&interface=org.apache.dubbo.demo.GreetingService&metadata-type=remote&methods=hello&pid=55805&release=&revision=1.0.0&service-name-mapping=true&side=provider&timeout=5000&timestamp=1630229110058&version=1.0.0");
        MetadataInfo metadataInfo = new MetadataInfo();
        metadataInfo.addService(url);
        instance.setServiceMetadata(metadataInfo);
    }

}
