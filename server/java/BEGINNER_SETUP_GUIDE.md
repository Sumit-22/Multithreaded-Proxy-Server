# 🚀 Beginner's Guide: Running the Proxy Server on IntelliJ IDEA

> **Note:** This is a **pure Java project**, not built with Spring Boot or any framework — only Java is required.

---

## 📥 Step 1: Get the Project

### Option A — Download as ZIP (Recommended for Beginners)

1. Click the **three dots (⋮)** on the repository page → choose **Download ZIP**
2. Wait for the download to finish
3. **Extract (unzip)** the folder
    - On Windows: Right-click → *Extract All*
    - On macOS: Just double-click
4. Remember where you extracted it, e.g.  
   `C:\Users\YourName\Downloads\webserver-main`

### Option B — Clone Using Git

```bash

git clone <your-repo-url>
cd server/java
```

---

## 🎯 Step 2: Open the Project in IntelliJ IDEA

1. Launch **IntelliJ IDEA**
2. Go to **File → Open**
3. Navigate to the extracted folder → open the **server/java** subfolder
4. Click **Open** (or **Open as Project**)
5. Wait a few moments while IntelliJ indexes the files

---

## ⚙️ Step 3: Configure the Java SDK

This step ensures IntelliJ uses the correct JDK version.

1. Go to **File → Project Structure**
2. Select **Project** on the left sidebar
3. Under **Project SDK**, open the dropdown
4. Choose **Java 17** (or newer)

    * If it’s not available, click **Add SDK → Download JDK**
    * Select version **17 or higher** and download it
5. Click **Apply → OK**

✅ Now IntelliJ is set up to run the project correctly.

---

## 🔍 Step 4: Check Your Project Structure

Your folder view should look like this:

```
ProxyServer
├── src/
│   └── com/example/proxy/
│       ├── ProxyServer.java        ← Main file
│       ├── ProxyHandler.java
│       ├── ProxyCache.java
│       ├── HttpProxyRequest.java
│       └── ProxyMetrics.java
├── .idea/
└── ProxyServer.iml
```

If files don’t appear under **src**, right-click the folder → **Mark Directory as → Sources Root**.

---

## ▶️ Step 5: Run the Proxy Server

### Method 1 — Quick Run

1. Open **src/com/example/proxy/ProxyServer.java**
2. Right-click the file → select **Run 'ProxyServer.main()'**
3. Wait a few seconds...

If successful, you’ll see:

```
ProxyServer started on port 9090
Listening for incoming connections...
```

✅ Your proxy server is now running!

### Method 2 — Run Configuration (if the above doesn’t work)

1. Go to **Run → Edit Configurations**
2. Click the **+** icon → choose **Application**
3. Fill in the following:

    * **Name:** ProxyServer
    * **Main class:** com.example.proxy.ProxyServer
    * **Working directory:** leave as default

4. Click **Apply → OK**
5. Press the green **Run** button (or hit `Shift + F10`)

---

## 🧪 Step 6: Test the Proxy Server

Open a new **Terminal** or **Command Prompt**, and try these:

```bash
# Test if the server is running
curl -v http://localhost:9090/

# Send a request through the proxy
curl -v -x http://localhost:9090 http://www.example.com/

# Test caching (second request should be faster)
curl -v -x http://localhost:9090 http://www.example.com/
```

You should see response headers and HTML output from the requested site.

---

## 🛑 Step 7: Stop the Server

In IntelliJ:

* Click the **red square (⏹)** in the bottom toolbar  
  **OR**
* Press **Ctrl + C** if running in a terminal

---

## 🚨 Common Issues & Fixes

### ❌ “Cannot find symbol 'ProxyServer'”

**Fix:**

* Right-click the **src** folder → **Mark Directory as → Sources Root**
* Then go to **Build → Rebuild Project**

---

### ❌ “Java 17 not found”

**Fix:**

* Go to **File → Project Structure**
* Under **Project SDK**, click **Add SDK → Download JDK**
* Choose **Java 17**, then apply changes

---

### ❌ “Port 9090 already in use”

**Fix:**

* Open `ProxyServer.java`
* Find this line:
  `private static final int PORT = 9090;`
* Change it to another number, e.g. `9999`
* Save and run again

---

### ❌ “Connection refused” during testing

**Fix:**

* Make sure the run console says “Listening for incoming connections…”
* Wait a few seconds before sending requests
* Ensure the proxy is running on the correct port

---

## 📊 What This Project Does

This Proxy Server:

* Listens for client connections on **port 9090**
* Forwards HTTP requests to external servers
* Uses **caching** to speed up repeat requests
* Handles **multiple concurrent requests** with a thread pool
* Tracks and logs performance using **ProxyMetrics**

---

## 🎓 What to Try Next

* **View Logs:** Watch IntelliJ’s console to see cache hits and responses
* **Tweak Settings:** Edit `ProxyServer.java` to change:
    * Port (`PORT`)
    * Thread pool size (`THREAD_POOL_SIZE`)
    * Cache limit (`CACHE_SIZE`)
* **Performance Testing:** Write your own client script to stress-test the proxy
* **Monitor Metrics:** Use `ProxyMetrics` to analyze performance trends

---

## ✅ Final Checklist

* [ ] Java 17+ installed
* [ ] IntelliJ IDEA installed
* [ ] Project extracted or cloned
* [ ] SDK configured in IntelliJ
* [ ] `ProxyServer.java` runs without errors
* [ ] Console shows “Listening for incoming connections…”
* [ ] `curl http://localhost:9090/` gives a valid response

✅ If everything checks out, your Proxy Server is ready!

---
**Happy Coding! 🎉**
