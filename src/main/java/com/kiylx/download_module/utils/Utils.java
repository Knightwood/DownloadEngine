package com.kiylx.download_module.utils;

import io.reactivex.annotations.NonNull;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.kiylx.download_module.ContextKt.getContext;

public class Utils {
    public static final String INFINITY_SYMBOL = "\u221e";
    public static final String HTTP_PREFIX = "http";
    public static final String DEFAULT_DOWNLOAD_FILENAME = "downloadfile";
    public static final String EXTENSION_SEPARATOR = ".";

    /*
     * Format as defined in RFC 2616 and RFC 5987.
     * Both inline and attachment types are supported
     */
    private static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern.compile(
            "(inline|attachment)\\s*;" +
                    "\\s*filename\\s*=\\s*(\"((?:\\\\.|[^\"\\\\])*)\"|[^;]*)\\s*" +
                    "(?:;\\s*filename\\*\\s*=\\s*(utf-8|iso-8859-1|windows-1251)'[^']*'(\\S*))?",
            Pattern.CASE_INSENSITIVE
    );
    private static final String[] supportedFilenameEncodings = new String[]{
            "utf-8",
            "windows-1251"
    };
    /**
     * Keys for the capture groups inside contentDispositionPattern
     */
    private static final int ENCODED_FILE_NAME_GROUP = 5;
    private static final int ENCODING_GROUP = 4;
    private static final int QUOTED_FILE_NAME_GROUP = 3;
    private static final int UNQUOTED_FILE_NAME = 2;

    /**
     * Definition as per RFC 5987, section 3.2.1. (value-chars)
     */
    private static final Pattern ENCODED_SYMBOL_PATTERN = Pattern.compile(
            "%[0-9a-f]{2}|[0-9a-z!#$&+-.^_`|~]",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ACCEPTED_URI_SCHEMA = Pattern.compile(
            "(?i)" +  /* Switch on case insensitive matching */
                    "(" +  /* Begin group for schema */
                    "(?:http|https|file|chrome)://" +
                    "|(?:inline|data|about|javascript):" +
                    ")" +
                    "(.*)"
    );


    /**
     * @param tmpUrl
     * @param contentDisposition
     * @param contentLocation
     * @return 0:filename; 1: mimetype 2:extension
     */
    public static String[] parseMIMEType(String tmpUrl, String contentDisposition, String contentLocation) {
        String fileName = getHttpFileName(tmpUrl, contentDisposition, contentLocation, null);
        String extension = getExtension(fileName);
        String mimeType = null;
        if (!TextUtils.isEmpty(extension))
            mimeType = MimeTypeUtils.guessMimeTypeFromExtension(extension);
        return new String[]{fileName, mimeType, extension};
    }

    public static String getHttpFileName(@NonNull String decodedUrl,
                                         String contentDisposition,
                                         String contentLocation,
                                         String mimeType) {
        String filename = null;
        String extension = null;

        /* First, try to use the content disposition */
        if (filename == null && contentDisposition != null) {
            filename = parseContentDisposition(contentDisposition);
            if (filename != null) {
                int index = filename.lastIndexOf('/') + 1;
                if (index > 0)
                    filename = filename.substring(index);
            }
        }

        /* If we still have nothing at this point, try the content location */
        if (filename == null && contentLocation != null) {
            String decodedContentLocation = URLDecoder.decode(contentLocation,Charset.defaultCharset());
            if (decodedContentLocation != null) {
                int queryIndex = decodedContentLocation.indexOf('?');
                /* If there is a query string strip it, same as desktop browsers */
                if (queryIndex > 0)
                    decodedUrl = decodedContentLocation.substring(0, queryIndex);

                if (!decodedContentLocation.endsWith("/")) {
                    int index = decodedContentLocation.lastIndexOf('/') + 1;
                    if (index > 0)
                        filename = decodedContentLocation.substring(index);
                    else
                        filename = decodedContentLocation;
                }
            }
        }

        /* If all the other http-related approaches failed, use the plain uri */
        if (filename == null) {
            int queryIndex = decodedUrl.indexOf('?');
            /* If there is a query string strip it, same as desktop browsers */
            if (queryIndex > 0)
                decodedUrl = decodedUrl.substring(0, queryIndex);

            if (!decodedUrl.endsWith("/")) {
                int index = decodedUrl.lastIndexOf('/') + 1;
                if (index > 0) {
                    String rawFilename = decodedUrl.substring(index);
                    filename = autoDecodePercentEncoding(rawFilename);
                    if (filename == null) {
                        filename = rawFilename;
                    }
                }
            }
        }

        /* Finally, if couldn't get filename from URI, get a generic filename */
        if (filename == null)
            filename = DEFAULT_DOWNLOAD_FILENAME;

        /*
         * Split filename between base and extension.
         * Add an extension if filename does not have one
         */
        int dotIndex = filename.indexOf('.');
        if (dotIndex < 0) {
            if (mimeType != null) {
                extension = MimeTypeUtils.guessExtensionFromMimeType(mimeType);
                if (extension != null)
                    extension = "." + extension;
            }
            if (extension == null) {
                if (mimeType != null && mimeType.toLowerCase(Locale.ROOT).startsWith("text/")) {
                    if (mimeType.equalsIgnoreCase("text/html"))
                        extension = ".html";
                    else
                        extension = ".txt";
                } else {
                    extension = ".bin";
                }
            }
        } else {
            if (mimeType != null) {
                /*
                 * Compare the last segment of the extension against the mime type.
                 * If there's a mismatch, discard the entire extension.
                 */
                int lastDotIndex = filename.lastIndexOf('.');
                String typeFromExt = MimeTypeUtils.guessMimeTypeFromExtension(filename.substring(lastDotIndex + 1));
                if (typeFromExt != null && !typeFromExt.equalsIgnoreCase(mimeType)) {
                    extension = MimeTypeUtils.guessExtensionFromMimeType(mimeType);
                    if (extension != null)
                        extension = "." + extension;
                }
            }
            if (extension == null)
                extension = filename.substring(dotIndex);

            filename = filename.substring(0, dotIndex);
        }

        /*
         * The VFAT file system is assumed as target for downloads.
         * Replace invalid characters according to the specifications of VFAT
         */
        //filename = fs.buildValidFatFilename(filename + extension);
        filename = filename + extension;
        return filename;
    }

    /*
     * Parse the Content-Disposition HTTP Header. The format of the header
     * is defined here: http://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html
     * This header provides a filename for content that is going to be
     * downloaded to the file system. We only support the attachment type
     */

    private static String parseContentDisposition(@NonNull String contentDisposition) {
        try {
            Matcher m = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                // If escaped string is found, decode it using the given encoding
                String encodedFileName = m.group(ENCODED_FILE_NAME_GROUP);
                String encoding = m.group(ENCODING_GROUP);

                if (encodedFileName != null && encoding != null) {
                    return decodePercentEncoding(encodedFileName, encoding);
                }

                // Return quoted string if available and replace escaped characters.
                String quotedFileName = m.group(QUOTED_FILE_NAME_GROUP);

                return quotedFileName == null ?
                        m.group(UNQUOTED_FILE_NAME) :
                        quotedFileName.replace("\\\\(.)", "$1");
            }
        } catch (IllegalStateException | NumberFormatException e) {
            // This function is defined as returning null when it can't parse the header
        } catch (UnsupportedEncodingException e) {
            // Nothing
        }

        return null;
    }

