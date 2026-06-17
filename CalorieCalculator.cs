// CalorieCalculator.cs - Калькулятор калорий на C# (CLI + WinForms)
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Windows.Forms;

namespace CalorieCalculator
{
    public static class Calculator
    {
        public static Dictionary<string, double> ActivityLevels = new Dictionary<string, double>
        {
            {"sedentary", 1.2}, {"light", 1.375}, {"moderate", 1.55},
            {"active", 1.725}, {"very_active", 1.9}
        };
        public static Dictionary<string, string> ActivityNames = new Dictionary<string, string>
        {
            {"sedentary", "Сидячий образ жизни"},
            {"light", "Лёгкая активность (1-3 дня/нед)"},
            {"moderate", "Умеренная активность (3-5 дней/нед)"},
            {"active", "Высокая активность (6-7 дней/нед)"},
            {"very_active", "Очень высокая активность (тяжёлая работа/спорт)"}
        };
        public static Dictionary<string, string> GoalNames = new Dictionary<string, string>
        {
            {"maintain", "Поддержание веса"},
            {"lose", "Похудение (дефицит 15%)"},
            {"gain", "Набор массы (профицит 15%)"}
        };
        public static Dictionary<string, string> GenderNames = new Dictionary<string, string>
        {
            {"male", "Мужской"}, {"female", "Женский"}
        };

        public static double BmrMifflin(string gender, double age, double height, double weight)
        {
            if (gender == "male")
                return 10 * weight + 6.25 * height - 5 * age + 5;
            else
                return 10 * weight + 6.25 * height - 5 * age - 161;
        }

        public static double BmrHarris(string gender, double age, double height, double weight)
        {
            if (gender == "male")
                return 88.362 + 13.397 * weight + 4.799 * height - 5.677 * age;
            else
                return 447.593 + 9.247 * weight + 3.098 * height - 4.330 * age;
        }

        public static double CalculateTdee(double bmr, string activity)
        {
            return bmr * ActivityLevels[activity];
        }

        public static double CalculateGoalCalories(double tdee, string goal)
        {
            switch (goal)
            {
                case "maintain": return tdee;
                case "lose": return tdee * 0.85;
                case "gain": return tdee * 1.15;
                default: return tdee;
            }
        }

        public static (double protein, double fat, double carbs) CalculateMacros(double calories, string goal)
        {
            double proteinRatio = 0.30, fatRatio = 0.25, carbRatio = 0.45;
            if (goal == "lose")
            { proteinRatio = 0.35; fatRatio = 0.25; carbRatio = 0.40; }
            else if (goal == "gain")
            { proteinRatio = 0.30; fatRatio = 0.20; carbRatio = 0.50; }
            double protein = (calories * proteinRatio) / 4;
            double fat = (calories * fatRatio) / 9;
            double carbs = (calories * carbRatio) / 4;
            return (protein, fat, carbs);
        }

        public static string GenerateReport(string gender, double age, double height, double weight, string activity, string goal,
                                            double bmr, double tdee, double goalCal, double protein, double fat, double carbs)
        {
            var lines = new List<string>();
            lines.Add(new string('=', 50));
            lines.Add("📋 ОТЧЁТ ПО КАЛОРИЯМ");
            lines.Add(new string('=', 50));
            lines.Add($"Пол: {GenderNames[gender]}");
            lines.Add($"Возраст: {age:F0} лет");
            lines.Add($"Рост: {height:F1} см");
            lines.Add($"Вес: {weight:F1} кг");
            lines.Add($"Уровень активности: {ActivityNames[activity]}");
            lines.Add($"Цель: {GoalNames[goal]}");
            lines.Add(new string('-', 50));
            lines.Add($"BMR (базовый метаболизм): {bmr:F1} ккал/день");
            lines.Add($"TDEE (суточная норма): {tdee:F1} ккал/день");
            lines.Add($"Рекомендуемая калорийность для цели: {goalCal:F1} ккал/день");
            lines.Add(new string('-', 50));
            lines.Add("🍽️ РАСПРЕДЕЛЕНИЕ ПО МАКРОНУТРИЕНТАМ:");
            lines.Add($"Белки: {protein:F1} г ({((protein*4/goalCal)*100):F0}%)");
            lines.Add($"Жиры: {fat:F1} г ({((fat*9/goalCal)*100):F0}%)");
            lines.Add($"Углеводы: {carbs:F1} г ({((carbs*4/goalCal)*100):F0}%)");
            lines.Add(new string('=', 50));
            return string.Join("\n", lines);
        }
    }

