package com.youguan.service;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.moandjiezana.toml.Toml;
import okhttp3.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HttpUploadService {
    private static final Logger LOG = Logger.getInstance(HttpUploadService.class);
    private static final MediaType MEDIA_TYPE_AUTO = MediaType.parse("application/octet-stream");
    private static final OkHttpClient client = new OkHttpClient();

    public static UploadResult uploadFile(String serverAddr, VirtualFile file, String projectBasePath, String targetRootDir, String protocol) throws IOException {
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
                .url(protocol + "://" + serverAddr)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = "";
            if (response.body() != null) {
                responseBody = new String(response.body().bytes(), StandardCharsets.UTF_8);
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