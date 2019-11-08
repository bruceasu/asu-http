package me.asu.http.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class MimeTypeDetector {
    static ResourceBundle resourceBundle = ResourceBundle.getBundle("mime");

    public static String detect(String fileName) {
        String type = null;
        Path path = Paths.get(fileName);
        try {
            type = Files.probeContentType(path);
        } catch (IOException e) {

        }
        if (Strings.isEmpty(type)) {
            //从最后一个点之后截取字符串
            String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
            type = resourceBundle.getString(suffix);
        }

        return type;

    }
}
