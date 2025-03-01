package com.youguan.syncclient;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.moandjiezana.toml.Toml;
import com.youguan.config.ClientConfig;
import com.youguan.config.ConfigManager;
import com.youguan.config.ServerConfig;

import java.util.ArrayList;
import java.util.List;

public class UploadFileActionGroup extends ActionGroup {
    @Override
    public AnAction[] getChildren(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || !ConfigManager.configExists(project)) {
            return new AnAction[]{new NoConfigAction()};
        }

        try {
            String configContent = ConfigManager.readConfig(project);
            Toml toml = new Toml().read(configContent);
            List<ServerConfig> servers = new ArrayList<>();

            // 解析 TOML 配置中的 servers
            List<Toml> serverConfigs = toml.getTables("remote.servers");
            for (Toml serverConfig : serverConfigs) {
                ServerConfig server = new ServerConfig();
                server.setName(serverConfig.getString("name"));
                server.setAddr(serverConfig.getString("addr"));
                server.setHost(serverConfig.getString("host", ""));
                servers.add(server);
            }

            // 在 getChildren 方法中修改创建 ServerUploadAction 的部分
            String targetRootDir = toml.getString("remote.target_root_dir");
            ClientConfig clientConfig = new ClientConfig(toml);
            return servers.stream()
                    .map(serverConfig -> new ServerUploadAction(serverConfig, targetRootDir, clientConfig))
                    .toArray(AnAction[]::new);

        } catch (Exception ex) {
            return new AnAction[]{new ErrorAction(ex.getMessage())};
        }
    }

    private static class NoConfigAction extends AnAction {
        NoConfigAction() {
            super("配置文件不存在");
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            // 处理配置文件不存在的情况
        }
    }

    private static class ErrorAction extends AnAction {
        ErrorAction(String error) {
            super("错误: " + error);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            // 处理错误情况
        }
    }
}