# Lightweight Runtime Benchmarks

Date: February 26, 2026  
Branch: `experiment/lightweight-arm-cloud`  
Host: local Docker Desktop (x86_64)

## Scope

- `weather-app` (standard JVM image, `Dockerfile`, port `8088`)
- `weather-app-lite` (distroless + layered jar + constrained runtime flags, port `8090`)
- Native-image attempt (`build-native-image.sh`) status

## Method

- Startup benchmark command:
  - `./scripts/bench/compose-startup-benchmark.sh <service> <port> 2`
- Health target:
  - `GET /actuator/health` with `"status":"UP"`
- Memory sample:
  - `docker stats --no-stream` immediately after health becomes `UP`

## Results

| Variant | Image Size | Startup Run 1 | Startup Run 2 | Startup Avg | Memory Run 1 | Memory Run 2 | Memory Avg |
|---|---:|---:|---:|---:|---:|---:|---:|
| `weather-app` | 607 MB | 11.89 s | 11.81 s | 11.85 s | 570.4 MiB | 566.5 MiB | 568.5 MiB |
| `weather-app-lite` | 468 MB | 30.37 s | 24.45 s | 27.41 s | 368 MiB | 370 MiB | 369.0 MiB |

## Native Image Status

Attempted with:

- `./scripts/docker/build-native-image.sh weather-alert-backend-native:local`
- Spring Boot native profile + Paketo tiny builder

Observed blocker:

- Native-image build fails during analysis with class initialization errors (logback/slf4j classes initialized at build time).
- Netty BouncyCastle ALPN class loading warnings also appear during analysis.

This branch includes script wiring to continue iterating on native arguments, but native image is not yet producing a runnable container.

## Takeaways

- The lite JVM container is significantly smaller and uses less memory.
- The lite JVM container is slower to become healthy in this local setup.
- For immediate cloud cost reduction with low risk, `weather-app-lite` is ready to test on ARM64 nodes.
- Native image likely needs explicit AOT/native tuning for logging/netty stack before it can be production-ready.
