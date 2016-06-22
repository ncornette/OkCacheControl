package com.ncornette.cache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by nic on 20/06/16.
 */
public class OkCacheControlTest {

    private MockWebServer mockWebServer;
    private Cache cache;
    private OkHttpClient client;
    private OkCacheControl.NetworkMonitor networkMonitor;
    private OkCacheControl.MaxAgeControl maxAgeControl;

    @Before
    public void setUp() throws Exception {
        mockWebServer = new MockWebServer(); mockWebServer.start();

        cache = new Cache(new File("./build/tmp/test-ok-cache"), 10 * 1024 * 1024);
        cache.evictAll();

        networkMonitor = mock(OkCacheControl.NetworkMonitor.class);
        when(networkMonitor.isOnline()).thenReturn(true);

        maxAgeControl = mock(OkCacheControl.MaxAgeControl.class);
        when(maxAgeControl.getMaxAge()).thenReturn(0L);

        client = OkCacheControl.on(new OkHttpClient.Builder())
                .overrideServerCachePolicy(maxAgeControl)
                .forceCacheWhenOffline(networkMonitor)
                .apply()
                .cache(cache)
                .build();
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    private void givenResponseInCache(String responseBody, long maxAge, TimeUnit maxAgeUnit) throws Exception {
        when(networkMonitor.isOnline()).thenReturn(true);
        when(maxAgeControl.getMaxAge()).thenReturn(maxAgeUnit.toSeconds(maxAge));

        mockWebServer.enqueue(new MockResponse().setBody(responseBody));
        Response response = getResult();

        assertThat(cache.networkCount()).isEqualTo(1);
        assertThat(cache.hitCount()).isEqualTo(0);
        assertThat(response.body().string()).isEqualTo(responseBody);
        assertThat(cache.size()).isGreaterThan(0);
    }

    @Test
    public void test_CACHE_IS_USED() throws Exception {
        //given
        givenResponseInCache("Cached Response", 5, MINUTES);
        given(networkMonitor.isOnline()).willReturn(true);

        //when
        mockWebServer.enqueue(new MockResponse().setBody("Network Response"));
        Response response = getResult();

        //then
        then(response.body().string()).isEqualTo("Cached Response");
        then(cache.hitCount()).isEqualTo(1);
    }

    private Response getResult() throws IOException {
        return client.newCall(new Request.Builder()
                .url(mockWebServer.url("/"))
                .build()).execute();
    }

    private Response getResultFromNetwork() throws IOException {
        return client.newCall(new Request.Builder()
                .url(mockWebServer.url("/"))
                .header("Cache-Control", "no-cache")
                .build()).execute();
    }

    private Response getResultNoStore() throws IOException {
        return client.newCall(new Request.Builder()
                .url(mockWebServer.url("/"))
                .header("Cache-Control", "no-store")
                .build()).execute();
    }

    @Test
    public void test_FORCE_NETWORK() throws Exception {
        //given
        givenResponseInCache("Cached Response", 5, MINUTES);
        given(networkMonitor.isOnline()).willReturn(true);

        //when
        //force network response
        mockWebServer.enqueue(new MockResponse().setBody("Network Response"));
        Response response = getResultFromNetwork();

        //then
        then(response.body().string()).isEqualTo("Network Response");
        then(cache.hitCount()).isEqualTo(0);
    }

    @Test
    public void test_CACHE_EXPIRED() throws Exception {
        //given
        givenResponseInCache("Expired Response", -5, MINUTES);
        given(networkMonitor.isOnline()).willReturn(true);

        //when
        mockWebServer.enqueue(new MockResponse().setBody("Network Response"));
        Response response = getResult();

        //then
        then(response.body().string()).isEqualTo("Network Response");
        then(cache.hitCount()).isEqualTo(0);
    }

    @Test
    public void test_NO_STORE() throws Exception {
        //given
        given(networkMonitor.isOnline()).willReturn(true);

        //when
        mockWebServer.enqueue(new MockResponse().setBody("Network Response"));
        Response response = getResultNoStore();

        //then
        then(response.body().string()).isEqualTo("Network Response");
        assertThat(cache.size()).isZero();
    }

    @Test
    public void test_CACHE_EXPIRED_NO_NETWORK() throws Exception {
        //given
        givenResponseInCache("Expired Response", -5, MINUTES);
        given(networkMonitor.isOnline()).willReturn(false);

        //when
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));
        Response response = getResult();

        //then
        then(response.body().string()).isEqualTo("Expired Response");
        then(cache.hitCount()).isEqualTo(1);
    }

    @Test
    public void test_FORCE_NETWORK_NO_NETWORK() throws Exception {
        //given
        givenResponseInCache("Expired Response", -5, MINUTES);
        given(networkMonitor.isOnline()).willReturn(false);

        //when
        //force network response
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));
        Response response = getResultFromNetwork();

        //then
        then(response.body().string()).isEqualTo("Expired Response");
        then(cache.hitCount()).isEqualTo(1);
    }

    @Test
    public void test_NO_CACHE_NO_NETWORK() throws Exception {
        //given
        given(networkMonitor.isOnline()).willReturn(false);

        //when
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));
        Response response = getResult();

        //then
        then(response.body().string()).isEmpty();
        then(cache.hitCount()).isEqualTo(0);
        then(cache.networkCount()).isEqualTo(0);
    }

}
