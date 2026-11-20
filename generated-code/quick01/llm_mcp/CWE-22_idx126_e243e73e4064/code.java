import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

static public File allocateFile(File dir, String name) {
    int queryStart = name.indexOf('?', 1);
    if (queryStart >= 0) {
        name = name.substring(0, queryStart);
    }

    Path relative = new File(name).toPath();
    if (relative.getRoot() != null) {
        throw new IllegalArgumentException("File name must be relative to the allocation directory");
    }

    try {
        Path base = dir.getCanonicalFile().toPath();
        Path requested = base.resolve(relative).normalize();
        if (requested.equals(base) || !requested.startsWith(base)) {
            throw new IllegalArgumentException("File path must be below the allocation directory");
        }

        Path parent = requested.getParent().toFile().getCanonicalFile().toPath();
        if (!parent.startsWith(base)) {
            throw new IllegalArgumentException("Parent directory resolves outside the allocation directory");
        }

        String fileName = requested.getFileName().toString();
        int extensionStart = fileName.indexOf('.');
        if (extensionStart < 0) {
            extensionStart = fileName.length();
        }
        String stem = fileName.substring(0, extensionStart);
        String extension = fileName.substring(extensionStart);

        Path candidate = parent.resolve(fileName);
        for (long suffix = 2; ; suffix++) {
            try {
                Files.readAttributes(candidate, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            } catch (NoSuchFileException unused) {
                Files.createDirectories(parent);
                return candidate.toFile();
            }
            candidate = parent.resolve(stem + "-" + suffix + extension);
        }
    } catch (IOException e) {
        throw new UncheckedIOException("Could not allocate a file path under " + dir, e);
    }
}