    class Program
    {
        [STAThread]
        static void Main(string[] args)
        {
            if (args.Length > 0 && args[0] == "--gui")
            {
                Application.EnableVisualStyles();
                Application.Run(new CalorieGUI());
                return;
            }
            // CLI (упрощённый)
            var opts = new Dictionary<string, string>();
            for (int i = 0; i < args.Length; i++)
            {
                if (args[i].StartsWith("--"))
                {
                    if (i + 1 < args.Length && !args[i+1].StartsWith("--"))
                        opts[args[i].Substring(2)] = args[++i];
                    else
                        opts[args[i].Substring(2)] = "";
                }
            }
            try
            {
                string gender = opts.GetValueOrDefault("gender");
                if (!double.TryParse(opts.GetValueOrDefault("age"), out double age) || age <= 0) throw new Exception("Возраст обязателен");
                string activity = opts.GetValueOrDefault("activity");
                if (string.IsNullOrEmpty(gender) || string.IsNullOrEmpty(activity)) throw new Exception("Укажите --gender, --age, --activity");
                double height = 0, weight = 0;
                if (opts.ContainsKey("height"))
                    height = double.Parse(opts["height"]);
                else if (opts.ContainsKey("height-ft"))
                {
                    int ft = int.Parse(opts["height-ft"]);
                    int inc = opts.ContainsKey("height-in") ? int.Parse(opts["height-in"]) : 0;
                    height = ft * 30.48 + inc * 2.54;
                }
                if (opts.ContainsKey("weight"))
                    weight = double.Parse(opts["weight"]);
                else if (opts.ContainsKey("weight-lb"))
                    weight = double.Parse(opts["weight-lb"]) * 0.453592;
                if (height <= 0 || weight <= 0) throw new Exception("Укажите рост и вес");

                string goal = opts.GetValueOrDefault("goal", "maintain");
                string formula = opts.GetValueOrDefault("formula", "msj");
                string output = opts.GetValueOrDefault("output");

                double bmr = formula == "msj" ? Calculator.BmrMifflin(gender, age, height, weight) : Calculator.BmrHarris(gender, age, height, weight);
                double tdee = Calculator.CalculateTdee(bmr, activity);
                double goalCal = Calculator.CalculateGoalCalories(tdee, goal);
                var macros = Calculator.CalculateMacros(goalCal, goal);
                string report = Calculator.GenerateReport(gender, age, height, weight, activity, goal, bmr, tdee, goalCal, macros.protein, macros.fat, macros.carbs);
                Console.WriteLine(report);
                if (!string.IsNullOrEmpty(output))
                {
                    File.WriteAllText(output, report);
                    Console.WriteLine($"Отчёт сохранён в {output}");
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Ошибка: {ex.Message}");
            }
        }
    }

    // ========== GUI ==========
    public class CalorieGUI : Form
    {
        private ComboBox genderBox, activityBox, goalBox, formulaBox;
        private TextBox ageBox, heightBox, weightBox;
        private TextBox resultBox;