    private static String decodePercentEncoding(String field, String encoding)
            throws UnsupportedEncodingException, NumberFormatException {
        byte[] bytes = percentEncodingBytes(field);
        return new String(bytes, 0, bytes.length, encoding);
    }

    private static byte[] percentEncodingBytes(String field)
            throws NumberFormatException {
        Matcher m = ENCODED_SYMBOL_PATTERN.matcher(field);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        while (m.find()) {
            String symbol = m.group();
            if (symbol.startsWith("%")) {
                stream.write(Integer.parseInt(symbol.substring(1), 16));
            } else {
                stream.write(symbol.charAt(0));
            }
        }

        return stream.toByteArray();
    }

    private static String autoDecodePercentEncoding(String field) throws NumberFormatException {
        String encoding;
        UniversalDetector detector = new UniversalDetector();
        byte[] bytes = percentEncodingBytes(field);

        detector.handleData(bytes);
        detector.dataEnd();
        encoding = detector.getDetectedCharset();
        detector.reset();

        try {
            return encoding == null ?
                    null :
                    new String(bytes, 0, bytes.length, encoding);
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static boolean checkConnectivity() {
        return getContext().getSysCallKit().checkConnectivity();
    }

    public static String getExtension(String fileName) {
        if (fileName == null)
            return null;

        int extensionPos = fileName.lastIndexOf(EXTENSION_SEPARATOR);
        int lastSeparator = fileName.lastIndexOf(File.separator);
        int index = (lastSeparator > extensionPos ? -1 : extensionPos);

        if (index == -1)
            return "";
        else
            return fileName.substring(index + 1);
    }
}
