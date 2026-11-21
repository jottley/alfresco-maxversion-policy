# Running Pre-commit Natively in GitHub Actions

## Overview

This project runs pre-commit checks directly in GitHub Actions using `pip install pre-commit` and `pre-commit run --all-files`, avoiding the `pre-commit/action` which has Node.js version dependencies.

## Why This Approach?

### Problem with `pre-commit/action`
```yaml
# ❌ Has Node.js version issues
- uses: pre-commit/action@v3.0.2  # Still uses Node 20 (deprecated)
```

The `pre-commit/action` is a JavaScript action that:
- Depends on Node.js runtime (currently stuck on Node 20)
- Triggers deprecation warnings as Node versions change
- Requires maintaining action version updates

### Solution: Native Installation
```yaml
# ✅ No Node.js dependency
- name: Install pre-commit
  run: pip install pre-commit

- name: Run pre-commit
  run: pre-commit run --all-files
```

This approach:
- ✅ No Node.js version issues
- ✅ Always uses latest pre-commit (Python-based)
- ✅ No action version to maintain
- ✅ Direct control over execution
- ✅ Works with all GitHub-hosted runner Node versions

## Current Workflow Configuration

### `.github/workflows/ci.yml`

```yaml
name: CI Quality Checks

on:
  push:
    branches: [ dev, master ]
  pull_request:
    branches: [ dev, master ]

jobs:
  pre-commit:
    name: Pre-commit Checks
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7

      - name: Set up Python
        uses: actions/setup-python@v6.3.0
        with:
          python-version: '3.x'

      - name: Set up Java
        uses: actions/setup-java@v5.4.0
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'maven'

      - name: Install pre-commit
        run: pip install pre-commit

      - name: Run pre-commit
        run: pre-commit run --all-files

  maven-test:
    name: Maven Tests
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7

      - name: Set up Java
        uses: actions/setup-java@v5.4.0
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'maven'

      - name: Run unit tests
        run: mvn test -pl max-version-policy-platform

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v7
        with:
          name: test-results
          path: '**/target/surefire-reports/*.xml'
```

## How It Works

### Pre-commit Job Flow

1. **Checkout code** (`actions/checkout@v7`)
2. **Set up Python** (`actions/setup-python@v6.3.0`)
3. **Set up Java 21** (`actions/setup-java@v5.4.0`, Temurin)
4. **Install pre-commit** (native Python command) - No Node dependency ✅
5. **Run pre-commit** (native command) - No Node dependency ✅

### Why We Need Java Setup

Pre-commit hooks in this project include:
- Java formatting (Google Java Format)
- Maven tests (when Java files change)

Both require Java to be available, hence the Java setup step.

## Benefits of This Approach

| Aspect | `pre-commit/action` | Native `pip install` |
|--------|---------------------|----------------------|
| **Node.js dependency** | ⚠️ Yes (Node 20) | ✅ No |
| **Deprecation warnings** | ⚠️ Frequent | ✅ None |
| **Maintenance** | ⚠️ Must update action versions | ✅ Auto-updates via pip |
| **Control** | ❌ Limited | ✅ Full control |
| **Speed** | ~Same | ~Same |
| **Caching** | ✅ Built-in | 🔧 Manual (optional) |

## Advanced: Adding Pre-commit Cache (Optional)

To speed up subsequent runs, add caching:

```yaml
- name: Set up Python
  uses: actions/setup-python@v6
  with:
    python-version: '3.x'
    cache: 'pip'  # Cache pip dependencies

- name: Cache pre-commit environments
  uses: actions/cache@v5
  with:
    path: ~/.cache/pre-commit
    key: pre-commit-${{ hashFiles('.pre-commit-config.yaml') }}
    restore-keys: |
      pre-commit-

- name: Install pre-commit
  run: pip install pre-commit

- name: Run pre-commit
  run: pre-commit run --all-files
```

**Benefits:**
- Pre-commit environments cached between runs
- ~30% faster on cache hit
- Cache invalidates when `.pre-commit-config.yaml` changes

**Trade-off:**
- Slightly more complex configuration
- Minimal time savings for our small project

**Recommendation:** Skip caching for now; add if build times become an issue.

## Comparison with Other Approaches

### Approach 1: pre-commit/action (Previous)

```yaml
- uses: pre-commit/action@v3.0.2
```

**Pros:** Simple, one-line
**Cons:** Node.js version dependency, deprecation warnings

### Approach 2: Native Installation (Current) ✅

```yaml
- run: pip install pre-commit
- run: pre-commit run --all-files
```

**Pros:** No Node.js dependency, always current, no action maintenance
**Cons:** Slightly more verbose (2 lines vs 1)

### Approach 3: pre-commit.ci (External Service)

```yaml
# No GitHub Actions needed - external service
```

**Pros:** Fastest, auto-fixing, auto-updates
**Cons:** External dependency, requires account setup, separate service

## Troubleshooting

### Pre-commit command not found

**Error:**
```
/bin/bash: pre-commit: command not found
```

**Fix:** Ensure `pip install pre-commit` runs before `pre-commit run`

### Java not found during pre-commit

**Error:**
```
Java formatting hook failed: java: command not found
```

**Fix:** Ensure `actions/setup-java` runs before pre-commit

### Hook installation slow

**Symptom:** First run takes ~30 seconds to download/install hooks

**Expected behavior:** Pre-commit downloads formatters on first use

**Optional fix:** Add caching (see "Advanced" section above)

### Maven tests fail in pre-commit but pass locally

**Cause:** Different Java version or Maven cache state

**Debug:**
```yaml
- name: Debug environment
  run: |
    java -version
    mvn -version
    echo $JAVA_HOME
```

## Maintenance

### Updating Pre-commit Hooks

The workflow always installs the latest `pre-commit` from PyPI automatically.

To update hook versions in `.pre-commit-config.yaml`:

```bash
# Local update
pre-commit autoupdate

# Commit changes
git add .pre-commit-config.yaml
git commit -m "chore: update pre-commit hooks"
```

### Updating GitHub Actions

Only these actions need maintenance:

- `actions/checkout` (currently v7)
- `actions/setup-python` (currently v6.3.0)
- `actions/setup-java` (currently v5.4.0)
- `actions/upload-artifact` (currently v7)

No `.github/dependabot.yml` exists yet; consider adding one to automate these updates.

## When to Use Each Approach

### Use Native Installation (Current) When:
- ✅ You want zero Node.js dependency issues
- ✅ You want full control over execution
- ✅ You don't need auto-fixing via PR commits
- ✅ GitHub Actions is your primary CI

### Use pre-commit/action When:
- Only if it's updated to Node 24+ and you prefer one-line syntax

### Use pre-commit.ci When:
- ✅ You want fastest possible feedback
- ✅ You want auto-fixing via PR commits
- ✅ You want automatic hook updates
- ✅ You're willing to use an external service

## Summary

**Current setup:**
- ✅ Pre-commit runs natively via `pip install` in GitHub Actions
- ✅ No Node.js version dependency
- ✅ Zero deprecation warnings
- ✅ Minimal maintenance required

**What you get:**
- Pre-commit and Maven tests run as parallel jobs
- No external service dependencies
- Full control within GitHub Actions
