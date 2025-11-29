# Environment Configuration Sample

This sample demonstrates how to configure a Temporal client using TOML configuration files. This allows you to manage connection settings across different environments without hardcoding them.

The `config.toml` file defines three profiles:
- `[profile.default]`: Local development configuration
- `[profile.staging]`: Configuration with incorrect address to demonstrate overrides
- `[profile.prod]`: Example production configuration (not runnable)

**Load from file (default profile):**
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.envconfig.LoadFromFile
```

**Load specific profile with overrides:**
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.envconfig.LoadProfile
```