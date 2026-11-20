@Override
    public void putObject(String objectName, String contentType, Map<String, String> metadata, byte[] objectData)
            throws IOException {
        // TODO: do something with metadata

        if (objectName.endsWith("/")) {
            Path path = root.resolve(objectName);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            } else if (!Files.isDirectory(path)) {
                throw new IOException("Object path is already in use");
            }
        } else {
            // Current implementation ignores specified contentType, instead deriving
            // MIME type from the filename extension.

            Path path = root.resolve(objectName);
            boolean fileExists = Files.isRegularFile(path);

            // Verify limits
            AtomicLong size = new AtomicLong(fileExists ? -Files.size(path) : 0);
            AtomicInteger count = new AtomicInteger(fileExists ? -1 : 0);
            Set<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
            Files.walkFileTree(root, opts, Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    size.addAndGet(attrs.size());
                    count.incrementAndGet();
                    return FileVisitResult.CONTINUE;
                }
            });

            long newSize = size.get() + objectData.length;
            if (newSize > maxSize) {
                throw new IOException("Maximum bucket size " + maxSize + " exceeded");
            }

            int newCount = count.get() + 1;
            if (newCount > maxObjects) {
                throw new IOException(
                        "Maximum number of objects in the bucket " + newCount + " exceeded");
            }

            if (!Files.exists(path.getParent())) { // Check to avoid error when the parent is a symlink
                Files.createDirectories(path.getParent());
            }
            Files.write(path, objectData);
        }
    }