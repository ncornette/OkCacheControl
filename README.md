# OkCacheControl
Helper class to configure cache behaviour of OkHttp client, also works with Retrofit for Android.

## Usage

Initialize OkCacheControl with `OkHttpClient.Builder()`, then you can call 3 methods : 

 - `overrideServerCachePolicy(MaxAgeControl)`
 - `forceCacheWhenOffline(NetworkMonitor)` 
 - `apply()`

```java
  okClient = OkCacheControl.on(new OkHttpClient.Builder())
          .overrideServerCachePolicy(30, MINUTES)
          .forceCacheWhenOffline(networkMonitor)
          .apply() // returns the OkHttpClient.Builder instance
          .cache(cache)
          .build();

```

## Description

 - `overrideServerCachePolicy(MaxAgeControl)` will override server cache policy with `Cache-Control: max-age` 
 on responses. Cache will be used until expiration even if network is available. Use it when server doesn't 
 implement a cache policy.
 - `forceCacheWhenOffline(NetworkMonitor)` will force the use of cache when no network connection is available. 
 When offline, cache will be used even if expired. for `Android` you can implement a `NetworkMonitor` that returns 
 the value of `ConnectivityManager.getActiveNetworkInfo().isConnected()`.

Call `apply()` to add interceptors to `OkHttpClient.Builder` then it returns the builder so you can continue to build
and add the cache.

## Installation

WIP : build.gradle configuration in progress

## License

    Copyright 2016 Nicolas Cornette

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
