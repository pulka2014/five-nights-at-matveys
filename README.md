# Five Nights at Matvey's

FNAF-style игра на Kotlin (JVM). Разработка — CachyOS, Windows-сборка через jpackage.

## Скачать и запустить на Windows (без установки Java)

1. Скачай архив из раздела Releases.
2. Распакуй в любую папку.
3. Запусти FiveNightsAtMatveys.exe.
4. Управление: TAB или C — камеры; 1..5 — переключение камер; A/D или стрелки влево/вправо — двери; R — рестарт ночи; Esc — меню.

## Собрать EXE самому (на Windows)

Нужен JDK 17+ с сайта adoptium.net. Из корня репозитория выполни:

    gradle clean fatJar
    & "C:\path\to\jdk-17\bin\jpackage.exe" --type exe --name "FiveNightsAtMatveys" --input "build\libs" --main-jar "game-all.jar" --main-class "desktop.MainKt" --app-version "1.0.0" --win-shortcut --win-menu --win-dir-chooser --dest "out"

На выходе: out\FiveNightsAtMatveys-1.0.0.exe

Портативная версия (без установщика):

    & "C:\path\to\jdk-17\bin\jpackage.exe" --type app-image --name "FiveNightsAtMatveys" --input "build\libs" --main-jar "game-all.jar" --main-class "desktop.MainKt" --dest "out"

На выходе папка out\FiveNightsAtMatveys\ — внутри FiveNightsAtMatveys.exe и встроенная JRE. Можно закинуть в zip и передать кому угодно.

## Разработка

    gradle run

Требуется: JDK 17+, gradle, ffmpeg.
