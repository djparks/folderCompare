Project guidelines for folderCompare

Overview
- Language/Build: Java 17 with Maven. Wrapper scripts (mvnw/mvnw.cmd) are included.
- Packaging: Shaded, executable JAR via maven-shade-plugin with Main-Class net.parksy.foldercompare.App.
- Testing: JUnit 5 (junit-jupiter) with optional parameterized tests (junit-jupiter-params).

Build and configuration
- Prerequisites
  - JDK 17 installed and available on PATH (maven.compiler.release=17).
  - No system Maven required; prefer the provided wrapper: ./mvnw (Linux/macOS) or mvnw.cmd (Windows).
  - DO NOT add Spring or Spring Boot dependencies to the project.
  - Use openjfx (JavaFX) as the UI framework.

- Clean build
  - ./mvnw -q clean package
  - Produces artifacts in target/:
    - folderCompare-1.0-SNAPSHOT.jar (original)
    - folderCompare-1.0-SNAPSHOT-shaded.jar (fat/executable JAR)
  - The shade plugin excludes test-only dependencies from the shaded JAR (see exclusions for junit-jupiter* in pom.xml).

- Run the application (shaded JAR)
  - java -jar target/folderCompare-1.0-SNAPSHOT-shaded.jar
  - Main class is net.parksy.foldercompare.foldercompare.App and currently prints "Hello World!".

Testing
- Framework and layout
  - Tests use JUnit 5 (Jupiter). The project imports the junit-bom for version alignment and includes:
    - org.junit.jupiter:junit-jupiter-api (test scope)
    - org.junit.jupiter:junit-jupiter-params (test scope) for parameterized tests
  - Test sources live under src/test/java.

- Run tests
  - All tests: ./mvnw -q test
  - A single test class: ./mvnw -q -Dtest=AppTest test
  - A single test method: ./mvnw -q -Dtest=AppTest#shouldAnswerWithTrue test
  - Failsafe is not configured; only unit tests (Surefire) are intended.

- Add a new test
  - Place tests under src/test/java, use package naming to mirror the code under src/main/java when possible.
    - Note: The current sample test class uses package net.parksy, while the main code uses net.parksy.foldercompare.
      For new tests, prefer mirroring the main package (e.g., net.parksy.foldercompare) to leverage IDE navigation and default conventions.
  - Example: parameterized test (uses junit-jupiter-params which is already declared)
    
    // File: src/test/java/net/parksy/foldercompare/ParamDemoTest.java
    package net.parksy.foldercompare;

    import static org.junit.jupiter.api.Assertions.assertEquals;
    import org.junit.jupiter.params.ParameterizedTest;
    import org.junit.jupiter.params.provider.CsvSource;

    class ParamDemoTest {
        @ParameterizedTest
        @CsvSource({
            "1, 2, 3",
            "2, 3, 5",
            "5, 8, 13"
        })
        void addsNumbers(int a, int b, int expected) {
            assertEquals(expected, a + b);
        }
    }

  - Then run it with ./mvnw -q -Dtest=ParamDemoTest test or run all tests.

- Notes/tips
  - Use org.junit.jupiter.api.Assertions for assertions and org.junit.jupiter.api.Test for tests.
  - For temporary debug output in tests, prefer descriptive assertion messages or System.out.println for quick checks; remove or gate such output before committing.

Additional development information
- Project structure and main entry point
  - App main class: src/main/java/net/parksy/foldercompare/App.java.
  - If the main class changes, update it in two places:
    - pom.xml: maven-jar-plugin <mainClass>
    - pom.xml: maven-shade-plugin ManifestResourceTransformer <mainClass>

- Dependency management
  - junit-bom is managed via <dependencyManagement>, allowing omitting explicit versions for junit-jupiter modules.
  - Keep test libraries in <scope>test</scope> so they are not included in the runtime classpath nor in the shaded JAR (current shade config also excludes them explicitly).

- Packaging and runtime
  - The shaded JAR is intended for distribution. If you add runtime libraries (e.g., logging), ensure they are compatible with shading and relocate packages if necessary to avoid conflicts.
  - CreateDependencyReducedPom is enabled to avoid leaking shaded dependencies into the published POM.

- Code style and conventions
  - Use standard Java naming and package conventions. Mirror test package structure to the main code where possible.
  - Keep main method lean; extract logic into testable classes/methods to allow unit testing without capturing System.out.
  - Avoid introducing logging frameworks unless needed. If logging is required, consider adding SLF4J with a simple backend (test scope can use slf4j-simple), and exclude it from shading or relocate as appropriate.

- IDE and tooling
  - The project is IDE-agnostic. Import as a Maven project. Ensure the JDK for the project is set to 17.
  - Running and debugging tests from the IDE uses the JUnit Platform by default with Maven projects.

- Misc
  - If you refactor package names, update any references in pom.xml (mainClass) and verify the shade plugin still produces a runnable JAR.
  - Prefer small, focused tests. Parameterized tests are available for table-driven scenarios.

Verification performed during this change
- Executed the existing test (net.parksy.AppTest) successfully.
- Added a temporary parameterized test to confirm junit-jupiter-params wiring, executed it successfully, and then removed it to keep the repository clean.
