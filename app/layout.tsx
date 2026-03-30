import type { Metadata } from 'next'
import { Geist, Geist_Mono } from 'next/font/google'
import './globals.css'

// Load fonts
const geist = Geist({ subsets: ['latin'] })
const geistMono = Geist_Mono({ subsets: ['latin'] })

// Project metadata — replace with your own app info
export const metadata: Metadata = {
    title: 'Web Server Dashboard',
    description: 'Landing page for the Java-based multithreaded HTTP server project.',
    authors: [{ name: 'Sumit Kumar' }],
    applicationName: 'WebServerUI',
}

export default function RootLayout({
                                       children,
                                   }: Readonly<{
    children: React.ReactNode
}>) {
    return (
        <html lang="en">
        <body className={`${geist.className} ${geistMono.className} font-sans antialiased`}>
        {children}
        </body>
        </html>
    )
}
