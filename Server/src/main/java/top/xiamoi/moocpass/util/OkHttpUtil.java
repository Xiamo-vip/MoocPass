package top.xiamoi.moocpass.util;

import okhttp3.*;
import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp 通用工具类
 * 解决 TLS / SSLHandshakeException 握手异常，提供网络波动自动重试机制与全局 Cookie 管理
 */
public class OkHttpUtil {

    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    /**
     * 创建一个忽略 SSL 校验且支持自动重试与 Cookie 管理的 OkHttpClient.Builder
     */
    public static OkHttpClient.Builder createUnsafeClientBuilder() {
        try {
            X509TrustManager trustAllCertsManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustAllCertsManager}, new SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, trustAllCertsManager)
                    .hostnameVerifier((hostname, session) -> true)
                    .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT))
                    .retryOnConnectionFailure(true)
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        Request originalRequest = chain.request();
                        Request.Builder builder = originalRequest.newBuilder();
                        if (originalRequest.header("User-Agent") == null) {
                            builder.header("User-Agent", DEFAULT_USER_AGENT);
                        }
                        Request req = builder.build();
                        
                        int maxRetries = 3;
                        int attempt = 0;
                        while (true) {
                            try {
                                return chain.proceed(req);
                            } catch (Exception e) {
                                attempt++;
                                if (attempt > maxRetries) {
                                    throw e;
                                }
                                try {
                                    Thread.sleep(attempt * 1500L);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    throw e;
                                }
                            }
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException("初始化 OkHttp 忽略 SSL 异常", e);
        }
    }

    /**
     * 创建内存 CookieJar 实例
     */
    public static CookieJar createInMemoryCookieJar() {
        return new CookieJar() {
            private final List<Cookie> cookieStore = new ArrayList<>();

            @Override
            public synchronized void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                for (Cookie newCookie : cookies) {
                    cookieStore.removeIf(c -> c.name().equalsIgnoreCase(newCookie.name()) 
                            && matchesDomain(url.host(), c.domain()));
                    cookieStore.add(newCookie);
                }
            }

            @Override
            public synchronized List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> matchingCookies = new ArrayList<>();
                List<Cookie> expiredCookies = new ArrayList<>();
                long now = System.currentTimeMillis();

                for (Cookie cookie : cookieStore) {
                    if (cookie.expiresAt() < now) {
                        expiredCookies.add(cookie);
                    } else if (cookie.matches(url) || matchesDomain(url.host(), cookie.domain())) {
                        matchingCookies.add(cookie);
                    }
                }
                cookieStore.removeAll(expiredCookies);
                return matchingCookies;
            }

            private boolean matchesDomain(String host, String cookieDomain) {
                if (host.equals(cookieDomain)) return true;
                if (cookieDomain.startsWith(".")) {
                    return host.endsWith(cookieDomain) || host.equals(cookieDomain.substring(1));
                }
                return false;
            }
        };
    }
}
