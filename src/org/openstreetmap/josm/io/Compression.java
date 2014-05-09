// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipOutputStream;

import org.apache.tools.bzip2.CBZip2OutputStream;
import org.openstreetmap.josm.tools.Utils;

/**
 * An enum representing the compression type of a resource.
 */
public enum Compression {
    /**
     * no compression
     */
    NONE,
    /**
     * bzip2 compression
     */
    BZIP2,
    /**
     * gzip compression
     */
    GZIP,
    /**
     * zip compression
     */
    ZIP;

    /**
     * Determines the compression type depending on the suffix of {@code name}.
     * @param name File name including extension
     * @return the compression type
     */
    public static Compression byExtension(String name) {
        return name != null && name.endsWith(".gz")
                ? GZIP
                : name != null && (name.endsWith(".bz2") || name.endsWith(".bz"))
                ? BZIP2
                : name != null && name.endsWith(".zip")
                ? ZIP
                : NONE;
    }

    /**
     * Returns an un-compressing {@link InputStream} for {@code in}.
     *
     * @throws IOException
     */
    public InputStream getUncompressedInputStream(InputStream in) throws IOException {
        switch (this) {
            case BZIP2:
                return FileImporter.getBZip2InputStream(in);
            case GZIP:
                return FileImporter.getGZipInputStream(in);
            case ZIP:
                return FileImporter.getZipInputStream(in);
            case NONE:
            default:
                return in;
        }
    }

    /**
     * Returns an un-compressing {@link InputStream} for the {@link File} {@code file}.
     *
     * @throws IOException
     */
    @SuppressWarnings("resource")
    public static InputStream getUncompressedFileInputStream(File file) throws IOException {
        return byExtension(file.getName()).getUncompressedInputStream(new FileInputStream(file));
    }

    /**
     * Returns an un-compressing {@link InputStream} for the {@link URL} {@code url}.
     *
     * @throws IOException
     */
    public static InputStream getUncompressedURLInputStream(URL url) throws IOException {
        return Utils.openURLAndDecompress(url, true);
    }

    /**
     * Returns a compressing {@link OutputStream} for {@code out}.
     *
     * @throws IOException
     */
    public OutputStream getCompressedOutputStream(OutputStream out) throws IOException {
        switch (this) {
            case BZIP2:
                out.write('B');
                out.write('Z');
                return new CBZip2OutputStream(out);
            case GZIP:
                return new GZIPOutputStream(out);
            case ZIP:
                return new ZipOutputStream(out, StandardCharsets.UTF_8);
            case NONE:
            default:
                return out;
        }
    }

    /**
     * Returns a compressing {@link OutputStream} for the {@link File} {@code file}.
     *
     * @throws IOException
     */
    @SuppressWarnings("resource")
    public static OutputStream getCompressedFileOutputStream(File file) throws IOException {
        return byExtension(file.getName()).getCompressedOutputStream(new FileOutputStream(file));
    }
}