        public CalorieGUI()
        {
            Text = "Калькулятор калорий";
            Size = new System.Drawing.Size(500, 550);
            StartPosition = FormStartPosition.CenterScreen;

            var layout = new TableLayoutPanel { Dock = DockStyle.Fill, ColumnCount = 2, RowCount = 9, Padding = new Padding(10) };
            layout.RowStyles.Add(new RowStyle(SizeType.AutoSize));
            layout.RowStyles.Add(new RowStyle(SizeType.AutoSize));
            layout.RowStyles.Add(new RowStyle(SizeType.AutoSize));
            layout.RowStyles.Add(new RowStyle(SizeType.AutoSize));
            layout.RowStyles.Add(new RowStyle(SizeType.AutoSize));
            layout.RowStyles.Add(new RowStyle(SizeType.AutoSize));
            layout.RowStyles.Add(new RowStyle(SizeType.AutoSize));
            layout.RowStyles.Add(new RowStyle(SizeType.Percent, 100));
            layout.RowStyles.Add(new RowStyle(SizeType.AutoSize));

            layout.Controls.Add(new Label { Text = "Пол:", AutoSize = true }, 0, 0);
            genderBox = new ComboBox { DropDownStyle = ComboBoxStyle.DropDownList, Items = { "male", "female" }, SelectedIndex = 0 };
            layout.Controls.Add(genderBox, 1, 0);

            layout.Controls.Add(new Label { Text = "Возраст (лет):", AutoSize = true }, 0, 1);
            ageBox = new TextBox();
            layout.Controls.Add(ageBox, 1, 1);

            layout.Controls.Add(new Label { Text = "Рост (см):", AutoSize = true }, 0, 2);
            heightBox = new TextBox();
            layout.Controls.Add(heightBox, 1, 2);

            layout.Controls.Add(new Label { Text = "Вес (кг):", AutoSize = true }, 0, 3);
            weightBox = new TextBox();
            layout.Controls.Add(weightBox, 1, 3);

            layout.Controls.Add(new Label { Text = "Активность:", AutoSize = true }, 0, 4);
            activityBox = new ComboBox { DropDownStyle = ComboBoxStyle.DropDownList, Items = { "sedentary", "light", "moderate", "active", "very_active" }, SelectedIndex = 2 };
            layout.Controls.Add(activityBox, 1, 4);

            layout.Controls.Add(new Label { Text = "Цель:", AutoSize = true }, 0, 5);
            goalBox = new ComboBox { DropDownStyle = ComboBoxStyle.DropDownList, Items = { "maintain", "lose", "gain" }, SelectedIndex = 0 };
            layout.Controls.Add(goalBox, 1, 5);

            layout.Controls.Add(new Label { Text = "Формула:", AutoSize = true }, 0, 6);
            formulaBox = new ComboBox { DropDownStyle = ComboBoxStyle.DropDownList, Items = { "msj", "hb" }, SelectedIndex = 0 };
            layout.Controls.Add(formulaBox, 1, 6);

            var calcBtn = new Button { Text = "Рассчитать" };
            calcBtn.Click += (s, e) => Calculate();
            layout.Controls.Add(calcBtn, 0, 7);
            layout.SetColumnSpan(calcBtn, 2);

            resultBox = new TextBox { Multiline = true, ReadOnly = true, ScrollBars = ScrollBars.Vertical, Dock = DockStyle.Fill };
            layout.Controls.Add(resultBox, 0, 8);
            layout.SetColumnSpan(resultBox, 2);

            var saveBtn = new Button { Text = "Сохранить в файл" };
            saveBtn.Click += (s, e) => SaveResult();
            layout.Controls.Add(saveBtn, 0, 9);
            layout.SetColumnSpan(saveBtn, 2);

            Controls.Add(layout);
        }

        private void Calculate()
        {
            try
            {
                string gender = genderBox.SelectedItem.ToString();
                double age = double.Parse(ageBox.Text);
                double height = double.Parse(heightBox.Text);
                double weight = double.Parse(weightBox.Text);
                string activity = activityBox.SelectedItem.ToString();
                string goal = goalBox.SelectedItem.ToString();
                string formula = formulaBox.SelectedItem.ToString();

                double bmr = formula == "msj" ? Calculator.BmrMifflin(gender, age, height, weight) : Calculator.BmrHarris(gender, age, height, weight);
                double tdee = Calculator.CalculateTdee(bmr, activity);
                double goalCal = Calculator.CalculateGoalCalories(tdee, goal);
                var macros = Calculator.CalculateMacros(goalCal, goal);
                string report = Calculator.GenerateReport(gender, age, height, weight, activity, goal, bmr, tdee, goalCal, macros.protein, macros.fat, macros.carbs);
                resultBox.Text = report;
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Ошибка: {ex.Message}");
            }
        }

        private void SaveResult()
        {
            if (string.IsNullOrEmpty(resultBox.Text))
            {
                MessageBox.Show("Нет данных для сохранения");
                return;
            }
            var sfd = new SaveFileDialog { Filter = "Text files|*.txt", DefaultExt = "txt" };
            if (sfd.ShowDialog() == DialogResult.OK)
            {
                File.WriteAllText(sfd.FileName, resultBox.Text);
                MessageBox.Show("Сохранено");
            }
        }
    }
}
