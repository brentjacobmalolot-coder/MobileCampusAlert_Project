package com.example.campusalert.utils;

import android.content.Context;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.nio.MappedByteBuffer;
import java.util.List;

public class PriorityClassifier {
    private static final String TAG = "PriorityClassifier";
    private static final String MODEL_FILE = "priority_model.tflite";
    private static final String LABEL_FILE = "labels.txt";

    private Interpreter interpreter;
    private List<String> labels;

    public PriorityClassifier(Context context) {
        try {
            MappedByteBuffer model = FileUtil.loadMappedFile(context, MODEL_FILE);
            interpreter = new Interpreter(model);
            labels = FileUtil.loadLabels(context, LABEL_FILE);
            Log.d(TAG, "Model and labels loaded successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Critical error loading TFLite model or labels: " + e.getMessage(), e);
            // Ensure interpreter is null if loading failed
            if (interpreter != null) {
                interpreter.close();
                interpreter = null;
            }
        }
    }

    public String classify(String description) {
        if (interpreter == null || labels == null || labels.isEmpty()) {
            return "Medium"; // Fallback
        }

        try {
            // The user's snippet suggested this input format: arrayOf(arrayOf("text"))
            // In Java, that's Object[][] or String[][]
            String[][] input = {{description}};
            float[][] output = new float[1][labels.size()];

            interpreter.run(input, output);

            Log.d(TAG, "Inference results for: " + description);
            int outputSize = output[0].length;
            for (int i = 0; i < outputSize; i++) {
                String labelStr = (labels != null && i < labels.size()) ? labels.get(i) : "Unknown";
                Log.d(TAG, labelStr + ": " + output[0][i]);
            }

            int predictedIndex = -1;
            float maxProb = -1f;
            for (int i = 0; i < output[0].length; i++) {
                if (output[0][i] > maxProb) {
                    maxProb = output[0][i];
                    predictedIndex = i;
                }
            }

            if (predictedIndex == -1) {
                return "Medium";
            }

            String label = labels.get(predictedIndex);
            
            // Map labels to Report priority format (Capitalized case)
            if ("CRITICAL".equalsIgnoreCase(label) || "HIGH".equalsIgnoreCase(label)) {
                return "High";
            } else if ("MEDIUM".equalsIgnoreCase(label)) {
                return "Medium";
            } else if ("LOW".equalsIgnoreCase(label)) {
                return "Low";
            }
            
            return "Medium";
        } catch (Exception e) {
            Log.e(TAG, "Error during inference", e);
            return "Medium";
        }
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
    }
}
