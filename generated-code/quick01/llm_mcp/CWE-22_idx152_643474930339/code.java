import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public static void unzip(File input, File destinationDir) throws IOException {
    try (ZipFile archive = new ZipFile(input)) {
        if (!destinationDir.isDirectory()) {
            FileMisc.mkdirs(destinationDir);
        }
        Path root = destinationDir.toPath().toRealPath();
        if (!Files.isDirectory(root)) {
            throw new IOException("ZIP destination is not a directory: " + root);
        }

        Enumeration<? extends ZipEntry> entries = archive.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            Path entryPath;
            try {
                entryPath = root.getFileSystem().getPath(name);
            } catch (InvalidPathException e) {
                throw new IOException("Invalid ZIP entry path: " + name, e);
            }

            Path target = root.resolve(entryPath).normalize();
            if (name.isEmpty() || entryPath.getRoot() != null || !target.startsWith(root)
                    || (target.equals(root) && !entry.isDirectory())) {
                throw new IOException("Invalid ZIP entry path: " + name);
            }

            Path current = root;
            for (Path component : root.relativize(target)) {
                current = current.resolve(component);
                if (Files.isSymbolicLink(current)) {
                    throw new IOException("ZIP entry path crosses a symbolic link: " + name);
                }
            }

            Path directory = entry.isDirectory() ? target : target.getParent();
            if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
                FileMisc.mkdirs(directory.toFile());
                if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("Could not create ZIP output directory: " + directory);
                }
            }

            if (!entry.isDirectory()) {
                try (InputStream source = archive.getInputStream(entry);
                     OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE,
                             StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE,
                             LinkOption.NOFOLLOW_LINKS)) {
                    copy(source, output);
                }
            }
        }
    }
}
