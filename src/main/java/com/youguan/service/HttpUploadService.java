package com.youguan.service;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.youguan.config.ClientConfig;
import com.youguan.config.ServerConfig;
import okhttp3.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class HttpUploadService {
    private static final Logger LOG = Logger.getInstance(HttpUploadService.class);
    private static final MediaType MEDIA_TYPE_AUTO = MediaType.parse("application/octet-stream");
    private final ClientConfig clientConfig;
    private final OkHttpClient client;

    public HttpUploadService(ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        this.client = OkHttpClientUtils.createTrustAllClient()
                .connectTimeout(clientConfig.getTimeout(), TimeUnit.SECONDS)
                .writeTimeout(clientConfig.getTimeout(), TimeUnit.SECONDS)
                .readTimeout(clientConfig.getTimeout(), TimeUnit.SECONDS)
                .build();
    }

    public UploadResult uploadFile(ServerConfig serverConfig, VirtualFile file, String projectBasePath, String targetRootDir) throws IOException {
        byte[] fileContent = ReadAction.compute(() -> {
            try {
                return file.contentsToByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        Path basePath = Paths.get(projectBasePath);
        Path filePath = Paths.get(file.getPath());
        String relativePath = basePath.relativize(filePath).toString();

        String targetFilePath = Paths.get(targetRootDir, relativePath)
                .toString()
                .replace('\\', '/');

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("action", "safe")
                .addFormDataPart("target_file_path", targetFilePath)
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(fileContent, MEDIA_TYPE_AUTO))
                .build();

        Request request = new Request.Builder()
                .url(String.format("%s://%s", clientConfig.getProtocol(), serverConfig.getAddr()))
                .addHeader("Host", serverConfig.getHost().isEmpty() ? serverConfig.getAddr() : serverConfig.getHost())
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = "null";
            if (response.body() != null) {
                if (response.body().contentLength() > 1024) {
                    responseBody = new String(Arrays.copyOfRange(response.body().bytes(), 0, 1024), StandardCharsets.UTF_8);
                } else {
                    responseBody = new String(response.body().bytes(), StandardCharsets.UTF_8);
                }
            }
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
            }

            return new UploadResult(
                    targetFilePath,
                    response.code(),
                    responseBody
            );
        }
    }

    public static class UploadResult {
        private final String targetPath;
        private final int responseCode;
        private final String responseBody;

        public UploadResult(String targetPath, int responseCode, String responseBody) {
            this.targetPath = targetPath;
            this.responseCode = responseCode;
            this.responseBody = responseBody;
        }

        @Override
        public String toString() {
            return String.format("""
                            目标路径: %s
                            响应状态码: %d
                            响应内容: %s""",
                    targetPath, responseCode, responseBody);
        }
    }
}

class OkHttpClientUtils {
    public static OkHttpClient.Builder createTrustAllClient() {
        try {
            // 创建信任所有证书的信任管理器
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            // 安装信任管理器
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // 创建 SSL socket 工厂
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);

            return builder;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create OkHttpClient", e);
        }
    }
}