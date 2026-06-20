--

## Sleep Tracker API

This is a Spring Boot and Kotlin-based Restful API designed to help users log, track, and analyze their sleep patterns over time.

## 🛠️ API Endpoints & Specifications

The API endpoints reside in `SleepLogController`. Spring-doc is utilized to expose the API Specifications dynamically, which can be located at http://localhost:8080/swagger-ui/index.html#/ upon local runtime.

The core application provides capability to:
* Create daily sleep records.
* Retrieve moving average trends for the past 30 days.
* Fetch information regarding last night's specific entry.
* Persist all telemetry data securely inside an optimized PostgreSQL table structure.

### Practical Integrations & Tooling
* **Identity Context:** In a real-world production scenario, the `userID` represents an immutable unique user identifier that would be securely provided and managed by an upstream federated cloud application or Identity Provider (IdP).
* **Postman Integration:** The interactive Postman collection is saved directly in this project as `SleepAPI.postman_collection.json` inside the `resources` folder. In a live team framework, this collection would actively utilize decoupled variables and secure environment scopes (restricted to lower-level configurations) to facilitate cross-team collaboration.
* **Script:** Running `test-api.sh` in the top level directory will test the API ./resources/test-api.sh (The API is required to be running prior to the script)
* Variables were used for parameters in the collection
* **Git & Version Control Conventions:** In a standardized corporate environment, every commit and branching path would mirror a project-specific convention tied directly to a target JIRA ticket tracking identifier. As explicit tickets were omitted for this assignment context, a concise structural description of each standalone requirement was substituted to ensure clear history tracking.

## Testing Suite and Code Coverage

* **Service Layer Unit Tests (`SleepLogServiceTest`):** Isolates business domain logic, ensuring mock calculations handle midnight crossovers cleanly and verify fallback behavior for fresh user profiles without tracking history.
* **Controller Layer Boundary Tests (`SleepLogControllerTest` / `GlobalExceptionHandlerTest`):** Leverages `MockMvc` to verify network serialization boundaries. Includes boundary type validation testing to guarantee malformed parameters, collections arrays, or corrupted payloads are blocked and returned early with a structured `400 Bad Request`.
* In a CI/CD process there would be a check for code coverage and code would have been prevented from being merged in.

## PR and Branching
* There would be comments in a branch and approvals
* A ticketing system would formalize a naming convention. Descriptions were utilized as the tickets instead
* A project specific board was not utilized in this excercise
* A release process will need to identify ticket numbers in a real world scenario


# Backend Engineering Interview: Take-home Assignment

Hello!

This is the repository that contains everything you need to complete the take home assignment, part of the backend interview process for Noom.

## The Assignment

You need to develop an API for sleep logger that will later be integrated into the Noom web interface. The functional requirements API needs to support are:

 1. Create the sleep log for the last night
    1. Sleep data contains
        1. The date of the sleep (today)
        1. The time in bed interval
        1. Total time in bed
        1. How the user felt in the morning: one of [BAD, OK, GOOD]

 1. Fetch information about the last night's sleep
 1. Get the last 30-day averages
    1. Data that needs to be included in the response
        1. The range for which averages are shown
        1. Average total time in bed
        1. The average time the user gets to bed and gets out of bed
        1. Frequencies of how the user felt in the morning
    1. The user can switch back to the single sleep log view (goes to requirement #1)

The assignment is to:

 1. Create tables required to support the functionality above (PostgreSQL)
    1. The Spring project includes Flyway, which you should use to manage your DB migrations.
 1. Build required functionality in the REST API service (Kotlin/Java + Spring)
    1. Ignore authentication and authorization, but make the REST API aware of the concept of a user.
 1. Write unit tests for the repository and any business logic.
 1. Write a simple script or create Postman collection that can be used to test the API

## Instructions

 1. Create a git repository from the files provided here.
 1. All code changes should be merged in as PRs to the repository. Separate your commits into meaningful pieces, and don't commit artifacts like build files etc.
 1. Write code and PR descriptions as if you were writing production-level code.
 1. The template in this repository provides a basic environment. Everything needed to start and test your code is available and functional. We expect you to use PostgreSQL and Java/Kotlin + Spring. On top of that, if you want to add new frameworks, you are free to do so.
 1. Keep in mind the goal of the interview is to assess your software development and coding skills. There's no need to spend time tweaking the configuration of the server, the DB, or the build system. The defaults in use here are good enough for this exercise.
 1. Once complete, zip the repository and upload it to the take-home link provided to you by Noom's talent team.

## How to Run

Dockerfiles are set up for your convenience for running the whole project. You will need docker and ports 5432 (Postgres) and 8080 (API).

To run everything, simply execute `docker-compose up`. To build and run, execute `docker-compose up --build`.
