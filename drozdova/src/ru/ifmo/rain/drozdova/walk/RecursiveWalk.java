package ru.ifmo.rain.drozdova.walk;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveWalk {
    private final Path inputPath;
    private final Path outputPath;

    RecursiveWalk(final String inputPath, final String outputPath) throws RecursiveWalkException {
        try {
            this.inputPath = Paths.get(inputPath);
            this.outputPath = Paths.get(outputPath);
            final Path parent = this.outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (InvalidPathException | IOException e) {
            throw new RecursiveWalkException(e.getMessage());
        }
    }

    public void walk() throws RecursiveWalkException {
        try (BufferedReader reader = Files.newBufferedReader(inputPath)) {
            try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
                String path;
                FileVisitor<Path> myFileVisitor = new FileVisitor<>() {
                    private final static int START = 0x811c9dc5;
                    private final static int FNV_32_PRIME = 0x01000193;
                    private final static int BYTE = (1 << 8) - 1;
                    private final static int BUFF_SIZE = 4096;
                    private final byte[] buffer = new byte[BUFF_SIZE];

                    @Override
                    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                        int hash = START;
                        InputStream reader;
                        try {
                            reader = Files.newInputStream(path);
                            int cnt;
                            while ((cnt = reader.read(buffer)) != -1) {
                                for (int i = 0; i < cnt; i++) {
                                    hash = (hash * FNV_32_PRIME) ^ (buffer[i] & BYTE);
                                }
                            }
                        } catch (IOException e) {
                            hash = 0;
                        }
                        writeHash(path, hash, writer);
                        return FileVisitResult.CONTINUE;
                    }

                    private void writeHash(Path path, int hash, BufferedWriter writer) throws IOException {
                        writer.write(String.format("%08x", hash) + " " + path.toString() + "\n");
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
                        writeHash(path, 0, writer);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
                        if (e == null) {
                            return FileVisitResult.CONTINUE;
                        } else {
                            throw e;
                        }
                    }
                };
                while ((path = reader.readLine()) != null) {
                    try {
                        Files.walkFileTree(Paths.get(path), myFileVisitor);
                    } catch (InvalidPathException e) {
                        writer.write("00000000 " + path + "\n");
                    }
                }
            }
        } catch (IOException e) {
            throw new RecursiveWalkException(e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            if (args == null || args.length < 2 || args[0] == null || args[1] == null) {
                throw new RecursiveWalkException("illegal arguments");
            }
            RecursiveWalk walker = new RecursiveWalk(args[0], args[1]);
            walker.walk();
        } catch (RecursiveWalkException e) {
            System.err.println(e.getMessage());
        }
    }
}
