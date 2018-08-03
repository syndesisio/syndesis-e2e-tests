package io.syndesis.qe.utils;

import static org.assertj.core.api.Assertions.fail;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FtpUtils {
    private FTPClient ftpClient = FtpClientManager.getClient();

    public void delete(String path) {
        checkConnection();
        log.info("Deleting " + path + " from FTP server");
        try {
            ftpClient.deleteFile(path);
        } catch (IOException e) {
            fail("Unable to delete file", path);
        }
    }

    public boolean isFileThere(String directory, String fileName) {
        checkConnection();
        try {
            return Arrays.stream(ftpClient.listFiles(directory)).filter(file -> file.getName().equals(fileName)).count() == 1;
        } catch (IOException ex) {
            fail("Unable to list files in FTP", ex);
        }
        return false;
    }

    public void uploadTestFile(String testFileName, String text, String remoteDirectory) {
        checkConnection();
        log.info("Uploading file " + testFileName + " with content " + text + " to directory " + remoteDirectory + ". This may take some time");
        try (InputStream is = IOUtils.toInputStream(text, "UTF-8")) {
            ftpClient.storeFile("/" + remoteDirectory + "/" + testFileName, is);
        } catch (IOException ex) {
            fail("Unable to upload test file: ", ex);
        }
    }

    /**
     * Sometimes the Connections ends with "FTPConnectionClosedException: Connection closed without indication".
     */
    private void checkConnection() {
        try {
            ftpClient.listFiles();
        } catch (IOException ex) {
            ftpClient = FtpClientManager.getClient();
        }
    }
}
