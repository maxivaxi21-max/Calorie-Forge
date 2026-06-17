<?php
// calorie_calc.php - Калькулятор калорий на PHP (CLI + веб)
// CLI: php calorie_calc.php --gender=male --age=30 --height=175 --weight=75 --activity=moderate --goal=lose

$activityLevels = [
    'sedentary' => 1.2,
    'light' => 1.375,
    'moderate' => 1.55,
    'active' => 1.725,
    'very_active' => 1.9,
];
$activityNames = [
    'sedentary' => 'Сидячий образ жизни',
    'light' => 'Лёгкая активность (1-3 дня/нед)',
    'moderate' => 'Умеренная активность (3-5 дней/нед)',
    'active' => 'Высокая активность (6-7 дней/нед)',
    'very_active' => 'Очень высокая активность (тяжёлая работа/спорт)',
];
$goalNames = [
    'maintain' => 'Поддержание веса',
    'lose' => 'Похудение (дефицит 15%)',
    'gain' => 'Набор массы (профицит 15%)',
];
$genderNames = ['male' => 'Мужской', 'female' => 'Женский'];

function bmrMifflin($gender, $age, $height, $weight) {
    if ($gender == 'male') {
        return 10 * $weight + 6.25 * $height - 5 * $age + 5;
    } else {
        return 10 * $weight + 6.25 * $height - 5 * $age - 161;
    }
}

function bmrHarris($gender, $age, $height, $weight) {
    if ($gender == 'male') {
        return 88.362 + 13.397 * $weight + 4.799 * $height - 5.677 * $age;
    } else {
        return 447.593 + 9.247 * $weight + 3.098 * $height - 4.330 * $age;
    }
}

function calculateTdee($bmr, $activity) {
    global $activityLevels;
    return $bmr * $activityLevels[$activity];
}

function calculateGoalCalories($tdee, $goal) {
    switch ($goal) {
        case 'maintain': return $tdee;
        case 'lose': return $tdee * 0.85;
        case 'gain': return $tdee * 1.15;
        default: return $tdee;
    }
}

function calculateMacros($calories, $goal) {
    $proteinRatio = 0.30; $fatRatio = 0.25; $carbRatio = 0.45;
    if ($goal == 'lose') {
        $proteinRatio = 0.35; $fatRatio = 0.25; $carbRatio = 0.40;
    } elseif ($goal == 'gain') {
        $proteinRatio = 0.30; $fatRatio = 0.20; $carbRatio = 0.50;
    }
    $protein = ($calories * $proteinRatio) / 4;
    $fat = ($calories * $fatRatio) / 9;
    $carbs = ($calories * $carbRatio) / 4;
    return ['protein' => $protein, 'fat' => $fat, 'carbs' => $carbs];
}

function generateReport($params, $bmr, $tdee, $goalCal, $macros) {
    global $activityNames, $goalNames, $genderNames;
    $lines = [];
    $lines[] = str_repeat('=', 50);
    $lines[] = "📋 ОТЧЁТ ПО КАЛОРИЯМ";
    $lines[] = str_repeat('=', 50);
    $lines[] = "Пол: " . $genderNames[$params['gender']];
    $lines[] = "Возраст: " . $params['age'] . " лет";
    $lines[] = "Рост: " . $params['height'] . " см";
    $lines[] = "Вес: " . $params['weight'] . " кг";
    $lines[] = "Уровень активности: " . $activityNames[$params['activity']];
    $lines[] = "Цель: " . $goalNames[$params['goal']];
    $lines[] = str_repeat('-', 50);
    $lines[] = "BMR (базовый метаболизм): " . number_format($bmr, 1) . " ккал/день";
    $lines[] = "TDEE (суточная норма): " . number_format($tdee, 1) . " ккал/день";
    $lines[] = "Рекомендуемая калорийность для цели: " . number_format($goalCal, 1) . " ккал/день";
    $lines[] = str_repeat('-', 50);
    $lines[] = "🍽️ РАСПРЕДЕЛЕНИЕ ПО МАКРОНУТРИЕНТАМ:";
    $lines[] = "Белки: " . number_format($macros['protein'], 1) . " г (" . number_format(($macros['protein']*4/$goalCal)*100, 0) . "%)";
    $lines[] = "Жиры: " . number_format($macros['fat'], 1) . " г (" . number_format(($macros['fat']*9/$goalCal)*100, 0) . "%)";
    $lines[] = "Углеводы: " . number_format($macros['carbs'], 1) . " г (" . number_format(($macros['carbs']*4/$goalCal)*100, 0) . "%)";
    $lines[] = str_repeat('=', 50);
    return implode("\n", $lines);
}

// ========== CLI ==========
if (php_sapi_name() === 'cli') {
    $options = getopt("", ["gender:", "age:", "height:", "height-ft:", "height-in:", "weight:", "weight-lb:", "activity:", "goal:", "formula:", "output:"]);
    if (empty($options['gender']) || empty($options['age']) || empty($options['activity'])) {
        echo "Укажите --gender, --age, --activity\n";
        exit(1);
    }
    $gender = $options['gender'];
    $age = (float)$options['age'];
    $activity = $options['activity'];
    $goal = $options['goal'] ?? 'maintain';
    $formula = $options['formula'] ?? 'msj';
    $height = isset($options['height']) ? (float)$options['height'] : 0;
    $weight = isset($options['weight']) ? (float)$options['weight'] : 0;
    if ($height == 0 && isset($options['height-ft'])) {
        $ft = (int)$options['height-ft'];
        $inc = isset($options['height-in']) ? (int)$options['height-in'] : 0;
        $height = $ft * 30.48 + $inc * 2.54;
    }
    if ($weight == 0 && isset($options['weight-lb'])) {
        $weight = (float)$options['weight-lb'] * 0.453592;
    }
    if ($height <= 0 || $weight <= 0) {
        echo "Укажите рост и вес (метрические или имперские)\n";
        exit(1);
    }
    $params = ['gender' => $gender, 'age' => $age, 'height' => $height, 'weight' => $weight, 'activity' => $activity, 'goal' => $goal];
    $bmr = $formula == 'msj' ? bmrMifflin($gender, $age, $height, $weight) : bmrHarris($gender, $age, $height, $weight);
    $tdee = calculateTdee($bmr, $activity);
    $goalCal = calculateGoalCalories($tdee, $goal);
    $macros = calculateMacros($goalCal, $goal);
    $report = generateReport($params, $bmr, $tdee, $goalCal, $macros);
    echo $report . "\n";
    if (!empty($options['output'])) {
        file_put_contents($options['output'], $report);
        echo "Отчёт сохранён в {$options['output']}\n";
    }
    exit;
}

