Calorie Forge — это коллекция из семи независимых реализаций интеллектуального калькулятора калорий, каждая из которых работает на своём языке программирования. Проект создан для демонстрации кроссплатформенного подхода и предоставления удобного инструмента для расчёта суточной нормы калорий с учётом индивидуальных параметров.

✨ Уникальные возможности (общие для всех версий)
🧬 Расчёт базового метаболизма (BMR) по формулам:

Mifflin‑St Jeor (наиболее точная для современного человека)

Harris‑Benedict (классическая, пересмотренная)

🏃 Учёт уровня физической активности (от сидячего образа до интенсивных тренировок) с коэффициентами от 1.2 до 1.9.

🎯 Цели:

Поддержание веса

Похудение (дефицит 10–20%)

Набор массы (профицит 10–20%)

📊 Подробный отчёт: BMR, TDEE (суточная норма), рекомендуемая калорийность для цели, распределение по макронутриентам (белки, жиры, углеводы).

🌍 Поддержка метрических (см, кг) и имперских (футы, дюймы, фунты) единиц.

💾 Сохранение результатов в файл (TXT, CSV, JSON) и загрузка параметров (в некоторых версиях).

🖥️ Интерфейсы:

Командная строка (CLI) с богатыми опциями

Графический интерфейс (GUI) в Python, Java и C#

Веб-интерфейс в PHP (и браузерный JavaScript)

📋 Сравнение реализаций
Язык	CLI	GUI	Формулы	Сохранение	Имперские единицы
Python	✅	✅ (Tkinter)	MSJ, HB	TXT, CSV	✅
JavaScript	✅ (Node.js)	❌	MSJ, HB	TXT	✅
Go	✅	❌	MSJ, HB	TXT	✅
Rust	✅	❌	MSJ, HB	TXT	✅
Java	✅	✅ (Swing)	MSJ, HB	TXT, CSV	✅
C#	✅	✅ (WinForms)	MSJ, HB	TXT, CSV	✅
PHP	✅	✅ (веб)	MSJ, HB	TXT	✅
🚀 Быстрый старт
Установка зависимостей (при необходимости)
Python
bash
pip install tkinter  # (опционально для GUI)
JavaScript (Node.js)
bash
npm install commander
Go
bash
go get -u
Rust
bash
cargo add clap
Java
Не требует внешних библиотек (используется только стандартный java и javax.swing).

C#
Не требует внешних библиотек (используется System.Windows.Forms).

PHP
Не требует внешних библиотек (используется только стандартный PHP).

Использование (общий синтаксис)
CLI (все версии):

bash
calorie_calc --gender male --age 30 --height 175 --weight 75 --activity moderate --goal lose
Примеры:

Рассчитать норму для женщины 25 лет, рост 165 см, вес 60 кг, активность средняя, цель — похудение:

bash
python calorie_calc.py --gender female --age 25 --height 165 --weight 60 --activity moderate --goal lose
Использовать имперские единицы (футы/дюймы, фунты):

bash
go run calorie_calc.go -gender male -age 35 -height-ft 5 -height-in 10 -weight 180 -activity high -goal gain
Получить подробный отчёт в формате JSON:

bash
node calorie_calc.js --gender male --age 30 --height 180 --weight 80 --activity sedentary --goal maintain --output report.json
📁 Структура репозитория
text
calorie-calculator/
├── README.md
├── calorie_calc.py      # Python (CLI + Tkinter GUI)
├── calorie_calc.js      # JavaScript (Node.js CLI)
├── calorie_calc.go      # Go (CLI)
├── calorie_calc.rs      # Rust (CLI)
├── CalorieCalculator.java  # Java (CLI + Swing GUI)
├── CalorieCalculator.cs    # C# (CLI + WinForms)
└── calorie_calc.php     # PHP (CLI + веб)
