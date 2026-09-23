package com.c3s.ai.admin.shell;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadUtilTest {

    @BeforeAll
    static void uploadDirectory() throws IOException {
        // UploadUtil reads upload.dir when the class loads
        System.setProperty("upload.dir", Files.createTempDirectory("upload-util-test").toString());
    }

    private static byte[] image(int width, int height, String format) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        return out.toByteArray();
    }

    @Test
    void acceptsARealImageOfItsClaimedFormat() throws Exception {
        assertArrayEquals(new int[]{40, 30}, UploadUtil.validateImage(image(40, 30, "png"), "png"));
        assertArrayEquals(new int[]{40, 30}, UploadUtil.validateImage(image(40, 30, "gif"), "gif"));
    }

    @Test
    void refusesContentThatDoesNotMatchTheExtension() throws Exception {
        byte[] gif = image(40, 30, "gif");
        UploadUtil.InvalidImageException e = assertThrows(UploadUtil.InvalidImageException.class,
                () -> UploadUtil.validateImage(gif, "png"));
        assertTrue(e.getMessage().contains(".png"));
    }

    @Test
    void refusesTooManyPixelsEvenWithinTheSideLimit() throws Exception {
        // 4000 x 3001 is inside 4000px per side but over 12 megapixels
        byte[] big = image(UploadUtil.MAX_DIMENSION, 3001, "png");
        assertThrows(UploadUtil.InvalidImageException.class, () -> UploadUtil.validateImage(big, "png"));
    }

    @Test
    void refusesAnythingThatIsNotAnImage() {
        assertThrows(UploadUtil.InvalidImageException.class,
                () -> UploadUtil.validateImage("<svg onload=alert(1)>".getBytes(), "png"));
    }

    @Test
    void onlyGeneratedNamesAreSafe() {
        assertTrue(UploadUtil.isSafeFilename("0b7c2f3e-1d2a-4c5b-8e9f-0a1b2c3d4e5f.png"));
        assertFalse(UploadUtil.isSafeFilename("../../etc/passwd"));
        assertFalse(UploadUtil.isSafeFilename("0b7c2f3e-1d2a-4c5b-8e9f-0a1b2c3d4e5f.svg"));
        assertFalse(UploadUtil.isSafeFilename(null));
    }
}
