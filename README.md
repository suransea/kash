# Kash

[![](https://jitpack.io/v/suransea/kash.svg?style=flat-square)](https://jitpack.io/#suransea/kash)

A simple disk cache.

## Maven

Step 1. Add the JitPack repository to your build file

	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>

Step 2. Add the dependency

	<dependency>
	    <groupId>com.github.suransea</groupId>
	    <artifactId>kash</artifactId>
	    <version>x.y.z</version>
	</dependency>


## Gradle

Step 1. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

Step 2. Add the dependency

	dependencies {
	        implementation 'com.github.suransea:kash:x.y.z'
	}
	
## Usage

```java
        DiskCache cache = new DiskCache.Builder()
                .name("sample")
                .path("./cache")
                .enableMemoryCache()
                .maxMemoryCacheCount(5)
                .maxMemoryCacheSingleSize(1024)
                .build();

        cache.put("name", "Alice"); // String
        cache.put("age", 10); // Primitive types

        User user = new User();
        user.setAge(10);
        user.setName("Alice");
        cache.put("user", user); // POJO

        System.out.println(cache.getString("name"));
        System.out.println(cache.get("age", Integer.class));
        System.out.println(cache.get("user", User.class));

        // Setting the expired time
        long expiredTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1);
        cache.put("key", "content", new CacheOption().expiredTime(expiredTime));

        // Customize the object serializer
        DiskCache anotherCache = new DiskCache.Builder()
                .serializer(new Serializer() {
                    @Override
                    public <T> T decode(byte[] bytes, Type type) {
                        return new Gson().fromJson(new String(bytes, StandardCharsets.UTF_8), type);
                    }

                    @Override
                    public byte[] encode(Object object) {
                        return new Gson().toJson(object).getBytes(StandardCharsets.UTF_8);
                    }
                }).build();
```
