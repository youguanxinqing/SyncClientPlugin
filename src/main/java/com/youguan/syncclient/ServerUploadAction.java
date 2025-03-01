package com.youguan.syncclient;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.youguan.config.ClientConfig;
import com.youguan.config.ServerConfig;
import com.youguan.service.HttpUploadService;
import com.youguan.service.HttpUploadService.UploadResult;
import org.jetbrains.annotations.NotNull;

public class ServerUploadAction extends AnAction {
    private final ServerConfig serverConfig;
    private final String targetRootDir;
    private final ClientConfig clientConfig;

    public ServerUploadAction(ServerConfig serverConfig, String targetRootDir, ClientConfig clientConfig) {
        super(serverConfig.getName());
        this.serverConfig = serverConfig;
        this.targetRootDir = targetRootDir;
        this.clientConfig = clientConfig;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null) {
            Messages.showErrorDialog("No file selected", "Error");
            return;
        }
        if (file.isDirectory()) {
            Messages.showErrorDialog("Not support directory", "Error");
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(e.getProject(), "Uploading File") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setIndeterminate(true);
                    indicator.setText("Uploading " + file.getName() + " to " + serverConfig.getName());

                    UploadResult result = new HttpUploadService(clientConfig).uploadFile(
                            serverConfig,
                            file,
                            e.getProject().getBasePath(),
                            targetRootDir
                    );

                    ApplicationManager.getApplication().invokeLater(() -> {
                        NotificationGroupManager.getInstance()
                                .getNotificationGroup("SyncClient Upload")
                                .createNotification(
                                        "Upload Success",
                                        result.toString(),
                                        NotificationType.INFORMATION
                                )
                                .notify(e.getProject());
                    });
                } catch (Exception ex) {
                    final String errorMessage = String.format("""
                            文件路径: %s
                            失败原因: %s""", file.getPath(), ex.getMessage());
                    ApplicationManager.getApplication().invokeLater(() -> {
                        NotificationGroupManager.getInstance()
                                .getNotificationGroup("SyncClient Upload")
                                .createNotification(
                                        "Upload Failed",
                                        errorMessage,
                                        NotificationType.ERROR
                                )
                                .notify(e.getProject());
                    });
                }
            }
        });
    }
}