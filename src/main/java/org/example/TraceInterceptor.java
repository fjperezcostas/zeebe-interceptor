package org.example;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public final class TraceInterceptor implements ServerInterceptor {

    private final Logger log = LoggerFactory.getLogger(TraceInterceptor.class);
    private final TextMapGetter<Metadata> getter;

    public TraceInterceptor() {
        final Resource resource = Resource.getDefault().toBuilder().build();
        final SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
                .setResource(resource)
                .build();
        final W3CTraceContextPropagator propagator = W3CTraceContextPropagator.getInstance();
        OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(propagator))
                .buildAndRegisterGlobal();
        getter = new TextMapGetter<>() {
            @Override
            public Iterable<String> keys(Metadata carrier) {
                return carrier.keys();
            }

            @Override
            public String get(Metadata carrier, String key) {
                return carrier.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
            }
        };
    }

    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            final ServerCall<ReqT, RespT> call,
            final Metadata headers,
            final ServerCallHandler<ReqT, RespT> next) {
        Context parentCtx = GlobalOpenTelemetry
                .getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), headers, getter);
        final Tracer tracer = GlobalOpenTelemetry.getTracer("zeebe-gateway");
        final Span span = tracer.spanBuilder("zeebe-span").setParent(parentCtx).startSpan();
        final SpanContext ctx = span.getSpanContext();
        MDC.put("traceId", ctx.getTraceId());
        MDC.put("spanId", ctx.getSpanId());
        MDC.put("traceFlags", ctx.getTraceFlags().toString());
        log.info("configured tracer successfully");
        try {
            return next.startCall(call, headers);
        } finally {
            MDC.clear();
            span.end();
        }
    }
}
