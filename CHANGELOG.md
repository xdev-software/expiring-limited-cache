# 2.0.0
* Redesign Scheduling: Use one central `ScheduledThreadPoolExecutor` per JVM #146
  * This should lower the overall amount of Threads and therefore improve performance
* Removed `name` parameter as it's no longer required
* Use `Instant` instead of `LocalDateTime`

# 1.0.4
* Migrated deployment to _Sonatype Maven Central Portal_ [#155](https://github.com/xdev-software/standard-maven-template/issues/155)
* Updated dependencies

# 1.0.3
* Improve performance when using virtual threads #49
* Updated dependencies

# 1.0.2
* Fixed possible NPE when shutting down

# 1.0.1
* Restore original logging with SLF4j

# 1.0.0
_Initial release_
