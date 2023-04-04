/*
 *
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.netflix.ribbon.transport.netty.http;

import io.netty.channel.ChannelOption;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientBuilder;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;

import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfigKey;
import com.netflix.loadbalancer.Server;

public final class SSEClient<I> extends LoadBalancingHttpClient<I, ServerSentEvent> {
    
    public static <I> Builder<I, ServerSentEvent> sseClientBuilder() {
        return new Builder<>(t1 -> new SSEClient<I>(t1));
    }
    
    private SSEClient(LoadBalancingHttpClient.Builder<I, ServerSentEvent> t1) {
        super(t1);
    }


    @Override
    protected HttpClient<I, ServerSentEvent> getOrCreateRxClient(Server server) {
        HttpClientBuilder<I, ServerSentEvent> clientBuilder =
                new HttpClientBuilder<I, ServerSentEvent>(server.getHost(), server.getPort()).pipelineConfigurator(pipelineConfigurator);
        int requestConnectTimeout = getProperty(IClientConfigKey.Keys.ConnectTimeout, null, DefaultClientConfigImpl.DEFAULT_CONNECT_TIMEOUT);
        RxClient.ClientConfig rxClientConfig = new HttpClientConfig.Builder().build();
        return clientBuilder.channelOption(
                ChannelOption.CONNECT_TIMEOUT_MILLIS, requestConnectTimeout).config(rxClientConfig).build();
    }

    @Override
    public void shutdown() {
    }
}
