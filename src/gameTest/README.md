# GameTest regression infrastructure

Run from the repository root with Java 17 available to Gradle:

```powershell
.\gradlew.bat runGameTestServer
```

The `gameTest` source set is loaded only by the Forge GameTest server. Its fluids,
buckets, tags, and empty structure are excluded from the published mod JAR.
Regression tests use real Forge capabilities and the common QFF Mixins.
The development runtime also supplies Create and optional storage integrations;
the tests do not require an integration pack or its scripts. These development
dependencies remain optional for the shipped mod.
