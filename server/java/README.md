# Java Multithreaded Web Server

A simple HTTP/1.1 server with:
- Thread pool workers
- Router with sample routes
- LRU cache (with TTL) for GET responses
- Token-bucket rate limiter per client
- Minimal metrics

Build and Run (locally on your machine):
- Requires Java 17+
- Compile:
  - Linux/Mac:
    - mkdir -p out && javac -d out $(find src -name "*.java")
  - Windows (PowerShell):
    - mkdir out; Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName } | % { "javac -d out `"$($_)`"" } | cmd
- Run:
  - java -cp out com.example.webserver.Main 8080

Test:
- GET http://localhost:8080/
- GET http://localhost:8080/healthz
- GET http://localhost:8080/time
- POST http://localhost:8080/echo (with a body)

Notes:
- Connection is closed per request (no keep-alive) for simplicity.
- Cache key is method + path. Adjust as needed for query strings/headers.
