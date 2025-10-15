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

- Clean build and package:
  
  ./mvnw -q clean package

Artifacts are created in `target/`:
- `folderCompare-1.0-SNAPSHOT.jar` (original)
- `folderCompare-1.0-SNAPSHOT-shaded.jar` (fat/executable JAR)

The shaded JAR contains runtime dependencies (including JavaFX controls) and excludes test-only libraries.


## Run
Run the shaded JAR:

  java -jar target/folderCompare-1.0-SNAPSHOT-shaded.jar

Main class: `net.parksy.foldercompare.App`.

Usage:
- In the UI, type two folder paths (e.g., `C:\Users\you\Documents` and `D:\Backup\Documents`, or `/Users/you/Documents` and `/Volumes/Backup/Documents`).
- Press Enter in either text field to refresh the comparison.
- Columns can be resized; rows are aligned across both tables for easy scanning.

Troubleshooting (JavaFX):
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