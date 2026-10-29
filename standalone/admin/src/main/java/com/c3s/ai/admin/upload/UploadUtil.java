package com.c3s.ai.admin.upload;

import com.c3s.ai.admin.exception.ConfigurationException;
import com.c3s.ai.admin.util.Config;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Profile photo storage under upload.dir, outside the webapp so files survive redeploys.
 * Filenames are always generated here (<uuid>.<ext>), never taken from the client.
 * Uploads must decode as a real image via ImageIO (no WEBP - the JDK can't read it).
 */
public final class UploadUtil {

    private static final Logger log = LoggerFactory.getLogger(UploadUtil.class);

    private static final Path UPLOAD_DIR = Paths.get(Config.INSTANCE.get(
            "upload.dir", "UPLOAD_DIR", System.getProperty("user.home") + "/c3s-admin-uploads"));
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");
    private static final Pattern SAFE_FILENAME = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|jpeg|png|gif)$");
    private static final int MAX_DIMENSION = 4_000;

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

    /** Bytes didn't decode as an image, or were bigger than MAX_DIMENSION. */
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

    /** Validate the part is an image in bounds, write it, return the generated <uuid>.<ext> name. */
    public static String save(Part part, String extension) throws InvalidImageException, IOException {
        byte[] bytes;
        try (InputStream in = part.getInputStream()) {
            bytes = in.readAllBytes();
        }

        int[] dims = validateImage(bytes);

        String filename = UUID.randomUUID() + "." + extension;
        Files.write(UPLOAD_DIR.resolve(filename), bytes);
        log.info("Saved profile photo upload {} ({} bytes, {}x{})", filename, bytes.length, dims[0], dims[1]);
        return filename;
    }

    // reads w/h from the header before decoding, so a decompression bomb is rejected
    // before we allocate the raster. returns [width, height].
    private static int[] validateImage(byte[] bytes) throws InvalidImageException {
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
                reader.setInput(iis, true, true);
                int w = reader.getWidth(0);
                int h = reader.getHeight(0);
                if (w > MAX_DIMENSION || h > MAX_DIMENSION) {
                    throw new InvalidImageException(
                            "Photo dimensions must be " + MAX_DIMENSION + "px or less on each side.");
                }
                if (reader.read(0) == null) {   // safe to decode now that dimensions are checked
                    throw new InvalidImageException("Photo could not be decoded as an image.");
                }
                return new int[] {w, h};
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new InvalidImageException("Photo could not be read as an image.");
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
