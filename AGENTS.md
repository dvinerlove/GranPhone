# Agent notes

## Toolchain

- Gradle 6.7.1 (wrapper), AGP 4.2.2.
- Builds require **JDK 11** — set `JAVA_HOME="C:/Users/adrew/jdk11/jdk-11.0.33+10"` before every `./gradlew` call. Newer JDKs (e.g. 25) break this Gradle/AGP combination.
- Variants: `./gradlew assembleFDroidDebug` / `installFDroidDebug`.
