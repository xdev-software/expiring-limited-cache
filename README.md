[![Latest version](https://img.shields.io/maven-central/v/software.xdev/expiring-limited-cache?logo=apache%20maven)](https://mvnrepository.com/artifact/software.xdev/expiring-limited-cache)
[![Build](https://img.shields.io/github/actions/workflow/status/xdev-software/expiring-limited-cache/check-build.yml?branch=develop)](https://github.com/xdev-software/expiring-limited-cache/actions/workflows/check-build.yml?query=branch%3Adevelop)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=xdev-software_expiring-limited-cache&metric=alert_status)](https://sonarcloud.io/dashboard?id=xdev-software_expiring-limited-cache)

# Expiring limited cache

A [expiring and limited java cache](./expiring-limited-cache/src/main/java/software/xdev/caching/ExpiringLimitedCache.java) <sup>[JD](https://javadoc.io/doc/software.xdev/expiring-limited-cache/latest/software/xdev/caching/ExpiringLimitedCache.html)</sup>

Provides a ``Map`` that clears entries after a specific time, when a specific size is reached or when the JVM needs memory.

Use-case examples:
* Caching all objects in an S3 Bucket
* Caching API responses

## Installation
[Installation guide for the latest release](https://github.com/xdev-software/expiring-limited-cache/releases/latest#Installation)

## Support
If you need support as soon as possible and you can't wait for any pull request, feel free to use [our support](https://xdev.software/en/services/support).

## Contributing
See the [contributing guide](./CONTRIBUTING.md) for detailed instructions on how to get started with our project.

## Dependencies and Licenses
View the [license of the current project](LICENSE) or the [summary including all dependencies](https://xdev-software.github.io/expiring-limited-cache/dependencies)
