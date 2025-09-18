# 2.2.2
* Re-Release due to Sonatype/Maven Central incorrectly claiming that the previous release is incorrectly signed #153

# 2.2.1
* Re-Release due to Sonatype/Maven Central incorrectly claiming that the previous release is incorrectly signed #153

# 2.2.0
* Added `computeIfAbsent`
* Improved logging performance
* Scheduler: Try to remove tasks immediately when cancelled to prevent unbound retention of cancelled tasks

# 2.1.0
* Java21+: Use Virtual Thread(s) for scheduling #146
* Make it possible to manually set the `ScheduledExecutorService` in `DefaultHolder`
* Renamed cleanup executor thread

# 2.0.0
* Redesign Scheduling: Use one central `ScheduledThreadPoolExecutor` per JVM #146
  * This should lower the overall amount of Threads and therefore improve performance
* Removed `name` parameter as it's no longer required
* Implement `AutoCloseable`
  * Calling this is optional
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
