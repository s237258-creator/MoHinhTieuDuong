package vn.edu.doan.diabetes.desktop;

import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

final class ApiTask {
    private ApiTask() {
    }

    static void run(
            JButton button,
            Callable<JsonNode> operation,
            Consumer<JsonNode> success,
            Consumer<String> failure) {
        if (button != null) {
            button.setEnabled(false);
        }
        new SwingWorker<JsonNode, Void>() {
            @Override
            protected JsonNode doInBackground() throws Exception {
                return operation.call();
            }

            @Override
            protected void done() {
                if (button != null) {
                    button.setEnabled(true);
                }
                try {
                    success.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    failure.accept("Yêu cầu đã bị gián đoạn.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    String message = cause instanceof IOException && cause.getMessage() != null
                            ? cause.getMessage()
                            : "Không thể hoàn tất yêu cầu: " + cause.getMessage();
                    failure.accept(message);
                }
            }
        }.execute();
    }

    static void showError(java.awt.Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "DiabetesAI", JOptionPane.ERROR_MESSAGE);
    }
}
