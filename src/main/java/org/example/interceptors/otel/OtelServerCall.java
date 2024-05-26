package org.example.interceptors.otel;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;

import java.util.Map;

import static org.example.interceptors.otel.OtelHeaders.X_SPAN_ID;
import static org.example.interceptors.otel.OtelHeaders.X_TRACE_ID;
import static org.example.interceptors.otel.OtelProps.SPAN_ID;
import static org.example.interceptors.otel.OtelProps.TRACE_ID;

public class OtelServerCall<T, U> extends ForwardingServerCall.SimpleForwardingServerCall<T, U> {

    private final Map<String, String> mdc;

    protected OtelServerCall(ServerCall<T, U> delegate, Map<String, String> mdc) {
        super(delegate);
        this.mdc = mdc;
    }

    public void sendHeaders(Metadata headers) {
        headers.put(X_TRACE_ID, mdc.get(TRACE_ID));
        headers.put(X_SPAN_ID, mdc.get(SPAN_ID));
        super.sendHeaders(headers);
    }

}
