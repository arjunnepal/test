package com.c3s.ai.admin.shell;

import com.c3s.ai.common.config.Config;
import com.c3s.ai.common.exception.ConfigurationException;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Profile photo storage under upload.dir, outside the webapp so files survive redeploys, with
 * filenames generated here as <uuid>.<ext> and never taken from the client. An upload must decode
 * as the format its extension claims and fit MAX_DIMENSION and MAX_PIXELS, both read from the
 * header first, and only MAX_CONCURRENT_DECODES run at once. No WEBP - the JDK can't read it.
 */
public final class UploadUtil {

    private static final Logger log = LoggerFactory.getLogger(UploadUtil.class);

    private static final Path UPLOAD_DIR = Paths.get(Config.INSTANCE.get(
            "upload.dir", "UPLOAD_DIR", System.getProperty("user.home") + "/c3s-admin-uploads"));
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");
    // extension -> the ImageIO format name its bytes must decode as
    private static final Map<String, String> FORMAT_FOR_EXTENSION = Map.of(
            "jpg", "jpeg", "jpeg", "jpeg", "png", "png", "gif", "gif");
    private static final Pattern SAFE_FILENAME = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|jpeg|png|gif)$");

    static final int MAX_DIMENSION = 4_000;
    static final long MAX_PIXELS = 12_000_000L;
    private static final int MAX_CONCURRENT_DECODES = 2;
    private static final Semaphore DECODES = new Semaphore(MAX_CONCURRENT_DECODES);

    static {
        try {
            Files.createDirectories(UPLOAD_DIR);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to create upload directory " + UPLOAD_DIR, e);
        }
        log.info("Upload directory: {}", UPLOAD_DIR);
    }

    private UploadUtil() {
    }

    /** The bytes aren't an acceptable image; the message is shown to the user. */
    public static final class InvalidImageException extends Exception {
        InvalidImageException(String message) {
            super(message);
        }
    }

    public static Path directory() {
        return UPLOAD_DIR;
    }

    public static boolean isSafeFilename(String filename) {
        return filename != null && SAFE_FILENAME.matcher(filename).matches();
    }

    /** null if the name has no allowed image extension. */
    public static String extractAllowedExtension(String submittedFilename) {
        if (submittedFilename == null) {
            return null;
        }
        int dot = submittedFilename.lastIndexOf('.');
        if (dot < 0 || dot == submittedFilename.length() - 1) {
            return null;
        }
        String ext = submittedFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.contains(ext) ? ext : null;
    }

    /** Validate the part is an acceptable image, write it, return the generated <uuid>.<ext> name. */
    public static String save(Part part, String extension) throws InvalidImageException, IOException {
        byte[] bytes;
        try (InputStream in = part.getInputStream()) {
            bytes = in.readAllBytes();
        }
        return save(bytes, extension);
    }

    static String save(byte[] bytes, String extension) throws InvalidImageException, IOException {
        int[] dims = validateImage(bytes, extension);
        String filename = UUID.randomUUID() + "." + extension;
        Files.write(UPLOAD_DIR.resolve(filename), bytes);
        log.info("Saved profile photo upload {} ({} bytes, {}x{})", filename, bytes.length, dims[0], dims[1]);
        return filename;
    }

    // Reads the format and size from the header before decoding, so a decompression bomb is refused
    // before a raster is allocated. Returns [width, height].
    static int[] validateImage(byte[] bytes, String extension) throws InvalidImageException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (iis == null) {
                throw new InvalidImageException("Photo could not be read.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new InvalidImageException("Photo must be a valid JPG, PNG, or GIF image.");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals(FORMAT_FOR_EXTENSION.get(extension))) {
                    throw new InvalidImageException("Photo's contents don't match its ." + extension + " extension.");
                }
                reader.setInput(iis, true, true);
                int w = reader.getWidth(0);
                int h = reader.getHeight(0);
                if (w > MAX_DIMENSION || h > MAX_DIMENSION) {
                    throw new InvalidImageException(
                            "Photo dimensions must be " + MAX_DIMENSION + "px or less on each side.");
                }
                if ((long) w * h > MAX_PIXELS) {
                    throw new InvalidImageException("Photo is too large - at most "
                            + (MAX_PIXELS / 1_000_000) + " megapixels.");
                }
                decode(reader);
                return new int[]{w, h};
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new InvalidImageException("Photo could not be read as an image.");
        }
    }

    private static void decode(ImageReader reader) throws InvalidImageException, IOException {
        boolean acquired;
        try {
            acquired = DECODES.tryAcquire(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InvalidImageException("Photo upload was interrupted - please try again.");
        }
        if (!acquired) {
            throw new InvalidImageException("The server is busy processing other photos - please try again.");
        }
        try {
            if (reader.read(0) == null) {
                throw new InvalidImageException("Photo could not be decoded as an image.");
            }
        } finally {
            DECODES.release();
        }
    }

    /** No-op if the file's gone or the name isn't one we'd have generated. */
    public static void delete(String filename) {
        if (!isSafeFilename(filename)) {
            return;
        }
        try {
            Files.deleteIfExists(UPLOAD_DIR.resolve(filename));
        } catch (IOException e) {
            log.warn("Failed to delete profile photo upload {}", filename, e);
        }
    }

    public static String contentType(String filename) {
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }
}
