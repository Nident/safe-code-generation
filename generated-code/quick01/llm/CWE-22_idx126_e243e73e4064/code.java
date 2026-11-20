import java.io.File;
import java.nio.file.Path;

static public File allocateFile(File dir, String name) {
    int queryStart = name.indexOf('?', 1);
    if (queryStart >= 0) {
        name = name.substring(0, queryStart);
    }

    Path relativePath = new File(name).toPath().normalize();
    if (relativePath.isAbsolute() || relativePath.startsWith("..") || relativePath.toString().isEmpty()) {
        throw new IllegalArgumentException("Name must identify a file below the supplied directory");
    }

    File candidate = new File(dir, relativePath.toString());
    File parent = candidate.getParentFile();
    String fileName = candidate.getName();
    int extensionStart = fileName.indexOf('.');
    String stem = extensionStart < 0 ? fileName : fileName.substring(0, extensionStart);
    String extension = extensionStart < 0 ? "" : fileName.substring(extensionStart);

    for (long suffix = 2; candidate.exists(); suffix++) {
        candidate = new File(parent, stem + "-" + suffix + extension);
    }

    if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
        throw new IllegalStateException("Could not create directory " + parent);
    }
    return candidate;
}
