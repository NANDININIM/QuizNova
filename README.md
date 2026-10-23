# QuizNova – DBMS Mini Project

QuizNova is a Java Swing + JDBC + MySQL online quiz management application.

## Features
- Student registration and login
- Admin login
- Create quizzes
- Add and delete quiz questions
- Students attempt MCQ quizzes
- Automatic score calculation and result storage
- Student result history and admin result listing
- Relational schema with primary keys, foreign keys and SQL joins

## Requirements
- JDK 17 or later
- NetBeans IDE (Maven support)
- MySQL Server 8.x
- MySQL Workbench (recommended)

## Database setup
1. Open MySQL Workbench and connect to your local MySQL server.
2. Open `database/quiznova.sql`.
3. Execute the complete script. It creates the `quiznova` database, tables, a demo admin, a sample quiz and sample questions.

Demo admin credentials:
- Username: `admin`
- Password: `admin123`

## Configure database connection
Open `src/main/java/com/quiznova/DBConnection.java`.
Change `PASSWORD` to your local MySQL root password. If your MySQL username is not `root`, change `USER` too.

## Run in NetBeans
1. Extract the ZIP file.
2. In NetBeans choose **File → Open Project**.
3. Select the extracted `QuizNova` folder (the one containing `pom.xml`).
4. Wait for Maven to download MySQL Connector/J.
5. Open `DBConnection.java` and set your MySQL password.
6. Run the project. Main class: `com.quiznova.QuizNova`.

## GitHub upload
1. Create a new empty GitHub repository named `QuizNova`.
2. Extract this ZIP.
3. Upload the contents of the `QuizNova` folder to the repository (not the ZIP itself).
4. Commit the files. Keep `README.md`, `pom.xml`, `database/`, and `src/`.

## Important notes
- This is an educational college project starter. Passwords are stored as plain text for simplicity; do not use this approach for a real production system. A real deployment should hash passwords and use HTTPS.
- Do not upload your actual MySQL password to a public GitHub repository. For a public submission, replace it with a placeholder before uploading.
