# Alfresco Max Version Policy

**Author:** Jared Ottley (jared@ottleys.net)
**Original Date:** February 20, 2018
**Current Version:** 0.0.10
**Last Updated:** June 10, 2026

## Summary

Alfresco Max Version Policy is an Alfresco Content Services (ACS) extension that automatically limits the number of versions retained for versioned documents. When a new version is created and the maximum is reached, the oldest version is automatically deleted.

**Key Features:**
- Automatically removes oldest versions when the configured limit is exceeded
- Default limit: 10 versions per document
- Configurable via `maxVersions` property in `alfresco-global.properties`
- Can be disabled by setting `maxVersions=0`
- Handles legacy nodes with long version histories efficiently
- Executes at transaction commit frequency for consistency

## Installation

Alfresco Max Version Policy is delivered as an AMP (Alfresco Module Package).

**Download:** [max-version-policy-0.0.10.amp](https://github.com/jottley/alfresco-maxversion-policy/releases/download/0.0.10/max-version-policy-0.0.10.amp)

**Installation Steps:**
1. Download the AMP file
2. Copy the AMP into the `amps` directory of your Alfresco installation
3. Ensure Alfresco is not running
4. Run the `apply_amps.sh` (Linux/Mac) or `apply_amps.bat` (Windows) script
5. Start Alfresco

**Requirements:**
- **Minimum Alfresco Version:** 23.1.0
- **Java:** 11 or 17

## Configuration

### Basic Configuration

The maximum number of versions is configured in `alfresco-global.properties`:

```properties
maxVersions=10
```

- **Default value:** 10
- **To disable the policy:** Set `maxVersions=0`
- **After changing:** Restart Alfresco

### Logging Configuration

Logging is configured in `log4j2.properties`:

```properties
logger.alfresco-extension-maxversionpolicy.name=org.alfresco.extension.versioning
logger.alfresco-extension-maxversionpolicy.level=INFO
```

**Log Levels:**
- **INFO** (default): Shows initialization, version counts, and deletion actions
- **DEBUG**: Adds detailed troubleshooting information

**Example Logs:**
```
INFO [extension.versioning.MaxVersionPolicy] MaxVersions is set to: 10
INFO [extension.versioning.MaxVersionPolicy] Current number of versions: 11
INFO [extension.versioning.MaxVersionPolicy] Max Version Policy - Removing Version: 1.0
```

## Contributors

**Konst Sergeev** ([@ksergeev](https://github.com/ksergeev))
- Added ability to disable policy by setting maxVersions to zero
- Enhanced to clean long history of legacy nodes completely

## Development

This project uses Docker Compose to run a complete Alfresco development environment.

### Pre-commit Hooks

This project uses [pre-commit](https://pre-commit.com/) to enforce code quality standards. Install once:

```bash
pip install pre-commit  # or: brew install pre-commit
pre-commit install
```

Hooks will run automatically on commit and check:
- Java code formatting (Google Java Format)
- Unit tests on changed files
- Wildcard imports (blocked per project standards)
- Trailing whitespace, line endings, file formatting
- Copyright headers

See [.github/PRE_COMMIT.md](.github/PRE_COMMIT.md) for full documentation.

### Quick Start

Run with `./run.sh build_start` (Linux/Mac) or `./run.bat build_start` (Windows) to start:
- Alfresco Content Services (ACS) on port 8080
- Alfresco Share on port 8180
- Alfresco Search Services (ASS) on port 8983
- PostgreSQL database on port 5555
- ActiveMQ message broker

### Available Commands

All services run as Docker containers. The run script provides these tasks:

- **`build_start`** - Build the whole project, recreate Docker images, start all services, and tail logs
- **`build_start_it_supported`** - Same as `build_start` but includes dependencies for integration test execution
- **`start`** - Start the environment without building (uses existing images)
- **`stop`** - Stop all services
- **`purge`** - Stop services and delete all persistent data (Docker volumes)
- **`tail`** - Tail logs of all containers
- **`reload_share`** - Hot reload: rebuild Share module and restart Share container only
- **`reload_acs`** - Hot reload: rebuild ACS module and restart ACS container only
- **`build_test`** - Full test cycle: build, start, run integration tests, then stop
- **`test`** - Run integration tests against an already-running environment

### Testing

**Unit Tests:**
```bash
mvn test -pl max-version-policy-platform
```

**Integration Tests:**
```bash
# Start environment with test support
./run.sh build_start_it_supported

# Run integration tests
./run.sh test

# Or full test cycle
./run.sh build_test
```

**Test Coverage:**
- Unit tests: 10 tests covering policy logic with Mockito mocks
- Integration tests: 3 tests covering end-to-end behavior
  - `MaxVersionPolicyIT`: Spring bean wiring verification
  - `RestApiVersioningIT`: Version creation and policy enforcement via REST API

## Project Structure

This is an All-In-One (AIO) Maven multi-module project with the following modules:

- **max-version-policy-platform** - Core repository extension with MaxVersionPolicy implementation
- **max-version-policy-share** - Alfresco Share UI. This project provides no Share customizations
- **max-version-policy-platform-docker** - Docker image for ACS with the extension
- **max-version-policy-share-docker** - Docker image for Share
- **max-version-policy-integration-tests** - Integration tests against the dockerized environment

### Technical Notes

- **No parent POM** - Self-contained Maven structure
- **Docker-based** - Development environment managed via [Docker](https://www.docker.com/)
- **IDE Support** - Works seamlessly with Eclipse and IntelliJ IDEA
- **JRebel** - Hot reloading support with JRebel maven plugin
- **AMP Distribution** - Packaged as an Alfresco Module Package (AMP) assembly; No WAR files; AMPs are embedded in Docker images
- **Persistent Data** - Docker volumes for ACS, ASS, and database data
- **Integration Testing** - Tests run against the complete dockerized environment
- **META-INF Resources** - Resources loaded from META-INF directories

## Architecture

**Core Implementation:** [`MaxVersionPolicy.java`](max-version-policy-platform/src/main/java/org/alfresco/extension/versioning/MaxVersionPolicy.java)
- Implements `AfterCreateVersionPolicy` (triggers after version creation)
- Bound to transaction commit frequency via Spring
- Removes oldest versions in a loop when limit exceeded
- Handles legacy nodes with long version histories efficiently

**How It Works:**
1. A document version is created
2. At transaction commit, the policy is triggered
3. Policy checks if version count > maxVersions
4. If exceeded, oldest version(s) are deleted until count ≤ maxVersions
5. Logs all actions at INFO level

## License

Licensed under the Apache License, Version 2.0

Copyright 2018-2026 Jared Ottley
