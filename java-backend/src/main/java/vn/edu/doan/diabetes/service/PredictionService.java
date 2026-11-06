package vn.edu.doan.diabetes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import vn.edu.doan.diabetes.model.PatientInput;
import vn.edu.doan.diabetes.model.PredictionResult;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PredictionService {
    private static final String[] FEATURES = {
            "Pregnancies", "Glucose", "BloodPressure", "SkinThickness",
            "Insulin", "BMI", "DiabetesPedigreeFunction", "Age"
    };

    private final ModelData model;

    public PredictionService(ObjectMapper objectMapper) {
        try {
            this.model = objectMapper.readValue(new ClassPathResource("model.json").getInputStream(), ModelData.class);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tải mô hình dự đoán Java", exception);
        }
    }

    public PredictionResult predict(PatientInput patient) {
        double[] input = {
                patient.pregnancies(), patient.glucose(), patient.bloodPressure(), patient.skinThickness(),
                patient.insulin(), patient.bmi(), patient.diabetesPedigree(), patient.age()
        };
        double[] scaled = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            scaled[i] = (float) ((input[i] - model.mean[i]) / model.scale[i]);
        }
        double probability = 0;
        for (TreeData tree : model.trees) {
            int node = 0;
            while (tree.left[node] >= 0) {
                int feature = tree.feature[node];
                node = scaled[feature] <= tree.threshold[node]
                        ? tree.left[node] : tree.right[node];
            }
            probability += tree.positive[node] / tree.total[node];
        }
        probability /= model.trees.length;
        double percentage = Math.round(probability * 10000.0) / 100.0;
        String level = percentage < 30 ? "Thấp" : percentage < 60 ? "Trung bình" : "Cao";
        Map<String, Double> importance = new LinkedHashMap<>();
        for (int i = 0; i < FEATURES.length; i++) {
            importance.put(FEATURES[i], Math.round(model.importance[i] * 10000.0) / 10000.0);
        }
        return new PredictionResult(probability, level, percentage, recommendation(patient, level, percentage), importance);
    }

    private static String recommendation(PatientInput patient, String level, double percentage) {
        String glucose = patient.glucose() >= 126
                ? "Glucose đang ở ngưỡng cảnh báo cao; nên trao đổi với bác sĩ về xét nghiệm xác nhận."
                : patient.glucose() >= 100
                ? "Glucose cao hơn mức lý tưởng; nên điều chỉnh ăn uống và tái kiểm tra định kỳ."
                : "Glucose chưa ở nhóm cảnh báo thường gặp.";
        String weight = patient.bmi() >= 30 ? "BMI thuộc nhóm béo phì."
                : patient.bmi() >= 25 ? "BMI thuộc nhóm thừa cân." : "BMI chưa ở nhóm thừa cân.";
        String visit = level.equals("Cao")
                ? "Nên sớm gặp bác sĩ để được tư vấn và làm xét nghiệm xác nhận."
                : level.equals("Trung bình")
                ? "Nên theo dõi các chỉ số và cân nhắc tái kiểm tra trong 3-6 tháng."
                : "Duy trì khám sức khỏe định kỳ và lối sống lành mạnh.";
        return "Tư vấn tham khảo — mức nguy cơ " + level + " (" + percentage + "%).\n\n"
                + glucose + " " + weight + "\n\n"
                + "Ưu tiên rau xanh, đạm nạc, ngũ cốc nguyên hạt; hạn chế đồ uống có đường và thực phẩm tinh chế. "
                + "Duy trì vận động phù hợp, ngủ đủ giấc và theo dõi đường huyết.\n\n"
                + visit + "\n\nKết quả chỉ mang tính tham khảo, không thay thế chẩn đoán của nhân viên y tế.";
    }

    public static class ModelData {
        public double[] mean;
        public double[] scale;
        public double[] importance;
        public TreeData[] trees;
    }

    public static class TreeData {
        public int[] left;
        public int[] right;
        public int[] feature;
        public double[] threshold;
        public double[] positive;
        public double[] total;
    }
}