// ========== ВЕБ-ИНТЕРФЕЙС ==========
?>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Калькулятор калорий (PHP)</title>
    <style>
        body { font-family: 'Segoe UI', sans-serif; background: #f4f7fb; margin: 40px; }
        .container { max-width: 500px; margin: 0 auto; background: white; padding: 20px; border-radius: 16px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
        label { display: inline-block; width: 100px; }
        input, select, button { margin: 8px 0; padding: 6px; }
        button { background: #3498db; color: white; border: none; padding: 8px 20px; border-radius: 4px; cursor: pointer; }
        .result { background: #e8f5e9; padding: 10px; border-radius: 8px; margin-top: 10px; white-space: pre-wrap; font-family: monospace; }
    </style>
</head>
<body>
<div class="container">
    <h1>🔥 Калькулятор калорий</h1>
    <form method="GET">
        <label>Пол:</label>
        <select name="gender">
            <option value="male" <?= isset($_GET['gender']) && $_GET['gender']=='male' ? 'selected' : '' ?>>Мужской</option>
            <option value="female" <?= isset($_GET['gender']) && $_GET['gender']=='female' ? 'selected' : '' ?>>Женский</option>
        </select><br>
        <label>Возраст (лет):</label>
        <input type="number" name="age" value="<?= isset($_GET['age']) ? $_GET['age'] : '' ?>" step="1"><br>
        <label>Рост (см):</label>
        <input type="number" name="height" value="<?= isset($_GET['height']) ? $_GET['height'] : '' ?>" step="0.1"><br>
        <label>Вес (кг):</label>
        <input type="number" name="weight" value="<?= isset($_GET['weight']) ? $_GET['weight'] : '' ?>" step="0.1"><br>
        <label>Активность:</label>
        <select name="activity">
            <option value="sedentary" <?= isset($_GET['activity']) && $_GET['activity']=='sedentary' ? 'selected' : '' ?>>Сидячий</option>
            <option value="light" <?= isset($_GET['activity']) && $_GET['activity']=='light' ? 'selected' : '' ?>>Лёгкая</option>
            <option value="moderate" <?= isset($_GET['activity']) && $_GET['activity']=='moderate' ? 'selected' : '' ?>>Умеренная</option>
            <option value="active" <?= isset($_GET['activity']) && $_GET['activity']=='active' ? 'selected' : '' ?>>Высокая</option>
            <option value="very_active" <?= isset($_GET['activity']) && $_GET['activity']=='very_active' ? 'selected' : '' ?>>Очень высокая</option>
        </select><br>
        <label>Цель:</label>
        <select name="goal">
            <option value="maintain" <?= isset($_GET['goal']) && $_GET['goal']=='maintain' ? 'selected' : '' ?>>Поддержание</option>
            <option value="lose" <?= isset($_GET['goal']) && $_GET['goal']=='lose' ? 'selected' : '' ?>>Похудение</option>
            <option value="gain" <?= isset($_GET['goal']) && $_GET['goal']=='gain' ? 'selected' : '' ?>>Набор массы</option>
        </select><br>
        <label>Формула:</label>
        <select name="formula">
            <option value="msj" <?= isset($_GET['formula']) && $_GET['formula']=='msj' ? 'selected' : '' ?>>Mifflin-St Jeor</option>
            <option value="hb" <?= isset($_GET['formula']) && $_GET['formula']=='hb' ? 'selected' : '' ?>>Harris-Benedict</option>
        </select><br>
        <button type="submit">Рассчитать</button>
    </form>
    <?php if (isset($_GET['gender']) && isset($_GET['age']) && isset($_GET['height']) && isset($_GET['weight']) && isset($_GET['activity'])): 
        $gender = $_GET['gender'];
        $age = (float)$_GET['age'];
        $height = (float)$_GET['height'];
        $weight = (float)$_GET['weight'];
        $activity = $_GET['activity'];
        $goal = $_GET['goal'] ?? 'maintain';
        $formula = $_GET['formula'] ?? 'msj';
        $params = ['gender' => $gender, 'age' => $age, 'height' => $height, 'weight' => $weight, 'activity' => $activity, 'goal' => $goal];
        $bmr = $formula == 'msj' ? bmrMifflin($gender, $age, $height, $weight) : bmrHarris($gender, $age, $height, $weight);
        $tdee = calculateTdee($bmr, $activity);
        $goalCal = calculateGoalCalories($tdee, $goal);
        $macros = calculateMacros($goalCal, $goal);
        $report = generateReport($params, $bmr, $tdee, $goalCal, $macros);
    ?>
        <div class="result"><?= htmlspecialchars($report) ?></div>
    <?php endif; ?>
</div>
</body>
</html>
