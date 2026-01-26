package org.amalitech.ui.util;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ErrorLogger {
    private static final Path LOG = Path.of(System.getProperty("java.io.tmpdir"), "blogging-navigation.log");

    public static Path log(Throwable t) {
        try {
            if (!Files.exists(LOG)) {
                Files.createFile(LOG);
            }
            try (FileWriter fw = new FileWriter(LOG.toFile(), true);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.println("--- " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + " ---");
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                pw.println(sw.toString());
                pw.println();
                pw.flush();
            }
            return LOG.toAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return LOG.toAbsolutePath();
        }
    }
}
