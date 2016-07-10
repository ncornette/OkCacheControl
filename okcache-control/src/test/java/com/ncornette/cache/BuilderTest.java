package com.ncornette.cache;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Created by nic on 24/06/16.
 */
public class BuilderTest {

    private MockWebServer mockWebServer;
    private OkCacheControl.NetworkMonitor networkMonitor;
    private OkCacheControl.MaxAgeControl maxAgeControl;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer(); mockWebServer.start();
        networkMonitor = mock(OkCacheControl.NetworkMonitor.class);
        maxAgeControl = mock(OkCacheControl.MaxAgeControl.class);
    }

    @Test
    public void testOverrideServerCachePolicy_NONE() throws Exception {
        //given
        OkHttpClient client = OkCacheControl.on(new OkHttpClient.Builder())
                .apply().build();

        //when
        mockWebServer.enqueue(new MockResponse().setBody("Response"));
        Response response = getResponse(client);

        //then
        then(response.header("Cache-Control")).isNull();
    }

    @Test
    public void testOverrideServerCachePolicy_DYNAMIC() throws Exception {
        //given
        given(maxAgeControl.getMaxAge()).willReturn(TimeUnit.MINUTES.toSeconds(5));
        OkHttpClient client = OkCacheControl.on(new OkHttpClient.Builder())
                .overrideServerCachePolicy(maxAgeControl)
                .apply().build();

        //when
        mockWebServer.enqueue(new MockResponse().setBody("Response"));
        Response response = getResponse(client);

        //then
        then(response.header("Cache-Control")).isEqualTo("max-age=300");
    }

    @Test
    public void testOverrideServerCachePolicy_LONG_NULL() throws Exception {
        //given
        OkHttpClient client = OkCacheControl.on(new OkHttpClient.Builder())
                .overrideServerCachePolicy((Long)null)
                .apply().build();

        //when
        mockWebServer.enqueue(new MockResponse().setBody("Response"));
        Response response = getResponse(client);

        //then
        then(response.header("Cache-Control")).isNull();
    }

    @Test
    public void testOverrideServerCachePolicy_LONG() throws Exception {
        //given
        OkHttpClient client = OkCacheControl.on(new OkHttpClient.Builder())
                .overrideServerCachePolicy(TimeUnit.SECONDS.convert(5, TimeUnit.MINUTES))
                .apply().build();

        //when
        mockWebServer.enqueue(new MockResponse().setBody("Response"));
        Response response = getResponse(client);

        //then
        then(response.header("Cache-Control")).isEqualTo(
                "max-age="+TimeUnit.SECONDS.convert(5, TimeUnit.MINUTES));
    }


    @Test
    public void testOverrideServerCachePolicy_STATIC() throws Exception {
        //given
        OkHttpClient client = OkCacheControl.on(new OkHttpClient.Builder())
                .overrideServerCachePolicy(5, TimeUnit.MINUTES)
                .apply().build();

        //when
        mockWebServer.enqueue(new MockResponse().setBody("Response"));
        Response response = getResponse(client);

        //then
        then(response.header("Cache-Control")).isEqualTo("max-age="+TimeUnit.MINUTES.toSeconds(5));
    }

    @Test
    public void testForceCacheWhenOffline_ENABLED_ONLINE() throws Exception {
        //given
        given(networkMonitor.isOnline()).willReturn(true);
        OkHttpClient client = OkCacheControl.on(new OkHttpClient.Builder())
                .forceCacheWhenOffline(networkMonitor)
                .apply().build();

        //when
        mockWebServer.enqueue(new MockResponse().setBody("Response"));
        Response response = getResponse(client);

        //then
        then(response.request().cacheControl().onlyIfCached()).isFalse();
        then(response.request().cacheControl().maxStaleSeconds()).isNotEqualTo(CacheControl.FORCE_CACHE.maxStaleSeconds());
    }

    @Test
    public void testForceCacheWhenOffline_ENABLED_OFFLINE() throws Exception {
        //given
        given(networkMonitor.isOnline()).willReturn(false);
        OkHttpClient client = OkCacheControl.on(new OkHttpClient.Builder())
                .forceCacheWhenOffline(networkMonitor)
                .apply().build();

        //when
        mockWebServer.enqueue(new MockResponse().setBody("Response"));
        Response response = getResponse(client);

        //then
        then(response.request().cacheControl().onlyIfCached()).isEqualTo(CacheControl.FORCE_CACHE.onlyIfCached());
        then(response.request().cacheControl().maxStaleSeconds()).isEqualTo(CacheControl.FORCE_CACHE.maxStaleSeconds());
    }

    private Response getResponse(OkHttpClient client) throws IOException {
        return client.newCall(new Request.Builder()
                .url(mockWebServer.url("/"))
                .build()).execute();
    }

}