import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"

export default function Page() {
  return (
    <main className="min-h-dvh flex items-center justify-center p-6">
      <div className="w-full max-w-3xl">
        <Card>
          <CardHeader>
            <CardTitle className="text-balance">Multithreaded Web Server</CardTitle>
            <CardDescription className="text-pretty">
              Java-based HTTP/1.1 server with thread pool, LRU cache, rate limiting, and basic metrics.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <p className="text-sm">
              This Next.js app provides a simple landing page. The backend Java server source lives at:
              <span className="font-mono"> {"server/java/src/com/example/webserver"}</span>.
            </p>
            <ul className="list-disc pl-5 text-sm space-y-1">
              <li>Core files: Main.java, HttpServer.java, HttpRequest.java, HttpResponse.java</li>
              <li>Features: LruCache, RateLimiter (token bucket), basic Metrics, and Router</li>
              <li>
                Sample routes in Java server: <span className="font-mono">/</span>,{" "}
                <span className="font-mono">/healthz</span>, <span className="font-mono">/time</span>,{" "}
                <span className="font-mono">/echo</span>
              </li>
            </ul>
            <p className="text-xs text-muted-foreground">
              Note: Build and run the Java server separately from this UI. You can publish this UI on Vercel while
              running the Java server on your machine or a VM.
            </p>
          </CardContent>
        </Card>
      </div>
    </main>
  )
}
