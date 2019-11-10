package me.asu.http.server;

import java.io.*;
import java.net.URL;
import lombok.extern.slf4j.Slf4j;
import me.asu.http.util.ApplicationConf;
import me.asu.http.util.Strings;


/**
 * @author suk
 * @since 2018/11/30
 */
@Slf4j
public class GlobalVariables {

    public static ApplicationConf APPLICATION_CONF = new ApplicationConf();
    public static int             HTTP_NODE_PORT   = 0;

    public static void loadConfigFile(String configFilePath) throws IOException {
        if (Strings.isNotBlank(configFilePath)) {
            File configFile = new File(configFilePath);
            if (configFile.isFile()) {
                log.info("using config file: {}", configFile);
                try (FileReader fileReader = new FileReader(configFile)) {
                    GlobalVariables.APPLICATION_CONF.load(fileReader);
                }
            } else {
                log.warn("There's no config file, using default value.");
                loadDefaultConfigFile();
            }
        } else {
            loadDefaultConfigFile();
        }
    }

    public static void loadDefaultConfigFile() throws IOException {
        File configFile = new File("application.properties");
        if (!configFile.isFile()) {
            configFile = new File("config", "application.properties");
        }
        if (!configFile.isFile()) {
            // try classpath
            URL resource = GlobalVariables.class.getClassLoader()
                                                .getResource("application.properties");
            if (resource != null) {
                log.info("using config file: {}", resource);
                try (InputStream stream = resource.openStream()) {
                    APPLICATION_CONF.load(new InputStreamReader(stream, "utf-8"));
                }
            } else {
                log.warn("There's no config file, using default value.");
            }
        } else {
            log.info("using config file: {}", configFile);
            try (FileReader fileReader = new FileReader(configFile)) {
                APPLICATION_CONF.load(fileReader);
            }
        }
    }


}
