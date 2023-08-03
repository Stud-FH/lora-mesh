package local;

import model.ApplicationContext;
import model.Logger;
import model.Module;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class FileClient implements Module {

    public final Path cwd;
    private Logger logger;

    public FileClient(Path cwd) {
        this.cwd = cwd;
    }

    @Override
    public String info() {
        return String.format("File Client @ %s", cwd);
    }

    @Override
    public void useContext(ApplicationContext ctx) {
        this.logger = ctx.resolve(Logger.class);
    }

    public void write(Path path, String text) {
        write(path, text.getBytes());
    }

    public void write(String path, String text) {
        write(path, text.getBytes());
    }

    public void write(String path, byte[] data) {
        write(cwd.resolve(path), data);
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
        return open(cwd.resolve(path));
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
            return Files.readAllLines(cwd.resolve(filename));
        } catch (Exception e) {
            logger.exception(e, this);
            throw new RuntimeException("file reading failed");
        }
    }

    public long lastModified(String path) {
        logger.debug(String.format("reading file attributes %s", path), this);
        try {
            BasicFileAttributes attr = Files.readAttributes(cwd.resolve(path), BasicFileAttributes.class);
            return attr.lastModifiedTime().to(TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.exception(e, this);
            throw new RuntimeException("file attrib reading failed");
        }
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(Logger.class);
    }

    @Override
    public Collection<Class<? extends Module>> providers() {
        return Set.of(FileClient.class);
    }
}
