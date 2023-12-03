package me.yellowstrawberry.rs;

import com.google.common.io.Files;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

public class Settings {

    Yaml yaml = new Yaml();
    File root;
    File sqliteFile;

    int centerX = 0;
    int centerZ = 0;
    int maxX = 29999984;
    int maxZ = 29999984;

    String sqlURL = "localhost:3306";
    String sqlUser = "root";
    String sqlPassword = "root";
    String databaseName = "server";

    boolean canSpawnInWater = false;

    public Settings(File pluginFolder) {
        root = new File(pluginFolder+"/RandomSpawn/");
        if(!root.exists()) root.mkdirs();

        try {
            loadSettings();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadSettings() throws IOException {
        File settingFile = new File(root+"/settings.yaml");
        if(!settingFile.exists()) {
            copySample(Settings.class.getResourceAsStream("/sample.yaml"), settingFile);
            sqliteFile = new File(root+"/server.db");
            if(!sqliteFile.exists()) copySample(Settings.class.getResourceAsStream("/sample.db"), sqliteFile);
        }else {
            Map<String, Object> settings = yaml.load(new FileInputStream(settingFile));

            Map<String, Object> coordinateS = ((Map<String, Object>) settings.get("coordinates"));
            Map<String, Object> sqlS = ((Map<String, Object>) settings.get("sql"));

            if(coordinateS.get("type").toString().equals("custom")) {
                if(coordinateS.containsKey("centerX")) centerX = (Integer) coordinateS.get("centerX");
                if(coordinateS.containsKey("centerZ")) centerZ = (Integer) coordinateS.get("centerZ");
                if(coordinateS.containsKey("maxX")) maxX = (Integer) coordinateS.get("maxX");
                if(coordinateS.containsKey("maxZ")) maxZ = (Integer) coordinateS.get("maxZ");
                if(coordinateS.containsKey("canSpawnInWater")) canSpawnInWater = Boolean.parseBoolean(coordinateS.get("canSpawnInWater").toString());
            }

            if(sqlS.get("type").toString().equals("sqlite")) {
                sqliteFile = new File(root+"/server.db");
                if(!sqliteFile.exists()) copySample(Settings.class.getResourceAsStream("/sample.db"), sqliteFile);
            }else if(sqlS.get("type").toString().equals("mariadb")) {
                sqliteFile = null;

                if(coordinateS.containsKey("databaseName")) databaseName = coordinateS.get("databaseName").toString();
                if(coordinateS.containsKey("url")) sqlURL = coordinateS.get("url").toString();
                if(coordinateS.containsKey("user")) sqlUser = coordinateS.get("user").toString();
                if(coordinateS.containsKey("password")) sqlPassword = coordinateS.get("password").toString();
            }
        }
    }

    public void copySample(InputStream ipt, File file) throws IOException {
        FileOutputStream opt = new FileOutputStream(file);

        byte[] b = new byte[1024];

        while (ipt.read(b) != -1) {
            opt.write(b);
        }
    }
}
