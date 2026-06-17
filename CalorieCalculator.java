// CalorieCalculator.java - Калькулятор калорий на Java (CLI + Swing GUI)
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class CalorieCalculator {
    private static final Map<String, Double> ACTIVITY_LEVELS = new LinkedHashMap<>();
    private static final Map<String, String> ACTIVITY_NAMES = new LinkedHashMap<>();
    private static final Map<String, String> GOAL_NAMES = new LinkedHashMap<>();
    private static final Map<String, String> GENDER_NAMES = new LinkedHashMap<>();
    static {
        ACTIVITY_LEVELS.put("sedentary", 1.2);
        ACTIVITY_LEVELS.put("light", 1.375);
        ACTIVITY_LEVELS.put("moderate", 1.55);
        ACTIVITY_LEVELS.put("active", 1.725);
        ACTIVITY_LEVELS.put("very_active", 1.9);
        ACTIVITY_NAMES.put("sedentary", "Сидячий образ жизни");
        ACTIVITY_NAMES.put("light", "Лёгкая активность (1-3 дня/нед)");
        ACTIVITY_NAMES.put("moderate", "Умеренная активность (3-5 дней/нед)");
        ACTIVITY_NAMES.put("active", "Высокая активность (6-7 дней/нед)");
        ACTIVITY_NAMES.put("very_active", "Очень высокая активность (тяжёлая работа/спорт)");
        GOAL_NAMES.put("maintain", "Поддержание веса");
        GOAL_NAMES.put("lose", "Похудение (дефицит 15%)");
        GOAL_NAMES.put("gain", "Набор массы (профицит 15%)");
        GENDER_NAMES.put("male", "Мужской");
        GENDER_NAMES.put("female", "Женский");
    }

    public static double bmrMifflin(String gender, double age, double height, double weight) {
        if (gender.equals("male")) {
            return 10 * weight + 6.25 * height - 5 * age + 5;
        } else {
            return 10 * weight + 6.25 * height - 5 * age - 161;
        }
    }

    public static double bmrHarris(String gender, double age, double height, double weight) {
        if (gender.equals("male")) {
            return 88.362 + 13.397 * weight + 4.799 * height - 5.677 * age;
        } else {
            return 447.593 + 9.247 * weight + 3.098 * height - 4.330 * age;
        }
    }

    public static double calculateTdee(double bmr, String activity) {
        return bmr * ACTIVITY_LEVELS.get(activity);
    }

    public static double calculateGoalCalories(double tdee, String goal) {
        switch (goal) {
            case "maintain": return tdee;
            case "lose": return tdee * 0.85;
            case "gain": return tdee * 1.15;
            default: return tdee;
        }
    }

    public static double[] calculateMacros(double calories, String goal) {
        double proteinRatio = 0.30, fatRatio = 0.25, carbRatio = 0.45;
        if (goal.equals("lose")) {
            proteinRatio = 0.35; fatRatio = 0.25; carbRatio = 0.40;
        } else if (goal.equals("gain")) {
            proteinRatio = 0.30; fatRatio = 0.20; carbRatio = 0.50;
        }
        double protein = (calories * proteinRatio) / 4;
        double fat = (calories * fatRatio) / 9;
        double carbs = (calories * carbRatio) / 4;
        return new double[]{protein, fat, carbs};
    }

    public static String generateReport(Map<String, Object> params, double bmr, double tdee, double goalCal, double[] macros) {
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(50)).append("\n");
        sb.append("📋 ОТЧЁТ ПО КАЛОРИЯМ\n");
        sb.append("=".repeat(50)).append("\n");
        sb.append("Пол: ").append(GENDER_NAMES.get(params.get("gender"))).append("\n");
        sb.append("Возраст: ").append(params.get("age")).append(" лет\n");
        sb.append("Рост: ").append(params.get("height")).append(" см\n");
        sb.append("Вес: ").append(params.get("weight")).append(" кг\n");
        sb.append("Уровень активности: ").append(ACTIVITY_NAMES.get(params.get("activity"))).append("\n");
        sb.append("Цель: ").append(GOAL_NAMES.get(params.get("goal"))).append("\n");
        sb.append("-".repeat(50)).append("\n");
        sb.append("BMR (базовый метаболизм): ").append(String.format("%.1f", bmr)).append(" ккал/день\n");
        sb.append("TDEE (суточная норма): ").append(String.format("%.1f", tdee)).append(" ккал/день\n");
        sb.append("Рекомендуемая калорийность для цели: ").append(String.format("%.1f", goalCal)).append(" ккал/день\n");
        sb.append("-".repeat(50)).append("\n");
        sb.append("🍽️ РАСПРЕДЕЛЕНИЕ ПО МАКРОНУТРИЕНТАМ:\n");
        sb.append("Белки: ").append(String.format("%.1f", macros[0])).append(" г (").append(String.format("%.0f", (macros[0]*4/goalCal)*100)).append("%)\n");
        sb.append("Жиры: ").append(String.format("%.1f", macros[1])).append(" г (").append(String.format("%.0f", (macros[1]*9/goalCal)*100)).append("%)\n");
        sb.append("Углеводы: ").append(String.format("%.1f", macros[2])).append(" г (").append(String.format("%.0f", (macros[2]*4/goalCal)*100)).append("%)\n");
        sb.append("=".repeat(50)).append("\n");
        return sb.toString();
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--gui")) {
            SwingUtilities.invokeLater(() -> new CalorieGUI().setVisible(true));
            return;
        }
        // CLI (упрощённый парсер)
        Map<String, String> opts = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                if (i + 1 < args.length && !args[i+1].startsWith("--")) {
                    opts.put(args[i].substring(2), args[++i]);
                } else {
                    opts.put(args[i].substring(2), "");
                }
            }
        }
        try {
            String gender = opts.get("gender");
            double age = Double.parseDouble(opts.get("age"));
            String activity = opts.get("activity");
            if (gender == null || age == 0 || activity == null) {
                System.err.println("Укажите --gender, --age, --activity");
                return;
            }
            double height = opts.containsKey("height") ? Double.parseDouble(opts.get("height")) : 0;
            double weight = opts.containsKey("weight") ? Double.parseDouble(opts.get("weight")) : 0;
            if (height == 0) {
                int ft = opts.containsKey("height-ft") ? Integer.parseInt(opts.get("height-ft")) : 0;
                int inc = opts.containsKey("height-in") ? Integer.parseInt(opts.get("height-in")) : 0;
                height = ft * 30.48 + inc * 2.54;
            }
            if (weight == 0) {
                double lb = opts.containsKey("weight-lb") ? Double.parseDouble(opts.get("weight-lb")) : 0;
                weight = lb * 0.453592;
            }
            String goal = opts.getOrDefault("goal", "maintain");
            String formula = opts.getOrDefault("formula", "msj");
            String output = opts.get("output");

            Map<String, Object> params = new HashMap<>();
            params.put("gender", gender);
            params.put("age", age);
            params.put("height", height);
            params.put("weight", weight);
            params.put("activity", activity);
            params.put("goal", goal);

            double bmr = formula.equals("msj") ? bmrMifflin(gender, age, height, weight) : bmrHarris(gender, age, height, weight);
            double tdee = calculateTdee(bmr, activity);
            double goalCal = calculateGoalCalories(tdee, goal);
            double[] macros = calculateMacros(goalCal, goal);
            String report = generateReport(params, bmr, tdee, goalCal, macros);
            System.out.println(report);
            if (output != null) {
                try (PrintWriter pw = new PrintWriter(output)) {
                    pw.print(report);
                    System.out.println("Отчёт сохранён в " + output);
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
        }
    }

    // ========== GUI ==========
    static class CalorieGUI extends JFrame {
        private JComboBox<String> genderBox, activityBox, goalBox, formulaBox;
        private JTextField ageField, heightField, weightField;
        private JTextArea resultArea;

        public CalorieGUI() {
            setTitle("Калькулятор калорий");
            setSize(500, 600);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0; gbc.gridy = 0;
            add(new JLabel("Пол:"), gbc);
            gbc.gridx = 1;
            genderBox = new JComboBox<>(new String[]{"male", "female"});
            add(genderBox, gbc);
            gbc.gridx = 0; gbc.gridy = 1;
            add(new JLabel("Возраст (лет):"), gbc);
            gbc.gridx = 1;
            ageField = new JTextField(10);
            add(ageField, gbc);
            gbc.gridx = 0; gbc.gridy = 2;
            add(new JLabel("Рост (см):"), gbc);
            gbc.gridx = 1;
            heightField = new JTextField(10);
            add(heightField, gbc);
            gbc.gridx = 0; gbc.gridy = 3;
            add(new JLabel("Вес (кг):"), gbc);
            gbc.gridx = 1;
            weightField = new JTextField(10);
            add(weightField, gbc);
            gbc.gridx = 0; gbc.gridy = 4;
            add(new JLabel("Активность:"), gbc);
            gbc.gridx = 1;
            activityBox = new JComboBox<>(ACTIVITY_LEVELS.keySet().toArray(new String[0]));
            add(activityBox, gbc);
            gbc.gridx = 0; gbc.gridy = 5;
            add(new JLabel("Цель:"), gbc);
            gbc.gridx = 1;
            goalBox = new JComboBox<>(new String[]{"maintain", "lose", "gain"});
            add(goalBox, gbc);
            gbc.gridx = 0; gbc.gridy = 6;
            add(new JLabel("Формула:"), gbc);
            gbc.gridx = 1;
            formulaBox = new JComboBox<>(new String[]{"msj", "hb"});
            add(formulaBox, gbc);
            gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
            JButton calcBtn = new JButton("Рассчитать");
            calcBtn.addActionListener(e -> calculate());
            add(calcBtn, gbc);
            gbc.gridy = 8;
            resultArea = new JTextArea(15, 40);
            resultArea.setEditable(false);
            JScrollPane scroll = new JScrollPane(resultArea);
            add(scroll, gbc);
            gbc.gridy = 9;
            JButton saveBtn = new JButton("Сохранить в файл");
            saveBtn.addActionListener(e -> saveResult());
            add(saveBtn, gbc);
        }

        private void calculate() {
            try {
                String gender = (String) genderBox.getSelectedItem();
                double age = Double.parseDouble(ageField.getText());
                double height = Double.parseDouble(heightField.getText());
                double weight = Double.parseDouble(weightField.getText());
                String activity = (String) activityBox.getSelectedItem();
                String goal = (String) goalBox.getSelectedItem();
                String formula = (String) formulaBox.getSelectedItem();

                Map<String, Object> params = new HashMap<>();
                params.put("gender", gender);
                params.put("age", age);
                params.put("height", height);
                params.put("weight", weight);
                params.put("activity", activity);
                params.put("goal", goal);

                double bmr = formula.equals("msj") ? bmrMifflin(gender, age, height, weight) : bmrHarris(gender, age, height, weight);
                double tdee = calculateTdee(bmr, activity);
                double goalCal = calculateGoalCalories(tdee, goal);
                double[] macros = calculateMacros(goalCal, goal);
                String report = generateReport(params, bmr, tdee, goalCal, macros);
                resultArea.setText(report);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Введите корректные числа");
            }
        }

        private void saveResult() {
            String content = resultArea.getText();
            if (content.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Нет данных для сохранения");
                return;
            }
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try (PrintWriter pw = new PrintWriter(fc.getSelectedFile())) {
                    pw.print(content);
                    JOptionPane.showMessageDialog(this, "Сохранено");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка сохранения: " + ex.getMessage());
                }
            }
        }
    }
}
