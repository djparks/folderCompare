# folderCompare

A lightweight desktop utility to visually compare the immediate contents of two folders.

The app is built with Java 17 and JavaFX. It shows two side‑by‑side tables (Folder 1 and Folder 2). Enter absolute paths to the folders and press Enter in either field to refresh. For each file or subfolder name that appears in either location, the tables show:

- Name (with a ➜ prefix where the entry exists only on one side)
- Size (for files; directories show “—”)
- Last modified timestamp

This makes it easy to spot items missing on one side or differences in size/timestamps at a glance.


## Requirements
- JDK 17 installed and available on PATH
- No system Maven required; project includes Maven Wrapper
- A desktop environment capable of launching JavaFX windows (Windows, macOS, or Linux with GUI)

Note: Do not add Spring/Spring Boot; this project uses plain Java + JavaFX.


## Build
Use the Maven Wrapper (recommended):

- Clean build:
  
  ./mvnw -q clean package

Outputs:
- Plain app JAR: target/folderCompare.jar (for use with your own JavaFX SDK/module path)
- Platform runtime image (recommended): build it with the JavaFX plugin:
  
  ./mvnw -q clean package javafx:jlink

This produces a self-contained runtime image under target/ with a bin/folderCompare launcher that includes the JRE and JavaFX modules.


## Run
Recommended (development):

  ./mvnw -q javafx:run

Recommended (distribution): build and use the jlink runtime image:

  ./mvnw -q clean package javafx:jlink
  # Then run the generated launcher (path varies by OS):
  target/**/bin/folderCompare

Alternatively, run the plain JAR with your local JavaFX SDK on the module path:

  java \
    --module-path /path/to/javafx-sdk-21.0.5/lib \
    --add-modules javafx.controls \
    -jar target/folderCompare.jar

Notes:
- Replace /path/to/javafx-sdk-21.0.5/lib with the path to the JavaFX SDK lib directory for your OS/arch.
- If you add FXML, include it in --add-modules: javafx.controls,javafx.fxml.

Usage:
- In the UI, type two folder paths (e.g., `C:\Users\you\Documents` and `D:\Backup\Documents`, or `/Users/you/Documents` and `/Volumes/Backup/Documents`).
- Press Enter in either text field to refresh the comparison.
- Columns can be resized; rows are aligned across both tables for easy scanning.

Troubleshooting (JavaFX):
- If you see “NoClassDefFoundError: javafx/application/Application”, you are running the plain JAR without JavaFX on the module path. Use javafx:run, the jlink launcher, or provide --module-path as shown above.
- On Linux, ensure you’re running in a graphical session and have appropriate graphics drivers. If running over SSH, enable X forwarding or use a local desktop session.


## Tests
Tests use JUnit 5 (Jupiter).

- Run all tests:
  
  ./mvnw -q test

- Run a single test class:
  
  ./mvnw -q -Dtest=AppTest test

- Run a single test method:
  
  ./mvnw -q -Dtest=AppTest#shouldAnswerWithTrue test

Test sources live under `src/test/java`. Parameterized tests are available via `junit-jupiter-params` and can be used as needed.


## Project Layout
- Main entry point: `src/main/java/net/parksy/foldercompare/App.java`
- Build config: `pom.xml`
- Tests: `src/test/java`

If you ever change the main class, update it in `pom.xml` under both `maven-jar-plugin` and the `maven-shade-plugin` `ManifestResourceTransformer`.


## Development Notes
- Java version is pinned via `maven.compiler.release=17`.
- The shaded JAR is intended for distribution; dependency-reduced POM generation is enabled.
- Keep the main method lean; extract logic into testable components when expanding features.
- Avoid introducing logging unless needed; if you add it, be mindful of shading and runtime footprints.


## License
Provide your preferred license here (e.g., MIT).