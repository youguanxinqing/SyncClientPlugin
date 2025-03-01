package com.youguan.config;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;

public class ConfigManager {
    private static final Logger LOG = Logger.getInstance(ConfigManager.class);
    private static final String CONFIG_FILE_NAME = ".sync-client.toml";
    
    public static File getConfigFile(Project project) {
        if (project == null) {
            return null;
        }
        return new File(project.getBasePath(), CONFIG_FILE_NAME);
    }
    
    public static boolean configExists(Project project) {
        File configFile = getConfigFile(project);
        return configFile != null && configFile.exists();
    }
    
    public static String readConfig(Project project) throws IOException {
        File configFile = getConfigFile(project);
        if (configFile == null || !configFile.exists()) {
            throw new IOException("配置文件不存在");
        }
        return Files.readString(configFile.toPath());
    }
}

