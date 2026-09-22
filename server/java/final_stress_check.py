import os
import subprocess
import sys
import threading
import time
import urllib.request

repo_root = r"D:\Github Projects\GitHub\Multithreaded-Proxy-Server"
java_dir = os.path.join(repo_root, "server", "java")


def start_process(args, cwd):
    return subprocess.Popen(args, cwd=cwd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


compile_proc = subprocess.run(
    [
        "javac",
        "-d",
        "out",
        "src/com/example/proxy/ProxyServer.java",
        "src/com/example/proxy/ProxyHandler.java",
        "src/com/example/proxy/HttpProxyRequest.java",
        "src/com/example/proxy/ProxyCache.java",
        "src/com/example/proxy/ProxyMetrics.java",
    ],
    cwd=java_dir,
    capture_output=True,
    text=True,
)
print(f"compile_rc={compile_proc.returncode}")
if compile_proc.returncode != 0:
    print(compile_proc.stdout)
    print(compile_proc.stderr)
    sys.exit(compile_proc.returncode)

origin = start_process([sys.executable, "-m", "http.server", "8000", "--directory", repo_root], cwd=repo_root)
proxy = start_process(["java", "-cp", "out", "com.example.proxy.ProxyServer", "10000", "25", "200"], cwd=java_dir)

try:
    ready = False
    for _ in range(80):
        try:
            with urllib.request.urlopen("http://127.0.0.1:10000", timeout=2) as r:
                if r.status == 200:
                    ready = True
                    break
        except Exception:
            pass
        time.sleep(0.1)

    if not ready:
        raise RuntimeError("proxy did not become ready")

    proxy_handler = urllib.request.ProxyHandler({
        "http": "http://127.0.0.1:10000",
        "https": "http://127.0.0.1:10000",
    })
    errors = []
    lock = threading.Lock()

    def worker(i):
        try:
            opener = urllib.request.build_opener(proxy_handler)
            req = urllib.request.Request("http://127.0.0.1:8000/", headers={"User-Agent": f"stress-{i}"})
            with opener.open(req, timeout=12) as r:
                body = r.read()
                if r.status != 200:
                    with lock:
                        errors.append(f"status {r.status} req {i}")
                if len(body) < 100:
                    with lock:
                        errors.append(f"short body req {i} len={len(body)}")
        except Exception as exc:
            with lock:
                errors.append(f"exception req {i}: {exc}")

    threads = [threading.Thread(target=worker, args=(i,)) for i in range(200)]
    start = time.perf_counter()
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    elapsed = time.perf_counter() - start

    print(f"concurrent_requests=200")
    print(f"errors={len(errors)}")
    print(f"elapsed_seconds={elapsed:.2f}")
    print(f"success_rate={100*(200-len(errors))/200:.2f}%")
    if errors:
        print(errors[:10])
        sys.exit(1)
    print("verified=pass")
finally:
    proxy.terminate()
    try:
        proxy.wait(timeout=5)
    except subprocess.TimeoutExpired:
        proxy.kill()
        proxy.wait()

    origin.terminate()
    try:
        origin.wait(timeout=5)
    except subprocess.TimeoutExpired:
        origin.kill()
        origin.wait()
