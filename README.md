# Trinity View Hostel system, group G09
Stage 3: Staff module (Douth Nhial) with 32 automated tests. Main menu options 1, 2, 3 and 7 work.

Compile (JDK 17 or newer): javac -d out $(find src tests -name "*.java")
Run: java -cp out ug.ac.vu.g09.app.MainMenu
Tests: java -cp out StaffModuleTests
