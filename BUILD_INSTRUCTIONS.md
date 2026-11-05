# Дастури build

## GitHub Actions
1. Мазмуни ин ZIP-ро (ҳамаи файлҳо дар решаи repo) ба GitHub push кунед.
2. Actions → «Build APK» худкор оғоз мешавад (ё Run workflow).
3. Artifact `LIST-OFFICE-ULTRA-APK` → файли `LIST-OFFICE-ULTRA.apk`.
4. Release APK танҳо вақте сохта мешавад, ки secrets-ҳои `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` гузошта шуда бошанд.
5. Ҳисоботҳо: artifact `build-reports`.

Workflow `gradle` -ро аз `gradle/actions/setup-gradle` (8.7) истифода мебарад, аз ин рӯ `gradlew`/wrapper лозим нест.

## Android Studio
Open → ин папка. Android Studio Gradle-ро худаш пешниҳод мекунад (8.7). JDK 17. AGP 8.5.2, Kotlin 1.9.24, Compose compiler 1.5.14.

## Шрифтҳо
`app/src/main/assets/fonts/README.txt`-ро бинед.
