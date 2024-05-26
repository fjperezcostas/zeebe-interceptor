package org.example.interceptors.otel;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.slf4j.MDC;

import javax.annotation.Nonnull;

import static org.example.interceptors.otel.OtelProps.*;

public final class OtelInterceptor implements ServerInterceptor {

    private final TextMapGetter<Metadata> getter;

    public OtelInterceptor() {
        final Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), "zeebe-gateway")));
        final SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder()
                                .setEndpoint("http://jaeger:4317")
                                .build())
                        .build())
                .setResource(resource)
                .build();
        final W3CTraceContextPropagator propagator = W3CTraceContextPropagator.getInstance();
        OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(propagator))
                .buildAndRegisterGlobal();
        getter = new TextMapGetter<>() {
            public Iterable<String> keys(@Nonnull Metadata carrier) {
                return carrier.keys();
            }
            public String get(Metadata carrier, @Nonnull String key) {
                return carrier.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
            }
        };
    }

    public <T, U> ServerCall.Listener<T> interceptCall(ServerCall<T, U> call, Metadata headers, ServerCallHandler<T, U> next) {
        final Context parentCtx = GlobalOpenTelemetry
                .getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), headers, getter);
        final Tracer tracer = GlobalOpenTelemetry.getTracer("zeebe-gateway");
        final Span span = tracer.spanBuilder("zeebe-span").setParent(parentCtx).startSpan();
        final SpanContext ctx = span.getSpanContext();
        MDC.put(TRACE_ID, ctx.getTraceId());
        MDC.put(SPAN_ID, ctx.getSpanId());
        MDC.put(TRACE_FLAGS, ctx.getTraceFlags().toString());
        try (Scope ignored = span.makeCurrent()) {
            final ServerCall.Listener<T> delegate = next.startCall(new OtelServerCall<>(call, MDC.getCopyOfContextMap()), headers);
            return new OtelListener<>(delegate, span);
        } finally {
            MDC.remove(TRACE_ID);
            MDC.remove(SPAN_ID);
            MDC.remove(TRACE_FLAGS);
        }
    }
}
