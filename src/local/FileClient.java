package local;

import model.ApplicationContext;
import model.Logger;
import model.Module;
import production.Config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class FileClient implements Module {

    private final ApplicationContext ctx;

    public FileClient(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    public void write(Path path, String text) {
        write(path, text.getBytes());
    }

    public void write(String path, String text) {
        write(path, text.getBytes());
    }

    public void write(String path, byte[] data) {
        write(Config.fsRoot.resolve(path), data);
    }

    public void write(Path path, byte[] data) {
        try {
            Files.write(path, data);
        } catch (Exception e) {
            var logger = ctx.resolve(Logger.class);
            var node = ctx.resolve(Node.class);
            logger.error("error writing file: " + e, node.info());
            throw new RuntimeException("file writing failed");
        }
    }

    public List<String> readAllLines(String filename) {
        try {
            return Files.readAllLines(Config.fsRoot.resolve(filename));
        } catch (Exception e) {
            var logger = ctx.resolve(Logger.class);
            var node = ctx.resolve(Node.class);
            logger.error("error reading file: " + e, node.info());
            throw new RuntimeException("file reading failed");
        }
    }

    public long lastModified(String path) {
        try {
            BasicFileAttributes attr = Files.readAttributes(Config.fsRoot.resolve(path), BasicFileAttributes.class);
            return attr.lastModifiedTime().to(TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            var logger = ctx.resolve(Logger.class);
            var node = ctx.resolve(Node.class);
            logger.error("error reading file attributes: " + e, node.info());
            throw new RuntimeException("file attrib reading failed");
        }
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(Logger.class, Node.class);
    }

    @Override
    public Collection<Class<? extends Module>> providers() {
        return Set.of(FileClient.class);
    }
}
