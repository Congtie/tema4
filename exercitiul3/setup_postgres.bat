@echo off
echo Setting up PostgreSQL database for Exercise 3...
echo.

echo Step 1: Creating database 'testdb'
echo Please enter your PostgreSQL password when prompted.
echo.

echo Creating database...
psql -U postgres -c "CREATE DATABASE testdb;" 2>nul
if %errorlevel% neq 0 (
    echo Database might already exist or PostgreSQL is not running.
    echo Please ensure PostgreSQL is installed and running.
    echo.
)

echo.
echo Step 2: Testing connection to testdb...
psql -U postgres -d testdb -c "\dt" 2>nul
if %errorlevel% neq 0 (
    echo Could not connect to database. Please check:
    echo 1. PostgreSQL is installed and running
    echo 2. User 'postgres' exists
    echo 3. Password is correct
    echo.
    pause
    exit /b 1
)

echo.
echo Database setup complete!
echo.
echo Now you can run the Java application with:
echo javac -cp ".;postgresql-42.7.3.jar" *.java
echo java -cp ".;postgresql-42.7.3.jar" JDBCConcurrentApp
echo.
pause
