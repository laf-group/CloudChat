package dev.laf.cloudchat;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class CloudChatConfig {

    private final Path file;

    private String partGlobal;
    private String partServer;
    private String partPlayer;
    private String messageFormat;
    private String permUse;
    private String permReload;

    public CloudChatConfig(Path dataDirectory) {
        this.file = dataDirectory.resolve("config.yml");
    }

    @SuppressWarnings("unchecked")
    public void load() throws IOException {
        // Copy default config out of the jar on first run
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in == null) {
                    throw new IOException("Default config.yml is missing from the jar!");
                }
                Files.copy(in, file);
            }
        }

        Yaml yaml = new Yaml();
        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(file)) {
            root = yaml.load(in);
        }
        if (root == null) {
            throw new IOException("config.yml is empty or malformed.");
        }

        Map<String, Object> parts = (Map<String, Object>) root.get("parts");
        if (parts == null) throw new IOException("Missing 'parts' section in config.yml.");
        this.partGlobal = String.valueOf(parts.get("global"));
        this.partServer = String.valueOf(parts.get("server"));
        this.partPlayer = String.valueOf(parts.get("player"));

        Object msg = root.get("message");
        if (msg == null) throw new IOException("Missing 'message' in config.yml.");
        this.messageFormat = String.valueOf(msg);

        Map<String, Object> perms = (Map<String, Object>) root.get("permissions");
        if (perms == null) throw new IOException("Missing 'permissions' section in config.yml.");
        this.permUse = String.valueOf(perms.get("use"));
        this.permReload = String.valueOf(perms.get("reload"));
    }

    public String getPartGlobal()     { return partGlobal; }
    public String getPartServer()     { return partServer; }
    public String getPartPlayer()     { return partPlayer; }
    public String getMessageFormat()  { return messageFormat; }
    public String getPermUse()        { return permUse; }
    public String getPermReload()     { return permReload; }
}