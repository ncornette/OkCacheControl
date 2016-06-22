# OkCacheControl
Helper class to configure cache behaviour of OkHttp client, also works with Retrofit for Android.

### Usage

Initialize OkCacheControl with `OkHttpClient.Builder()`, then you can call 3 methods : 
 - `overrideServerCachePolicy(MaxAgeControl)`
 - `forceCacheWhenOffline(NetworkMonitor)` 
 - `apply()`

```java
  okClient = OkCacheControl.on(new OkHttpClient.Builder())
          .overrideServerCachePolicy(30, MINUTES)
          .forceCacheWhenOffline(networkMonitor)
          .apply()
          .cache(cache)
          .build();

```

### Description

 - `overrideServerCachePolicy(MaxAgeControl)` will override server cache policy with `Cache-Control: max-age` 
 on responses. Cache will be used until expiration even if network is available. Use it when server doesn't 
 implement a cache policy.
 - `forceCacheWhenOffline(NetworkMonitor)` will force the use of cache when no network connection is available. 
 When offline, cache will be used even if expired. for `Android` you can implement a `NetworkMonitor` that returns 
 the value of `ConnectivityManager.getActiveNetworkInfo().isConnected()`.

Call `apply()` to add interceptors to `OkHttpClient.Builder` then it returns the builder so you can continue to build
and add the cache.

### Installation

WIP : build.gradle configuration in progress
