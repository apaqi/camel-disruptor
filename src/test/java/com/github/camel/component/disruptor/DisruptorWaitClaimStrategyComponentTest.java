/*
 * Copyright 2012 Riccardo Sirchia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.camel.component.disruptor;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.runners.Parameterized.Parameters;

/**
 * Tests the WaitStrategy and ClaimStrategy configuration of the disruptor component
 */
@RunWith(value = Parameterized.class)
public class DisruptorWaitClaimStrategyComponentTest extends CamelTestSupport {
    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce
    protected ProducerTemplate template;

    private static final Integer VALUE = Integer.valueOf(42);

    private final String producerType;
    private final String waitStrategy;
    private String disruptorUri;

    public DisruptorWaitClaimStrategyComponentTest(final String waitStrategy, final String producerType) {

        this.waitStrategy = waitStrategy;
        this.producerType = producerType;
    }

    @Parameters
    public static Collection<String[]> strategies() {
        final List<String[]> strategies = new ArrayList<String[]>();

        for (final DisruptorWaitStrategy waitStrategy : DisruptorWaitStrategy.values()) {
            for (final DisruptorProducerType producerType : DisruptorProducerType.values()) {
                strategies.add(new String[] {waitStrategy.name(), producerType.name()});
            }
        }

        return strategies;
    }


    @Test
    public void testProduce() throws InterruptedException {
        resultEndpoint.expectedBodiesReceived(VALUE);
        resultEndpoint.setExpectedMessageCount(1);

        template.asyncSendBody(disruptorUri, VALUE);

        resultEndpoint.await(5, TimeUnit.SECONDS);
        resultEndpoint.assertIsSatisfied();
    }


    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {

        disruptorUri = "disruptor:test?waitStrategy=" + waitStrategy+"&producerType=" + producerType;

        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(disruptorUri).to("mock:result");
            }
        };
    }
}
