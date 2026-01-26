# Blogging Platform - JavaFX Frontend

This README explains how to run the JavaFX desktop frontend locally and fixes the "JavaFX runtime components are missing" error.

Two recommended ways to run:

1) Run using Maven (recommended)

- From the project root, build and run via the JavaFX Maven plugin. This ensures the JavaFX dependencies are placed on the module path correctly:

```powershell
mvn -DskipTests package
mvn javafx:run
```

2) Run from IntelliJ / `java` directly (VM options required)

If you run the `AppLauncher` main class directly from IntelliJ or by invoking `java` on the command line, you must provide JavaFX on the module path and enable the required modules.

Example PowerShell `java` command (adjust versions and paths if different):

```powershell
"C:\Program Files\Java\jdk-25\bin\java.exe" \
  --module-path "C:\Users\JacobQuaye\.m2\repository\org\openjfx\javafx-controls\21\javafx-controls-21-win.jar;C:\Users\JacobQuaye\.m2\repository\org\openjfx\javafx-fxml\21\javafx-fxml-21-win.jar;C:\Users\JacobQuaye\.m2\repository\org\openjfx\javafx-graphics\21\javafx-graphics-21-win.jar;C:\Users\JacobQuaye\.m2\repository\org\openjfx\javafx-base\21\javafx-base-21-win.jar" \
  --add-modules javafx.controls,javafx.fxml \
  -classpath "target\classes;C:\Users\JacobQuaye\.m2\repository\org\postgresql\postgresql\42.7.8\postgresql-42.7.8.jar;C:\Users\JacobQuaye\.m2\repository\com\zaxxer\HikariCP\5.0.1\HikariCP-5.0.1.jar;C:\Users\JacobQuaye\.m2\repository\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar;C:\Users\JacobQuaye\.m2\repository\org\slf4j\slf4j-api\2.0.9\slf4j-api-2.0.9.jar" \
  org.amalitech.AppLauncher
```

- If you run from IntelliJ, edit the Run Configuration for `AppLauncher` and add the VM options (example - put everything on one line):

```
--module-path "C:\Users\JacobQuaye\.m2\repository\org\openjfx\javafx-controls\21\javafx-controls-21-win.jar;C:\Users\JacobQuaye\.m2\repository\org\openjfx\javafx-fxml\21\javafx-fxml-21-win.jar;C:\Users\JacobQuaye\.m2\repository\org\openjfx\javafx-graphics\21\javafx-graphics-21-win.jar;C:\Users\JacobQuaye\.m2\repository\org\openjfx\javafx-base\21\javafx-base-21-win.jar" --add-modules javafx.controls,javafx.fxml
```

Notes & troubleshooting

- Ensure the JavaFX version in `pom.xml` (property `javafx.version`) matches the jars you reference. I set it to `21` in the POM.
- If you prefer using a JavaFX SDK distribution instead of Maven classifier jars, set `--module-path` to the SDK `lib` directory (e.g., `C:\javafx-sdk-21\lib`).
- If you see errors about missing native libraries, ensure the `-win` classifier jars are used on Windows (examples above use `-win` jars from your local Maven repo).
- If you get classpath/module errors, run `mvn javafx:run` from the project root — it handles module-path setup automatically.

Quick verification steps

1. Build project: `mvn -DskipTests package`
2. Seed an admin user (run from IDE or via `java -cp ...`):
   - Run `org.amalitech.util.CreateAdmin admin admin@example.com MySecret123`
3. Launch the app and login with the admin credentials.