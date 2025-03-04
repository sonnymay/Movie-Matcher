# Movie Matcher: VS Code Setup Guide

This document provides step-by-step instructions for setting up and running the Movie Matcher application using Visual Studio Code.

## Prerequisites

- Java Development Kit (JDK) 17 or later
- Visual Studio Code
- VS Code Extensions:
  - Extension Pack for Java
  - Spring Boot Extension Pack
  - Thymeleaf Support
- Maven (included with VS Code Java extensions)

## Step 1: Clone or Create the Project

If you're starting fresh:

1. Open VS Code
2. Go to View → Command Palette (Ctrl+Shift+P or Cmd+Shift+P)
3. Type "Spring Initializr" and select "Spring Initializr: Create a Maven Project"
4. Choose Spring Boot version (3.3.0)
5. For project metadata, enter:
   - Group ID: `com.yourname.moviematcher`
   - Artifact ID: `moviematcher`
   - Java version: `17`
6. Select dependencies:
   - Spring Web
   - Thymeleaf
   - Spring Cache Abstraction
   - Spring Boot DevTools

If you're using the existing project:

1. Open the folder `/Users/santipapmay/Desktop/Movie Matcher` in VS Code
2. Wait for VS Code to initialize the project and download dependencies

## Step 2: Configure TMDB API Key

The application is already configured with your API key in `application.properties`. For better security, consider setting it as an environment variable:

1. Open the Run/Debug configurations in VS Code
2. Add an environment variable:
   - Name: `TMDB_API_KEY`
   - Value: `6f756c7ebd790cd7fc5ce9cdb0869d7a`

## Step 3: Run the Application

Method 1: Using VS Code Run Button

1. Open `MoviematcherApplication.java`
2. Click the Run icon (▶️) above the `main` method or use the Run menu at the top
3. Wait for the application to start (check the console for "Started MoviematcherApplication")

Method 2: Using Terminal

1. Open a terminal in VS Code (Terminal → New Terminal)
2. Run the following command:

```bash
./mvnw spring-boot:run
```

## Step 4: Access the Application

Open your web browser and navigate to [http://localhost:8080](http://localhost:8080)

## Step 5: Development Tips

### Live Reload

With Spring Boot DevTools enabled, the application will automatically reload when you make changes to the code. Just save your files, and the application will restart.

### Debugging

1. Set breakpoints by clicking in the gutter (left margin of the editor)
2. Start the application in debug mode by clicking on the Debug icon or selecting "Debug" from the Run menu
3. Use the Debug console to inspect variables and step through code

### Testing API Endpoints

You can use VS Code's Thunder Client extension or REST Client extension to test API endpoints directly from the editor.

## Troubleshooting

### Application Fails to Start

Check the console for specific error messages. Common issues include:

- Port 8080 already in use: Change the port in `application.properties` (e.g., `server.port=8081`)
- API key issues: Verify your TMDB API key is correct
- Java version issues: Make sure you're using JDK 17 or later

### Dependencies Not Found

Run the following command to force Maven to update dependencies:

```bash
./mvnw clean install -U
```

### Debugging TMDB API Issues

If you're having trouble with the TMDB API:

1. Check your API key is correct
2. Test a simple API call directly in your browser:
   [https://api.themoviedb.org/3/movie/550?api_key=YOUR_API_KEY](https://api.themoviedb.org/3/movie/550?api_key=YOUR_API_KEY)
3. Enable more detailed logging by adding the following to `application.properties`:
   ```
   logging.level.org.springframework.web.client.RestTemplate=DEBUG
   ```
