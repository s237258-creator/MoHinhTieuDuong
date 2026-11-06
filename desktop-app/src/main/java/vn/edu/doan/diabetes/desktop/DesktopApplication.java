package vn.edu.doan.diabetes.desktop;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import vn.edu.doan.diabetes.DiabetesApiApplication;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;

public final class DesktopApplication {
    private static DesktopWindow desktopWindow;

    private DesktopApplication() {
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        try {
            String configuredApi = System.getenv("DIABETES_API_URL");
            ConfigurableApplicationContext backend = null;
            String apiUrl = configuredApi;

            if (apiUrl == null || apiUrl.isBlank()) {
                backend = new SpringApplicationBuilder(DiabetesApiApplication.class)
                        .headless(false)
                        .run("--server.port=0");
                int port = ((ServletWebServerApplicationContext) backend).getWebServer().getPort();
                apiUrl = "http://localhost:" + port;
            }

            DesktopApiClient apiClient = new DesktopApiClient(apiUrl);
            ConfigurableApplicationContext managedBackend = backend;
            CountDownLatch desktopClosed = new CountDownLatch(1);
            SwingUtilities.invokeLater(() -> {
                try {
                    desktopWindow = new DesktopWindow(apiClient, managedBackend, desktopClosed::countDown);
                    desktopWindow.setVisible(true);
                } catch (RuntimeException exception) {
                    exception.printStackTrace(System.err);
                    if (managedBackend != null) {
                        managedBackend.close();
                    }
                    desktopClosed.countDown();
                    JOptionPane.showMessageDialog(
                            null,
                            "Không thể mở cửa sổ desktop.\n" + exception.getMessage(),
                            "DiabetesAI — lỗi giao diện",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
            desktopClosed.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            exception.printStackTrace(System.err);
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    null,
                    "Không thể khởi động ứng dụng.\n"
                            + "Hãy kiểm tra SQL Server, TCP/IP và cấu hình SQLSERVER_URL, "
                            + "SQLSERVER_USERNAME, SQLSERVER_PASSWORD.\n\n"
                            + exception.getMessage(),
                    "DiabetesAI — lỗi khởi động",
                    JOptionPane.ERROR_MESSAGE));
        }
    }
}
