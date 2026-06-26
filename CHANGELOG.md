# Changelog

All notable changes to the Alfresco Max Version Policy project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Pre-commit hooks for code quality enforcement
  - Google Java Format (AOSP style) auto-formatting
  - Unit tests run automatically on Java file changes
  - Wildcard import detection
  - System.out.println detection
  - Trailing whitespace and line ending fixes
  - YAML/XML validation
  - Copyright header validation
- GitHub Actions CI/CD workflow for quality checks
- EditorConfig for consistent IDE formatting
- Comprehensive pre-commit documentation in `.github/PRE_COMMIT.md`
- Comparison guide: Pre-commit vs Maven plugins (`.github/PRE_COMMIT_VS_MAVEN.md`)

### Changed
- Updated `.gitignore` to exclude OS-specific files (.DS_Store, Thumbs.db)
- Updated `.gitignore` to exclude Maven debug files (effective-pom.xml)
- Enhanced README.md with pre-commit setup instructions
- Enhanced CLAUDE.md with code quality standards and pre-commit usage

### Removed
- Unused `.github/modernize/java-upgrade/hooks` directory

## [0.0.10] - 2026-06-10

### Changed
- Updated version to 0.0.10
- Moved to Apache 2.0 license
- Updated `.gitignore`
- Moved Alfresco repo version to 5.2.g for build
- Removed `module.repo.version.max` (newer versions work without changes)
- Updated README to reflect changes

## [0.0.9] - Date Unknown

### Changed
- Updated to support 5.1.g as max version
- Updated version to 0.0.9

## [0.0.8] - Date Unknown

### Fixed
- Fixed link to AMP in documentation

## Earlier Versions

See git history for changes in versions prior to 0.0.9.

---

**Contributors:**
- Jared Ottley (jared.ottley@hyland.com) - Original author and maintainer
- Konst Sergeev ([@ksergeev](https://github.com/ksergeev)) - Enhanced version cleanup for legacy nodes, added ability to disable policy
