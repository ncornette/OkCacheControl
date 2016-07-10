/*
 * Copyright (C) 2016 Nicolas Cornette
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ncornette.cache;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by nic on 21/06/16.
 */
public class OkCacheControl {

    /**
     * Created by nic on 20/06/16.
     */
    public interface NetworkMonitor {

        boolean isOnline();
    }

    public interface MaxAgeControl {
        /**
         * @return max-age in seconds
         */
        long getMaxAge();
    }

    public static OkCacheControl on(OkHttpClient.Builder okBuilder) {
        OkCacheControl builder = new OkCacheControl(okBuilder);
        return builder;
    }


    private NetworkMonitor networkMonitor;
    private long maxAgeValue;
    private TimeUnit maxAgeUnit;
    private OkHttpClient.Builder okBuilder;
    private MaxAgeControl maxAgeControl;

    private OkCacheControl(OkHttpClient.Builder okBuilder) {
        this.okBuilder = okBuilder;
    }

    public OkCacheControl overrideServerCachePolicy(Long maxAgeSeconds) {
        if (maxAgeSeconds == null) {
            return this.overrideServerCachePolicy(0, null);
        } else {
            return this.overrideServerCachePolicy(maxAgeSeconds, TimeUnit.SECONDS);
        }
    }

    public OkCacheControl overrideServerCachePolicy(long timeValue, TimeUnit unit) {
        this.maxAgeControl = null;
        this.maxAgeValue = timeValue;
        this.maxAgeUnit = unit;
        return this;
    }

    public OkCacheControl overrideServerCachePolicy(MaxAgeControl maxAgeControl) {
        this.maxAgeUnit = null;
        this.maxAgeControl = maxAgeControl;
        return this;
    }

    public OkCacheControl forceCacheWhenOffline(NetworkMonitor networkMonitor) {
        this.networkMonitor = networkMonitor;
        return this;
    }

    public OkHttpClient.Builder apply() {
        if (networkMonitor == null && maxAgeUnit == null && maxAgeControl == null) {
            return okBuilder;
        }

        if (maxAgeUnit != null) {
            maxAgeControl = new StaticMaxAgeControl(maxAgeValue, maxAgeUnit);
        }

        ResponseHandler responseHandler;
        if (maxAgeControl != null) {
            responseHandler = new CachePolicyResponseHandler(maxAgeControl);
        } else {
            responseHandler = new ResponseHandler();
        }

        RequestHandler requestHandler;
        if (networkMonitor != null) {
            requestHandler = new NetworkMonitorRequestHandler(networkMonitor);
        } else {
            requestHandler = new RequestHandler();
        }

        Interceptor cacheControlInterceptor = getCacheControlInterceptor(
                requestHandler, responseHandler);

        okBuilder.addNetworkInterceptor(cacheControlInterceptor);

        if (networkMonitor != null) {
            okBuilder.addInterceptor(cacheControlInterceptor);
        }

        return okBuilder;
    }

    private static class StaticMaxAgeControl implements MaxAgeControl {
        private TimeUnit maxAgeUnit;
        private long maxAgeValue;

        private StaticMaxAgeControl(long maxAgeValue, TimeUnit maxAgeUnit) {
            this.maxAgeUnit = maxAgeUnit;
            this.maxAgeValue = maxAgeValue;
        }

        @Override
        public long getMaxAge() {
            return maxAgeUnit.toSeconds(maxAgeValue);
        }
    }

    private static class CachePolicyResponseHandler extends ResponseHandler {
        private MaxAgeControl maxAgeControl;

        private CachePolicyResponseHandler(MaxAgeControl maxAgeControl) {
            this.maxAgeControl = maxAgeControl;
        }

        @Override
        public Response newResponse(Response response) {
            return response.newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", "max-age=" + maxAgeControl.getMaxAge())
                .build();
        }
    }

    private static class NetworkMonitorRequestHandler extends RequestHandler {
        private NetworkMonitor networkMonitor;

        private NetworkMonitorRequestHandler(NetworkMonitor networkMonitor) {
            this.networkMonitor = networkMonitor;
        }

        @Override
        public Request newRequest(Request request) {
            Request.Builder newBuilder = request.newBuilder();
            if (!networkMonitor.isOnline()) {
                // To be used with Application Interceptor to use Expired cache
                newBuilder.cacheControl(CacheControl.FORCE_CACHE);
            }
            return newBuilder.build();
        }
    }

    private static Interceptor getCacheControlInterceptor(final RequestHandler requestHandler,
                                                          final ResponseHandler responseHandler) {
        return new Interceptor() {
            @Override public Response intercept(Chain chain) throws IOException {
                Request originalRequest = chain.request();
                Request request = requestHandler.newRequest(originalRequest);

                Response originalResponse = chain.proceed(request);

                return responseHandler.newResponse(originalResponse);
            }
        };
    }

    private static class ResponseHandler {
        public Response newResponse(Response response) {
            return response;
        }
    }

    private static class RequestHandler {
        public Request newRequest(Request request) {
            return request;
        }
    }
}
