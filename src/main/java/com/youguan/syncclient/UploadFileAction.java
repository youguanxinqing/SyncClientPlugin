package com.youguan.syncclient;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.youguan.config.ConfigManager;

public class UploadFileAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            Messages.showErrorDialog(
                "无法获取项目信息",
                "错误"
            );
            return;
        }

        // 检查配置文件是否存在
        if (!ConfigManager.configExists(project)) {
            Messages.showErrorDialog(
                "项目根目录下不存在 .sync-client.toml 配置文件",
                "配置文件缺失"
            );
            return;
        }

        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (virtualFile != null) {
            try {
                String fileContent = new String(virtualFile.contentsToByteArray());
                String configContent = ConfigManager.readConfig(project);
                
                String message = String.format(
                    "文件路径: %s\n\n" +
                    "配置文件内容:\n%s\n\n" +
                    "当前文件内容:\n%s",
                    virtualFile.getPath(),
                    configContent,
                    fileContent
                );
                
                Messages.showMessageDialog(
                    project,
                    message,
                    "文件信息",
                    Messages.getInformationIcon()
                );
            } catch (Exception ex) {
                Messages.showErrorDialog(
                    "操作失败: " + ex.getMessage(),
                    "错误"
                );
            }
        } else {
            Messages.showMessageDialog(
                project,
                "没有选中文件",
                "错误",
                Messages.getErrorIcon()
            );
        }
    }
}