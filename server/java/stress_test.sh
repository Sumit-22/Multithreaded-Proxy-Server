#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$repo_root/server/java"

pkill -f 'com.example.proxy.ProxyServer' || true
pkill -f 'http.server 8000' || true

mkdir -p out
find src -name "*.java" > /tmp/proxy_java_files.txt
javac -d out @/tmp/proxy_java_files.txt

python3 -m http.server 8000 --directory "$repo_root" >/tmp/proxy_origin.log 2>&1 &
origin_pid=$!
trap 'kill "$origin_pid" >/dev/null 2>&1 || true; pkill -f "com.example.proxy.ProxyServer" || true' EXIT

java -cp out com.example.proxy.ProxyServer 10000 8 50 >/tmp/proxy_server.log 2>&1 &
proxy_pid=$!

for _ in $(seq 1 30); do
  if curl -fsS --max-time 2 http://127.0.0.1:10000 >/dev/null 2>&1; then
    break
  fi
  sleep 0.2
done

python3 - <<'PY'
import subprocess, sys, threading

proxy = 'http://127.0.0.1:10000'
url = 'http://127.0.0.1:8000/'
errors = []
lock = threading.Lock()

def worker(i):
    try:
        result = subprocess.run(
            ['curl', '-sS', '--max-time', '10', '-x', proxy, url],
            capture_output=True,
            text=True,
            timeout=20,
            check=False,
        )
        if result.returncode != 0:
            with lock:
                errors.append(f'curl rc={result.returncode}: {result.stderr.strip() or result.stdout.strip()[:200]}')
            return
        if '200' not in result.stdout[:200]:
            with lock:
                errors.append(f'bad body for request {i}: {result.stdout[:200]!r}')
    except Exception as exc:
        with lock:
            errors.append(f'exception {i}: {exc}')

threads = [threading.Thread(target=worker, args=(i,)) for i in range(200)]
for t in threads:
    t.start()
for t in threads:
    t.join()

if errors:
    print('Stress test failed with errors:')
    print('\n'.join(errors[:20]))
    sys.exit(1)

print('Stress test passed: 200 concurrent proxy requests succeeded.')
PY

kill "$proxy_pid" >/dev/null 2>&1 || true
wait "$proxy_pid" 2>/dev/null || true
