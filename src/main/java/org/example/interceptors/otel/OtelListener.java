package org.example.interceptors.otel;

import io.grpc.ForwardingServerCallListener;
import io.grpc.ServerCall;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import org.slf4j.MDC;

import static org.example.interceptors.otel.OtelProps.*;


public class OtelListener<T> extends ForwardingServerCallListener.SimpleForwardingServerCallListener<T> {
    private final Span span;

    public OtelListener(ServerCall.Listener<T> delegate, Span span) {
        super(delegate);
        this.span = span;
    }

    public void onMessage(T message) {
        addTracingContext(() -> super.onMessage(message));
    }

    public void onHalfClose() {
        addTracingContext(super::onHalfClose);
    }

    public void onCancel() {
        addTracingContext(() -> closeSpan(super::onCancel));
    }

    public void onComplete() {
        addTracingContext(() -> closeSpan(super::onComplete));
    }

    public void onReady() {
        addTracingContext(super::onReady);
    }

    private void addTracingContext(Runnable function) {
        final SpanContext ctx = span.getSpanContext();
        MDC.put(TRACE_ID, ctx.getTraceId());
        MDC.put(SPAN_ID, ctx.getSpanId());
        MDC.put(TRACE_FLAGS, ctx.getTraceFlags().toString());
        try {
            function.run();
        } finally {
            MDC.remove(TRACE_ID);
            MDC.remove(SPAN_ID);
            MDC.remove(TRACE_FLAGS);
        }
    }

    private void closeSpan(Runnable function) {
        try {
            function.run();
        } finally {
            span.end();
        }
    }

}
