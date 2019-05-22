package org.parabot.environment.handlers.exceptions;

import org.parabot.core.Directories;
import org.parabot.core.ui.utils.UILog;
import org.parabot.environment.api.utils.FileUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Writes exceptions to a file and reports the file location back to the user
 */
public class FileExceptionHandler extends ExceptionHandler {
    /**
     * Directory where the reports get written to
     */
    private final File reportsDirectory;

    /**
     * Defines if the alert should popup during this client instance again
     */
    private boolean ignored = false;

    /**
     * Initializes the exception handler and ensures the reports directory is created and writable
     */
    public FileExceptionHandler(ExceptionType exceptionType) {
        super("File exception handler", exceptionType);

        this.reportsDirectory = new File(Directories.getWorkspace(), "reports");
        if (!this.reportsDirectory.exists() || !this.reportsDirectory.isDirectory()) {
            this.reportsDirectory.mkdir();
        }

        this.cleanOldErrors();
    }

    @Override
    public void handle(Throwable e) {
        File report = new File(this.reportsDirectory, "report-" + (System.currentTimeMillis() / 1000) + ".txt");
        try {
            report.createNewFile();

            StringBuilder reportContent = new StringBuilder();
            reportContent.append(e.getMessage() + "\n\n");

            for (int i = 0; i < e.getStackTrace().length; i++) {
                reportContent.append((i > 0 ? "  " : "") + e.getStackTrace()[i] + "\n");
            }

            FileUtil.writeFileContents(report, reportContent.toString());

            if (!ignored) {
                displayAlert(report);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public boolean isIgnored() {
        return ignored;
    }

    public FileExceptionHandler setIgnored(boolean ignored) {
        this.ignored = ignored;
        return this;
    }

    public File getReportsDirectory() {
        return reportsDirectory;
    }

    /**
     * Displays the dialog of the alert
     *
     * @param report
     */
    private void displayAlert(File report) {
        int response = UILog.alert(
                "Error occurred",
                "We are sorry to inform you that an error occurred within Parabot.\n\n" +
                        "The error has been written to a report file.\n" +
                        "Please report the error to the Parabot staff with as much information as possible.",
                new Object[]{
                        "Close",
                        "Open report",
                        "Ignore " + this.getExceptionType().getName().toLowerCase() + " errors" },
                1,
                JOptionPane.WARNING_MESSAGE
        );

        switch (response) {
            case 1:
                try {
                    Desktop.getDesktop().open(report);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
            case 2:
                ignored = true;
                break;

        }
    }

    /**
     * Remove errors older than 24 hours
     */
    private void cleanOldErrors() {
        File[] reports = this.reportsDirectory.listFiles();
        if (reports != null) {
            for (File report : reports) {
                if (report.isFile()) {
                    if ((System.currentTimeMillis() - report.lastModified()) / 1000 > 60 * 60 * 24) {
                        report.delete();
                    }
                }
            }
        }
    }
}
