package vn.edu.doan.diabetes.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

final class DesktopWindow extends JFrame {
    private static final Color PURPLE = new Color(105, 75, 235);
    private static final Color INK = new Color(36, 39, 54);
    private static final Color MUTED = new Color(103, 107, 124);
    private static final String[] FIELD_KEYS = {
            "pregnancies", "glucose", "blood_pressure", "skin_thickness",
            "insulin", "bmi", "diabetes_pedigree", "age"
    };
    private static final String[] FIELD_LABELS = {
            "Số lần mang thai", "Glucose huyết tương (mg/dL)", "Huyết áp tâm trương (mmHg)",
            "Độ dày nếp da (mm)", "Insulin 2 giờ (mU/L)", "BMI (kg/m²)",
            "Hệ số di truyền", "Tuổi"
    };
    private static final double[] FIELD_MIN = {0, 0, 0, 0, 0, 0, 0, 1};
    private static final double[] FIELD_MAX = {20, 300, 200, 100, 900, 70, 3, 120};

    private final DesktopApiClient api;
    private final ConfigurableApplicationContext backend;
    private final JLabel sessionLabel = new JLabel("Chưa đăng nhập");
    private final JLabel statusLabel = new JLabel(" ");
    private final JTextField[] healthFields = new JTextField[FIELD_KEYS.length];
    private final JTextField patientName = new JTextField();
    private final JCheckBox[] comorbidityFields = checkboxes("Tăng huyết áp", "Tim mạch", "Béo phì");
    private final JCheckBox[] lifestyleFields = checkboxes("Ít vận động", "Hút thuốc", "Ăn nhiều đồ ngọt");
    private final JTextArea recommendation = new JTextArea(8, 48);
    private final JButton savePredictionButton = button("Lưu vào lịch sử", true);
    private final JComboBox<String> historyFilter = new JComboBox<>(
            new String[]{"Tất cả", "Thấp", "Trung bình", "Cao"});
    private final DefaultTableModel historyModel = new DefaultTableModel(
            new String[]{"ID", "Người được đánh giá", "Mức nguy cơ", "Tỷ lệ", "Ngày tạo"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable historyTable = new JTable(historyModel);
    private final DefaultTableModel userModel = new DefaultTableModel(
            new String[]{"ID", "Họ tên", "Số điện thoại", "Vai trò"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable userTable = new JTable(userModel);
    private final JLabel statsLabel = new JLabel("Nhấn “Làm mới” để xem thống kê.");
    private final JPanel adminPanel = new JPanel(new BorderLayout(8, 8));
    private JsonNode currentUser;
    private JsonNode lastInput;
    private JsonNode lastContext;
    private JsonNode lastPrediction;
    private List<JsonNode> historyRecords = List.of();
    private List<JsonNode> users = List.of();

    DesktopWindow(DesktopApiClient api, ConfigurableApplicationContext backend, Runnable onClose) {
        super("DiabetesAI — ứng dụng desktop");
        this.api = api;
        this.backend = backend;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(980, 720));
        setSize(1100, 790);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.WHITE);
        setLayout(new BorderLayout(0, 0));
        add(createHeader(), BorderLayout.NORTH);
        add(createTabs(), BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                if (DesktopWindow.this.backend != null) {
                    DesktopWindow.this.backend.close();
                }
                dispose();
                onClose.run();
            }
        });
        updateSession();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(232, 232, 240)),
                BorderFactory.createEmptyBorder(15, 24, 15, 24)));
        JLabel brand = new JLabel("DiabetesAI");
        brand.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        brand.setForeground(PURPLE);
        JLabel subtitle = new JLabel("  Đánh giá nguy cơ tiểu đường");
        subtitle.setForeground(MUTED);
        JPanel title = new JPanel(new BorderLayout());
        title.setBackground(Color.WHITE);
        title.add(brand, BorderLayout.WEST);
        title.add(subtitle, BorderLayout.CENTER);
        sessionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        sessionLabel.setForeground(INK);
        header.add(title, BorderLayout.WEST);
        header.add(sessionLabel, BorderLayout.EAST);
        return header;
    }

    private JTabbedPane createTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        tabs.addTab("Dự đoán", createPredictionTab());
        tabs.addTab("Lịch sử", createHistoryTab());
        tabs.addTab("Thống kê", createStatsTab());
        tabs.addTab("Tài khoản", createAccountTab());
        return tabs;
    }

    private JPanel createPredictionTab() {
        JPanel content = page();
        content.add(heading("Đánh giá nguy cơ"));
        content.add(label("Nhập các chỉ số sức khỏe để nhận kết quả tham khảo."));
        JPanel grid = new JPanel(new GridLayout(0, 2, 14, 8));
        grid.setBackground(Color.WHITE);
        for (int i = 0; i < FIELD_KEYS.length; i++) {
            JPanel field = new JPanel(new BorderLayout(4, 3));
            field.setBackground(Color.WHITE);
            field.add(label(FIELD_LABELS[i]), BorderLayout.NORTH);
            JTextField input = new JTextField(defaultValue(i), 16);
            input.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
            healthFields[i] = input;
            field.add(input, BorderLayout.CENTER);
            grid.add(field);
        }
        content.add(grid);

        JPanel context = new JPanel(new GridLayout(1, 2, 16, 0));
        context.setBackground(Color.WHITE);
        context.add(checkGroup("Bệnh nền", comorbidityFields));
        context.add(checkGroup("Thói quen sinh hoạt", lifestyleFields));
        content.add(context);

        JPanel patientPanel = new JPanel(new BorderLayout(8, 0));
        patientPanel.setBackground(Color.WHITE);
        patientPanel.add(label("Tên người được đánh giá (không bắt buộc)"), BorderLayout.WEST);
        patientPanel.add(patientName, BorderLayout.CENTER);
        content.add(patientPanel);

        JPanel actions = new JPanel(new BorderLayout(10, 0));
        actions.setBackground(Color.WHITE);
        JButton analyze = button("Phân tích nguy cơ", true);
        savePredictionButton.setEnabled(false);
        actions.add(analyze, BorderLayout.WEST);
        actions.add(savePredictionButton, BorderLayout.EAST);
        content.add(actions);

        recommendation.setEditable(false);
        recommendation.setLineWrap(true);
        recommendation.setWrapStyleWord(true);
        recommendation.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        recommendation.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        recommendation.setText("Kết quả và hướng dẫn sẽ hiển thị tại đây.");
        content.add(new JScrollPane(recommendation));

        analyze.addActionListener(event -> predict(analyze));
        savePredictionButton.addActionListener(event -> savePrediction());
        return wrap(content);
    }

    private JPanel createHistoryTab() {
        JPanel content = page();
        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setBackground(Color.WHITE);
        toolbar.add(heading("Lịch sử đánh giá"), BorderLayout.WEST);
        JPanel controls = new JPanel();
        controls.setBackground(Color.WHITE);
        controls.add(historyFilter);
        JButton refresh = button("Làm mới", false);
        controls.add(refresh);
        toolbar.add(controls, BorderLayout.EAST);
        content.add(toolbar);

        historyTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        historyTable.setRowHeight(28);
        historyTable.getColumnModel().getColumn(0).setMinWidth(0);
        historyTable.getColumnModel().getColumn(0).setMaxWidth(0);
        historyTable.getColumnModel().getColumn(0).setWidth(0);
        content.add(new JScrollPane(historyTable));

        JPanel actions = new JPanel();
        actions.setBackground(Color.WHITE);
        JButton detail = button("Xem chi tiết", false);
        JButton delete = button("Xóa bản ghi", false);
        actions.add(detail);
        actions.add(delete);
        content.add(actions);
        refresh.addActionListener(event -> loadHistory());
        historyFilter.addActionListener(event -> refreshHistoryRows());
        detail.addActionListener(event -> showSelectedRecord());
        delete.addActionListener(event -> deleteSelectedRecord());
        return wrap(content);
    }

    private JPanel createStatsTab() {
        JPanel content = page();
        content.add(heading("Thống kê"));
        statsLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        statsLabel.setForeground(INK);
        content.add(statsLabel);
        JButton refresh = button("Làm mới thống kê", true);
        refresh.addActionListener(event -> loadStats(refresh));
        content.add(refresh);
        return wrap(content);
    }

    private JPanel createAccountTab() {
        JPanel content = page();
        content.add(heading("Tài khoản"));
        JPanel form = new JPanel(new GridLayout(0, 2, 12, 8));
        form.setBackground(Color.WHITE);
        JTextField phone = addFormField(form, "Số điện thoại");
        JPasswordField password = addPasswordField(form, "Mật khẩu");
        JTextField fullName = addFormField(form, "Họ và tên (khi đăng ký)");
        JTextField age = addFormField(form, "Tuổi (khi đăng ký)");
        JTextField gender = addFormField(form, "Giới tính (khi đăng ký)");
        content.add(form);

        JPanel buttons = new JPanel();
        buttons.setBackground(Color.WHITE);
        JButton login = button("Đăng nhập", true);
        JButton register = button("Đăng ký", false);
        JButton logout = button("Đăng xuất", false);
        buttons.add(login);
        buttons.add(register);
        buttons.add(logout);
        content.add(buttons);
        JLabel connection = label("Backend: " + api.baseUrl());
        content.add(connection);

        adminPanel.setBackground(Color.WHITE);
        JButton loadUsers = button("Tải danh sách người dùng", false);
        JButton changeRole = button("Đổi vai trò người dùng đã chọn", false);
        adminPanel.add(loadUsers, BorderLayout.NORTH);
        adminPanel.add(new JScrollPane(userTable), BorderLayout.CENTER);
        adminPanel.add(changeRole, BorderLayout.SOUTH);
        content.add(adminPanel);

        login.addActionListener(event -> {
            ObjectNode body = api.object();
            body.put("phone_number", phone.getText().trim());
            body.put("password", new String(password.getPassword()));
            ApiTask.run(login, () -> api.post("/auth/login", body), response -> {
                currentUser = response.path("user");
                api.setAccessToken(response.path("access_token").asText());
                updateSession();
                setStatus("Đăng nhập thành công.");
                loadHistory();
            }, message -> ApiTask.showError(this, message));
        });

        register.addActionListener(event -> {
            int years;
            try {
                years = Integer.parseInt(age.getText().trim());
            } catch (NumberFormatException exception) {
                ApiTask.showError(this, "Nhập tuổi hợp lệ để đăng ký.");
                return;
            }
            ObjectNode body = api.object();
            body.put("phone_number", phone.getText().trim());
            body.put("password", new String(password.getPassword()));
            body.put("full_name", fullName.getText().trim());
            body.put("age", years);
            body.put("gender", gender.getText().trim());
            ApiTask.run(register, () -> api.post("/auth/register", body), response -> {
                JOptionPane.showMessageDialog(this, "Đăng ký thành công. Hãy đăng nhập.");
                phone.setText(response.path("phone_number").asText());
            }, message -> ApiTask.showError(this, message));
        });

        logout.addActionListener(event -> {
            api.setAccessToken(null);
            currentUser = null;
            lastInput = null;
            lastContext = null;
            lastPrediction = null;
            historyRecords = List.of();
            users = List.of();
            historyModel.setRowCount(0);
            userModel.setRowCount(0);
            recommendation.setText("Kết quả và hướng dẫn sẽ hiển thị tại đây.");
            statsLabel.setText("Đăng nhập để xem thống kê lịch sử.");
            updateSession();
            setStatus("Đã đăng xuất.");
        });
        loadUsers.addActionListener(event -> loadUsers(loadUsers));
        changeRole.addActionListener(event -> changeSelectedRole(changeRole));
        return wrap(content);
    }

    private void predict(JButton button) {
        ObjectNode input = api.object();
        try {
            for (int i = 0; i < FIELD_KEYS.length; i++) {
                double value = Double.parseDouble(healthFields[i].getText().trim());
                if (!Double.isFinite(value) || value < FIELD_MIN[i] || value > FIELD_MAX[i]) {
                    throw new IllegalArgumentException(FIELD_LABELS[i] + " nằm ngoài phạm vi cho phép.");
                }
                input.put(FIELD_KEYS[i], value);
            }
        } catch (NumberFormatException exception) {
            ApiTask.showError(this, "Vui lòng nhập đủ các chỉ số dưới dạng số.");
            return;
        } catch (IllegalArgumentException exception) {
            ApiTask.showError(this, exception.getMessage());
            return;
        }

        ObjectNode context = createContext();
        setStatus("Đang phân tích dữ liệu...");
        ApiTask.run(button, () -> api.post("/predict", input), result -> {
            lastInput = input;
            lastPrediction = result;
            lastContext = context;
            recommendation.setText("Mức nguy cơ: " + result.path("risk_level").asText()
                    + " (" + result.path("risk_percentage").asDouble() + "%)\n\n"
                    + result.path("recommendation").asText());
            recommendation.setCaretPosition(0);
            savePredictionButton.setEnabled(currentUser != null);
            setStatus(currentUser == null
                    ? "Đã có kết quả. Đăng nhập để lưu kết quả vào lịch sử."
                    : "Phân tích hoàn tất.");
        }, message -> {
            setStatus("Không thể dự đoán.");
            ApiTask.showError(this, message);
        });
    }

    private ObjectNode createContext() {
        ObjectNode context = api.object();
        ArrayNode conditions = context.putArray("comorbidities");
        for (JCheckBox box : comorbidityFields) {
            if (box.isSelected()) conditions.add(box.getText());
        }
        ArrayNode habits = context.putArray("lifestyle_habits");
        for (JCheckBox box : lifestyleFields) {
            if (box.isSelected()) habits.add(box.getText());
        }
        return context;
    }

    private void savePrediction() {
        if (currentUser == null || lastInput == null || lastPrediction == null) {
            ApiTask.showError(this, "Hãy đăng nhập và chạy dự đoán trước khi lưu.");
            return;
        }
        ObjectNode body = api.object();
        String name = patientName.getText().trim();
        body.put("patient_name", name.isEmpty() ? "Ẩn danh" : name);
        body.set("input_data", lastInput);
        body.set("patient_context", lastContext);
        body.set("prediction", lastPrediction);
        ApiTask.run(savePredictionButton, () -> api.post("/records", body), response -> {
            setStatus("Đã lưu kết quả vào lịch sử.");
            loadHistory();
        }, message -> ApiTask.showError(this, message));
    }

    private void loadHistory() {
        if (currentUser == null) {
            historyRecords = List.of();
            refreshHistoryRows();
            return;
        }
        ApiTask.run(null, () -> api.get("/records?page=1&limit=100"), response -> {
            List<JsonNode> records = new ArrayList<>();
            JsonNode values = response.path("records");
            if (values.isArray()) values.forEach(records::add);
            historyRecords = List.copyOf(records);
            refreshHistoryRows();
            setStatus("Đã tải " + historyRecords.size() + " bản ghi.");
        }, message -> ApiTask.showError(this, message));
    }

    private void refreshHistoryRows() {
        historyModel.setRowCount(0);
        String filter = (String) historyFilter.getSelectedItem();
        for (JsonNode record : historyRecords) {
            JsonNode prediction = record.path("prediction");
            String level = prediction.path("risk_level").asText();
            if (filter != null && !filter.equals("Tất cả") && !filter.equals(level)) continue;
            historyModel.addRow(new Object[]{
                    record.path("id").asText(),
                    record.path("patient_name").asText("Ẩn danh"),
                    level,
                    prediction.path("risk_percentage").asDouble() + "%",
                    record.path("created_at").asText()
            });
        }
    }

    private void showSelectedRecord() {
        JsonNode record = selectedHistoryRecord();
        if (record == null) {
            ApiTask.showError(this, "Chọn một bản ghi trong danh sách trước.");
            return;
        }
        JsonNode input = record.path("input_data");
        JsonNode result = record.path("prediction");
        StringBuilder details = new StringBuilder()
                .append("Người được đánh giá: ").append(record.path("patient_name").asText()).append('\n')
                .append("Mức nguy cơ: ").append(result.path("risk_level").asText())
                .append(" (").append(result.path("risk_percentage").asDouble()).append("%)\n\n");
        for (int i = 0; i < FIELD_KEYS.length; i++) {
            details.append(FIELD_LABELS[i]).append(": ")
                    .append(input.path(FIELD_KEYS[i]).asText()).append('\n');
        }
        details.append("\n").append(result.path("recommendation").asText());
        JTextArea text = new JTextArea(details.toString(), 20, 55);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(this, new JScrollPane(text),
                "Chi tiết đánh giá", JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteSelectedRecord() {
        JsonNode record = selectedHistoryRecord();
        if (record == null) {
            ApiTask.showError(this, "Chọn một bản ghi trong danh sách trước.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Xóa bản ghi đã chọn?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) return;
        String id = record.path("id").asText();
        ApiTask.run(null, () -> api.delete("/records/" + id), response -> {
            setStatus("Đã xóa bản ghi.");
            loadHistory();
        }, message -> ApiTask.showError(this, message));
    }

    private JsonNode selectedHistoryRecord() {
        int row = historyTable.getSelectedRow();
        if (row < 0) return null;
        String id = String.valueOf(historyModel.getValueAt(row, 0));
        return historyRecords.stream()
                .filter(record -> record.path("id").asText().equals(id))
                .findFirst().orElse(null);
    }

    private void loadStats(JButton button) {
        if (currentUser == null) {
            statsLabel.setText("Đăng nhập để xem thống kê lịch sử.");
            return;
        }
        ApiTask.run(button, () -> api.get("/stats"), response -> {
            JsonNode distribution = response.path("risk_distribution");
            statsLabel.setText("<html><div style='line-height:1.8'>"
                    + "<b>Tổng số bản ghi:</b> " + response.path("total_records").asInt() + "<br>"
                    + "<b>Nguy cơ thấp:</b> " + distribution.path("Thấp").asInt() + "<br>"
                    + "<b>Nguy cơ trung bình:</b> " + distribution.path("Trung bình").asInt() + "<br>"
                    + "<b>Nguy cơ cao:</b> " + distribution.path("Cao").asInt() + "<br>"
                    + "<b>Tuổi trung bình:</b> " + response.path("avg_age").asDouble() + "<br>"
                    + "<b>BMI trung bình:</b> " + response.path("avg_bmi").asDouble() + "<br>"
                    + "<b>Glucose trung bình:</b> " + response.path("avg_glucose").asDouble() + " mg/dL<br>"
                    + "<b>Tỷ lệ nguy cơ cao:</b> "
                    + response.path("high_risk_percentage").asDouble() + "%</div></html>");
            setStatus("Đã cập nhật thống kê.");
        }, message -> ApiTask.showError(this, message));
    }

    private void loadUsers(JButton button) {
        if (currentUser == null || !currentUser.path("role").asText().equals("admin")) {
            ApiTask.showError(this, "Chức năng này chỉ dành cho quản trị viên.");
            return;
        }
        ApiTask.run(button, () -> api.get("/users/"), response -> {
            List<JsonNode> loaded = new ArrayList<>();
            if (response.isArray()) response.forEach(loaded::add);
            users = List.copyOf(loaded);
            userModel.setRowCount(0);
            for (JsonNode user : users) {
                userModel.addRow(new Object[]{
                        user.path("id").asText(),
                        user.path("full_name").asText(),
                        user.path("phone_number").asText(),
                        user.path("role").asText()
                });
            }
        }, message -> ApiTask.showError(this, message));
    }

    private void changeSelectedRole(JButton button) {
        if (userTable.getSelectedRow() < 0) {
            ApiTask.showError(this, "Chọn một tài khoản trong danh sách trước.");
            return;
        }
        int row = userTable.getSelectedRow();
        String id = String.valueOf(userModel.getValueAt(row, 0));
        String oldRole = String.valueOf(userModel.getValueAt(row, 3));
        ObjectNode body = api.object();
        body.put("role", oldRole.equals("admin") ? "user" : "admin");
        ApiTask.run(button, () -> api.patch("/users/" + id + "/role", body), response -> {
            setStatus(response.path("message").asText("Đã cập nhật vai trò."));
            loadUsers(null);
        }, message -> ApiTask.showError(this, message));
    }

    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(232, 232, 240)),
                BorderFactory.createEmptyBorder(7, 18, 7, 18)));
        bar.setBackground(Color.WHITE);
        statusLabel.setForeground(MUTED);
        bar.add(statusLabel, BorderLayout.CENTER);
        return bar;
    }

    private JPanel checkGroup(String title, JCheckBox[] fields) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setBackground(Color.WHITE);
        group.setBorder(BorderFactory.createTitledBorder(title));
        for (JCheckBox field : fields) {
            group.add(field);
        }
        return group;
    }

    private static JTextField addFormField(JPanel form, String label) {
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setForeground(MUTED);
        JTextField field = new JTextField();
        form.add(fieldLabel);
        form.add(field);
        return field;
    }

    private static JPasswordField addPasswordField(JPanel form, String label) {
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setForeground(MUTED);
        JPasswordField field = new JPasswordField();
        form.add(fieldLabel);
        form.add(field);
        return field;
    }

    private static JPanel page() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(Box.createVerticalStrut(4));
        return panel;
    }

    private static JPanel wrap(JPanel content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 23));
        label.setForeground(INK);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        return label;
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        label.setForeground(MUTED);
        return label;
    }

    private static JButton button(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        button.setForeground(primary ? Color.WHITE : PURPLE);
        button.setBackground(primary ? PURPLE : new Color(242, 239, 255));
        button.setBorder(BorderFactory.createEmptyBorder(9, 15, 9, 15));
        return button;
    }

    private static JCheckBox[] checkboxes(String... labels) {
        JCheckBox[] boxes = new JCheckBox[labels.length];
        for (int i = 0; i < labels.length; i++) {
            boxes[i] = new JCheckBox(labels[i]);
            boxes[i].setBackground(Color.WHITE);
        }
        return boxes;
    }

    private static String defaultValue(int index) {
        String[] defaults = {"2", "120", "72", "25", "80", "28.5", "0.45", "35"};
        return defaults[index];
    }

    private void updateSession() {
        sessionLabel.setText(currentUser == null
                ? "Chưa đăng nhập  •  Đăng nhập tại tab Tài khoản"
                : "Xin chào, " + currentUser.path("full_name").asText()
                        + (currentUser.path("role").asText().equals("admin") ? " (quản trị viên)" : ""));
        savePredictionButton.setEnabled(currentUser != null && lastPrediction != null);
        adminPanel.setVisible(currentUser != null && currentUser.path("role").asText().equals("admin"));
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}
