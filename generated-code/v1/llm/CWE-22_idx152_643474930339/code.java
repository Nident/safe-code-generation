import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public static void unzip(File input, File destinationDir) throws IOException {
    File root = destinationDir.getCanonicalFile();
    try (ZipFile archive = new ZipFile(input)) {
        FileMisc.mkdirs(root);
        Enumeration<? extends ZipEntry> entries = archive.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            File target = new File(root, name).getCanonicalFile();
            if (new File(name).isAbsolute() || !target.toPath().startsWith(root.toPath())
                    || (target.equals(root) && !entry.isDirectory())) {
                throw new IOException("ZIP entry is outside the destination or names it as a file: " + name);
            }

            if (entry.isDirectory()) {
                FileMisc.mkdirs(target);
            } else {
                FileMisc.mkdirs(target.getParentFile());
                try (InputStream source = archive.getInputStream(entry);
                     OutputStream output = new FileOutputStream(target)) {
                    copy(source, output);
                }
            }
        }
    }
}
