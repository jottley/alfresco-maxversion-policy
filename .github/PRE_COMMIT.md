# Pre-commit Hooks

This project uses [pre-commit](https://pre-commit.com/) to automatically enforce code quality standards before commits.

## Installation

### First-time setup

1. Install pre-commit (if not already installed):

```bash
# macOS with Homebrew
brew install pre-commit

# Or with pip
pip install pre-commit

# Or with pipx (recommended)
pipx install pre-commit
```

2. Install the git hook scripts:

```bash
pre-commit install
```

This will install the pre-commit hook into `.git/hooks/pre-commit`.

## What the hooks do

The pre-commit configuration includes:

### General File Checks
- **Trailing whitespace removal** (except `.properties` files)
- **End-of-file fixer** - ensures files end with a newline
- **YAML validation** - checks docker-compose.yml and other YAML files
- **XML validation** - validates all XML files
- **Merge conflict detection**
- **Case conflict detection** - prevents issues on case-insensitive filesystems
- **Line ending normalization** - converts to LF
- **Private key detection** - prevents accidental commit of secrets

### Java-specific Checks
- **Java formatting** - automatically formats Java code using Google Java Format (AOSP style)
- **Unit tests** - runs `mvn test -pl max-version-policy-platform` on Java file changes
- **Wildcard import detection** - blocks commits with `import foo.*;` (project standards require explicit imports)
- **System.out detection** - blocks commits using `System.out.println` (should use logger)
- **Copyright header validation** - warns if copyright headers are missing

## Usage

### Automatic checking (recommended)

Once installed, hooks run automatically on `git commit`. If any hook fails:
- Files will be automatically fixed where possible (formatting, whitespace, etc.)
- You'll need to `git add` the fixed files and commit again
- For test failures or code quality issues, fix the problems and retry

### Manual checking

Run hooks on all files:
```bash
pre-commit run --all-files
```

Run hooks only on staged files:
```bash
pre-commit run
```

Run a specific hook:
```bash
pre-commit run trailing-whitespace
pre-commit run maven-test
```

### Skipping hooks (not recommended)

In rare cases where you need to bypass hooks:
```bash
git commit --no-verify
```

⚠️ **Warning**: Only use `--no-verify` when absolutely necessary, as it bypasses all quality checks.

## Updating hooks

Update to the latest versions:
```bash
pre-commit autoupdate
```

## Troubleshooting

### Hook installation failed
```bash
pre-commit clean
pre-commit install
```

### Formatting changes cause merge conflicts
Run pre-commit on the entire codebase after merging:
```bash
pre-commit run --all-files
git add -u
git commit -m "Apply code formatting"
```

### Tests fail in hook but pass manually
The hook runs `mvn test -pl max-version-policy-platform -q`. Run this exact command to reproduce:
```bash
mvn test -pl max-version-policy-platform -q
```

## Configuration

The configuration file is `.pre-commit-config.yaml` in the project root. Modify this file to:
- Add new hooks
- Change hook versions
- Adjust hook arguments
- Enable/disable specific checks

After modifying `.pre-commit-config.yaml`:
```bash
pre-commit install  # Reinstall if needed
pre-commit run --all-files  # Test the changes
```

## CI/CD Integration

Consider adding pre-commit to CI/CD:

```yaml
# Example GitHub Actions workflow
- uses: pre-commit/action@v3.0.0
```

This ensures all commits meet quality standards, even if developers bypass local hooks.
