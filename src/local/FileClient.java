package local;

import model.ApplicationContext;
import model.Logger;
import model.Module;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
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
        try {
            Files.write(path, data);
        } catch (Exception e) {
            logger.error("error writing file: " + e, this);
            throw new RuntimeException("file writing failed");
        }
    }

    public FileWriter open(String path) {
        return open(cwd.resolve(path));
    }

    public FileWriter open(Path path) {
        try {
            return new FileWriter(path.toFile());
        } catch (Exception e) {
            logger.error("error opening file: " + e, this);
            throw new RuntimeException("file opening failed");
        }
    }

    public void close(FileWriter writer) {
        try {
            writer.close();
        } catch (Exception e) {
            logger.error("error closing file: " + e, this);
            throw new RuntimeException(e);
        }
    }

    public List<String> readAllLines(String filename) {
        try {
            return Files.readAllLines(cwd.resolve(filename));
        } catch (Exception e) {
            logger.error("error reading file: " + e, this);
            throw new RuntimeException("file reading failed");
        }
    }

    public long lastModified(String path) {
        try {
            BasicFileAttributes attr = Files.readAttributes(cwd.resolve(path), BasicFileAttributes.class);
            return attr.lastModifiedTime().to(TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.error("error reading file attributes: " + e, this);
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
