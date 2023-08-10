package v2.shared.integration;

import v2.core.log.Logger;
import v2.core.context.Context;
import v2.core.context.Module;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FileClient implements Module {

    private Config config;
    private Logger logger;

    @Override
    public String info() {
        return String.format("File Client @ %s", config.root());
    }

    @Override
    public void build(Context ctx) {
        config = ctx.resolve(Config.class);
        logger = ctx.resolve(Logger.class);
    }

    @Override
    public void deploy() {
        create("");
    }

    public Path create(String dir) {
        Path path = config.root().resolve(dir);
        File file = path.toFile();
        if (!file.exists() && !file.mkdirs()) {
            logger.warn("unable to create directory " + dir, this);
        }
        return path;
    }

    public Path resolve(Path subPath) {
        return config.root().resolve(subPath);
    }

    public Path resolve(String filename) {
        return config.root().resolve(filename);
    }

    public void write(Path path, String text) {
        write(path, text.getBytes());
    }

    public void write(String path, String text) {
        write(path, text.getBytes());
    }

    public void write(String path, byte[] data) {
        write(resolve(path), data);
    }

    public void write(Path path, byte[] data) {
        logger.debug(String.format("writing to file %s: %s", path, data.length > 50? String.format("(%d bytes)", data.length) : new String(data)), this);
        try {
            Files.write(path, data);
        } catch (Exception e) {
            logger.exception(e, this);
        }
    }

    public FileWriter open(String path) {
        return open(resolve(path));
    }

    public FileWriter open(Path path) {
        logger.debug(String.format("opening file %s", path), this);
        try {
            return new FileWriter(path.toFile());
        } catch (Exception e) {
            logger.exception(e, this);
            throw new RuntimeException("file opening failed");
        }
    }

    public void close(FileWriter writer) {
        logger.debug("closing file writer", this);
        try {
            writer.close();
        } catch (Exception e) {
            logger.exception(e, this);
        }
    }

    public List<String> readAllLines(String filename) {
        logger.debug(String.format("reading file %s", filename), this);
        try {
            return Files.readAllLines(resolve(filename));
        } catch (Exception e) {
            logger.exception(e, this);
            throw new RuntimeException("file reading failed");
        }
    }

    public long lastModified(String path) {
        logger.debug(String.format("reading file attributes %s", path), this);
        try {
            BasicFileAttributes attr = Files.readAttributes(resolve(path), BasicFileAttributes.class);
            return attr.lastModifiedTime().to(TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.exception(e, this);
            throw new RuntimeException("file attrib reading failed");
        }
    }

    public interface Config extends Module {
        Path root();
    }
}
