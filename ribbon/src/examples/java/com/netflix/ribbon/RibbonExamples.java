package com.netflix.ribbon;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;

import java.util.Map;

import rx.Observable;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixInvokableInfo;
import com.netflix.hystrix.HystrixObservableCommand;
import com.netflix.ribbon.http.HttpRequestTemplate;
import com.netflix.ribbon.http.HttpResourceGroup;


public class RibbonExamples {
    public static void main(String[] args) {
        HttpResourceGroup group = Ribbon.createHttpResourceGroup("myclient");
        HttpRequestTemplate<ByteBuf> template = group.newTemplateBuilder("GetUser")
        .withResponseValidator(response -> {
            if (response.getStatus().code() >= 500) {
                throw new ServerError("Unexpected response");
            }
        })   
        .withFallbackProvider((t1, vars) -> Observable.empty())
        .withHystrixProperties((HystrixObservableCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("mygroup"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionIsolationThreadTimeoutInMilliseconds(2000))))
        .withUriTemplate("/{id}").build();
        
        template.requestBuilder().withRequestProperty("id", 1).build().execute();
        
        // example showing the use case of getting the entity with Hystrix meta data
        template.requestBuilder().withRequestProperty("id", 3).build().withMetadata().observe()
            .flatMap(t1 -> {
            if (t1.getHystrixInfo().isResponseFromFallback()) {
                return Observable.empty();
            }
            return t1.content().map(ByteBuf::toString);
        });
    }

}
