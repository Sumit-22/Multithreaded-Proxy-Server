'use client'

import { useEffect, useState } from "react"

export default function Page() {
  const [data, setData] = useState("Loading...")

  useEffect(() => {
    fetch("http://localhost:8080/metrics")
      .then(res => res.text())
      .then(res => setData(res))
      .catch(() => setData("Error connecting to backend"))
  }, [])

  return (
    <main className="min-h-dvh flex items-center justify-center p-6">
      <div>
        <h1 className="text-xl font-bold">Backend Metrics</h1>
        <pre className="mt-4">{data}</pre>
      </div>
    </main>
  )
}