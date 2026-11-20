import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public static void unzip(File input, File destinationDir) throws IOException {
    try (InputStream archive = new FileInputStream(input);
         ZipInputStream zip = new ZipInputStream(archive)) {
        FileMisc.mkdirs(destinationDir);
        Path root = destinationDir.toPath().toRealPath();
        if (!Files.isDirectory(root)) {
            throw new IOException("ZIP destination is not a directory: " + root);
        }

        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            String name = entry.getName();
            Path relative;
            try {
                relative = root.getFileSystem().getPath(name);
            } catch (InvalidPathException e) {
                throw new IOException("Invalid ZIP entry path: " + name, e);
            }

            Path target = root.resolve(relative).normalize();
            if (name.isEmpty() || relative.getRoot() != null || !target.startsWith(root)
                    || (target.equals(root) && !entry.isDirectory())) {
                throw new IOException("Invalid ZIP entry path: " + name);
            }

            for (Path path = target; !path.equals(root); path = path.getParent()) {
                if (Files.isSymbolicLink(path)) {
                    throw new IOException("ZIP entry resolves through a symbolic link: " + name);
                }
            }

            if (entry.isDirectory()) {
                FileMisc.mkdirs(target.toFile());
            } else {
                FileMisc.mkdirs(target.getParent().toFile());
                try (OutputStream output = Files.newOutputStream(target,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                    copy(zip, output);
                }
            }
            zip.closeEntry();
        }
    }
}
