package org.example.interceptors.auth;

import io.grpc.*;
import io.grpc.ServerCall.Listener;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class AuthInterceptor implements ServerInterceptor {

    private final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    public AuthInterceptor() {}

    public <T, U> Listener<T> interceptCall(ServerCall<T, U> call, Metadata headers, ServerCallHandler<T, U> next) {
        log.info("starting auth-interceptor...");
        authorize();
        log.info("auth process finished!");
        return next.startCall(call, headers);
    }

    private void authorize() {
        final Tracer tracer = GlobalOpenTelemetry.getTracer("zeebe-gateway");
        final Span span = tracer.spanBuilder("zeebe-span-auth").startSpan();
        log.info("doing authorization stuff...");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        span.end();
    }

}
