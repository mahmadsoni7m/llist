# Гузориши санҷиш

| Санҷиш | Натиҷа |
|---|---|
| Gradle build / компилятсияи Kotlin | **ИҶРО НАШУД** — дар муҳити таҳия Gradle, Kotlin ва интернет набуд |
| Unit-тестҳо (TextToolsTest, ReportGeneratorTest, DocxExporterTest) | **навишта шудаанд, иҷро нашудаанд** |
| Баландии қавсҳои `{}()[]` дар ҳамаи .kt | гузашт (скрипт) |
| YAML-и workflow | гузашт (PyYAML) |
| XML-и manifest/resources | гузашт (well-formed) |
| PDF export, чоп, handwriting дар дастгоҳ | **санҷида нашудааст** |

Санҷиши воқеӣ: push ба GitHub → Actions (unit tests + lint + assembleDebug), баъд насб дар телефон.
Lint `abortOnError=false` аст, то ҳисобот сохта шавад; хатоҳои lint дар build-reports намоён мешаванд.
