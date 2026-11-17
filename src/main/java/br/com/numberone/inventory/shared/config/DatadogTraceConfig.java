package br.com.numberone.inventory.shared.config;

import br.com.numberone.inventory.shared.infrastructure.observability.NumberOneHealthTraceInterceptor;
import datadog.trace.api.GlobalTracer;
import datadog.trace.api.interceptor.TraceInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatadogTraceConfig {

    @Bean
    public TraceInterceptor numberOneHealthTraceInterceptor() {
        TraceInterceptor interceptor = new NumberOneHealthTraceInterceptor();
        GlobalTracer.get().addTraceInterceptor(interceptor);
        return interceptor;
    }
}
