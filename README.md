# OkCacheControl

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/b566a6e4564e4393b78b7b4b36a70452)](https://www.codacy.com/app/nicolas-cornette/OkCacheControl?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ncornette/OkCacheControl&amp;utm_campaign=Badge_Grade)
[![Build Status](https://travis-ci.org/ncornette/OkCacheControl.svg?branch=master)](https://travis-ci.org/ncornette/OkCacheControl)
[![Bintray](https://img.shields.io/bintray/v/ncornette/maven/okcache-control.svg?maxAge=2592000)]()
[![codecov](https://codecov.io/gh/ncornette/OkCacheControl/branch/master/graph/badge.svg)](https://codecov.io/gh/ncornette/OkCacheControl)

Helper class to configure cache behaviour of OkHttp client, also works 
with Retrofit for Android.

Release version is available on jcenter: 
```groovy
repositories {
    jcenter()
}
dependencies {
    compile 'com.ncornette.cache:okcache-control:1.0.0'
}
```

## Usage

Initialize `OkCacheControl` with *OkHttpClient.Builder()*, then you can 
call 2 extra methods : 

 - `overrideServerCachePolicy(MaxAgeControl)`
 - `forceCacheWhenOffline(NetworkMonitor)` 

```java
  okClient = OkCacheControl.on(new OkHttpClient.Builder())
          .overrideServerCachePolicy(30, MINUTES)
          .forceCacheWhenOffline(networkMonitor)
          .apply() // return to the OkHttpClient.Builder instance
          .cache(cache)
          .build();

```

## Description


 - `overrideServerCachePolicy(MaxAgeControl)` will override server cache policy
 on responses with *Cache-Control: max-age*. Cache will be used until expiration 
 even if network is available. Use it when server doesn't implement a cache policy!
 
 - `forceCacheWhenOffline(NetworkMonitor)` will force the use of cache when 
 no network connection is available. When offline, cache will be used even 
 if expired. for Android you can implement a *NetworkMonitor* that returns 
 the value of *ConnectivityManager.getActiveNetworkInfo().isConnected()*.

Call `apply()` to add interceptors to *OkHttpClient.Builder* and return the 
builder so you can continue to build *OkHttpClient* and add the cache.

### Per request cache control 

without `Cache-Control` header, cache is used until expiration, then network. 
(with `forceCacheWhenOffline()` expired cache will be used when offline)

- add `Cache-Control: no-cache` to always use network, cache will be used 
only when offline.

- add `Cache-Control: no-store` to not store response in cache.

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
