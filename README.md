## Kash

[![](https://jitpack.io/v/suransea/kash.svg?style=flat-square)](https://jitpack.io/#suransea/kash)

##### 磁盘缓存工具


#### Maven

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


#### Gradle

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
	
#### 使用


```java
        DiskCache cache = new DiskCache.Builder()
                .name("sample") //缓存名称
                .path("./cache") //缓存根目录
                .enableMemoryCache() //是否启用内存缓存
                .maxMemoryCacheCount(5) //内存缓存数量限制
                .maxMemoryCacheSingleSize(1024) //内存缓存单值大小限制
                .build();

        cache.put("name", "Alice"); //缓存字符串
        cache.put("age", 10); //缓存基本类型

        User user = new User();
        user.setAge(10);
        user.setName("Alice");
        cache.put("user", user); //缓存POJO

        System.out.println(cache.getString("name"));
        System.out.println(cache.get("age", Integer.class));
        System.out.println(cache.get("user", User.class));

        long expiredTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1);
        cache.put("key", "content", CacheOption.expiredTime(expiredTime)); //设置过期时间

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
                }).build(); //自定义对象序列化
```