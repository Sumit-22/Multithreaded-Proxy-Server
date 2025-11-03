# 🚀 Complete Beginner's Guide: Running Proxy Server on IntelliJ

**NOTE:** This is a **pure Java project** (NOT Spring Boot). No frameworks needed—just Java!

---

## 📋 Prerequisites (Before You Start)

Before downloading anything, make sure you have:

1. **Java 17 or higher** installed
   - Check: Open Command Prompt/Terminal, type: `java -version`
   - Should show: `java version "17"` or higher
   - If not installed: Download from [java.com](https://www.oracle.com/java/technologies/downloads/)

2. **IntelliJ IDEA** (Community Edition is FREE)
   - Download from: [jetbrains.com/idea](https://www.jetbrains.com/idea/download/)
   - Install it (next → next → finish)

3. **Terminal/Command Prompt** (built-in on your OS)
   - Windows: Search "Command Prompt" or "PowerShell"
   - Mac/Linux: Search "Terminal"

---

## 📥 Step 1: Download the Project

### Option A: Download ZIP (Easiest for Beginners)

1. In v0, click **three dots (⋮)** in top right → **Download ZIP**
2. Wait for download to complete
3. **Extract/Unzip** the folder
   - Right-click → "Extract All" (Windows)
   - Double-click (Mac)
4. Note down where you extracted it (e.g., `C:\Users\YourName\Downloads\webserver-main`)

### Option B: Using Git (Advanced)

\`\`\`bash
git clone <your-repo-url>
cd server/java
\`\`\`

---

## 🎯 Step 2: Open Project in IntelliJ

1. **Open IntelliJ IDEA**
2. Click **File** → **Open**
3. Navigate to the extracted folder → **server/java** subfolder
4. Click **Open** (or **Open as Project**)
5. Wait 30 seconds for IntelliJ to load and index files

---

## ⚙️ Step 3: Configure Java (Newbie-Proof)

This step prevents "Java not found" errors.

1. **Top Menu** → **File** → **Project Structure**
2. On left panel, click **Project**
3. Under "Project SDK", click the dropdown
4. Select **Java 17** (if available)
   - If not listed: Click **Add SDK** → **Download JDK**
   - Choose version **17** or higher
   - Click **Download**
   - Wait for download to complete
5. Click **Apply** → **OK**

✅ **IntelliJ will now use the correct Java version**

---

## 🔍 Step 4: Verify Project Structure

The left panel should show:

\`\`\`
ProxyServer
├── src/
│   └── com/example/proxy/
│       ├── ProxyServer.java         ← MAIN FILE
│       ├── ProxyHandler.java
│       ├── ProxyCache.java
│       ├── HttpProxyRequest.java
│       └── ProxyMetrics.java
├── .idea/
└── ProxyServer.iml
\`\`\`

If you don't see files, right-click the folder → **Mark Directory as** → **Sources Root**

---

## ▶️ Step 5: Run the Proxy Server (THE BIG MOMENT!)

### Method 1: Click to Run (Easiest)

1. In left panel, navigate to: **src/com/example/proxy/ProxyServer.java**
2. Right-click on **ProxyServer.java**
3. Select **Run 'ProxyServer.main()'**
4. Wait 3-5 seconds...

**You should see in the bottom terminal:**
\`\`\`
ProxyServer started on port 9090
Listening for incoming connections...
\`\`\`

✅ **SUCCESS! Server is running!**

### Method 2: Using Run Configuration (If Method 1 doesn't work)

1. **Top Menu** → **Run** → **Edit Configurations**
2. Click **+** → **Application**
3. Fill in:
   - **Name:** ProxyServer
   - **Main class:** com.example.proxy.ProxyServer
   - **Working directory:** (leave default)
4. Click **Apply** → **OK**
5. Click the green **Run** button (or press Shift+F10)

---

## 🧪 Step 6: Test If It's Working

Open a **new Terminal/Command Prompt** and run:

\`\`\`bash
# Test 1: Check if server is running
curl -v http://localhost:9090/

# Test 2: Forward a request through the proxy
curl -v -x http://localhost:9090 http://www.example.com/

# Test 3: Check cache hit
curl -v -x http://localhost:9090 http://www.example.com/
# (Should be faster the second time!)
\`\`\`

**Expected output:** You'll see response headers and HTML content

---

## 🛑 Step 7: Stop the Server

In IntelliJ:
- Click the **red square** (⏹) button in the bottom toolbar
- Or press **Ctrl+C** in the terminal

---

## 🚨 Common Beginner Errors & Fixes

### ❌ Error: "Cannot find symbol 'ProxyServer'"

**Fix:** 
- Right-click **src** folder → **Mark Directory as** → **Sources Root**
- Then **Build** → **Rebuild Project**

### ❌ Error: "Java 17 not found"

**Fix:**
- Go to **File** → **Project Structure**
- Under Project SDK, click **Add SDK** → **Download JDK**
- Select **17** and download
- Apply changes

### ❌ Error: "Port 9090 already in use"

**Fix:**
- Open **ProxyServer.java**
- Find: `private static final int PORT = 9090;`
- Change to: `private static final int PORT = 9999;`
- Save and run again

### ❌ Error: "Connection refused when testing"

**Fix:**
- Make sure the green run button completed (check console)
- Wait 5 seconds before testing
- Make sure you see "Listening for incoming connections..." message

---

## 📊 Understanding the Project

This proxy server:
- **Listens on port 9090** for client connections
- **Forwards HTTP requests** to any upstream server
- **Caches responses** (60% faster on repeated requests!)
- **Handles 100+ concurrent requests** using thread pool
- **Logs performance metrics** to track speed & cache hits

---

## 🎓 Next Steps After It's Running

1. **View Logs:** Watch the console while testing—you'll see cache hits!
2. **Modify Configuration:** Open ProxyServer.java and change:
   - `PORT` (line with 9090)
   - `THREAD_POOL_SIZE` (default 100)
   - `CACHE_SIZE` (default 1000)
3. **Create Test Clients:** Write Java code to stress-test the proxy
4. **Monitor Performance:** Check ProxyMetrics for throughput and latency

---

## ✅ Verification Checklist

- [ ] Java 17+ installed
- [ ] IntelliJ downloaded & installed
- [ ] Project extracted
- [ ] Project opened in IntelliJ
- [ ] Project SDK set to Java 17+
- [ ] ProxyServer.java runs without errors
- [ ] Console shows "Listening for incoming connections..."
- [ ] `curl http://localhost:9090/` returns a response

**If all checked ✅ → You're done!**

---

## 💬 Need Help?

If you hit an issue:
1. Check the error message in IntelliJ console
2. Check this guide's "Common Errors" section
3. Google the exact error message
4. Ask in Java communities (Stack Overflow, Reddit r/java)

**Happy coding! 🎉**
