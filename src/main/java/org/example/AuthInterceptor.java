package org.example;

import io.grpc.*;
import io.grpc.ServerCall.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class AuthInterceptor implements ServerInterceptor {

    private final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    public AuthInterceptor() {}

    public <ReqT, RespT> Listener<ReqT> interceptCall(
            final ServerCall<ReqT, RespT> call,
            final Metadata headers,
            final ServerCallHandler<ReqT, RespT> next) {
        log.info("starting auth-interceptor...");
        log.info("doing authorization stuff...");
        log.info("done!");
        return next.startCall(call, headers);
    }

}
