"use client";

import Link from "next/link";
import { motion, useMotionTemplate, useMotionValue, useSpring } from "framer-motion";
import { useEffect, useState, type MouseEvent } from "react";
import { BackgroundStars } from "@/components/BackgroundStars";
import { HeroSection } from "@/components/HeroSection";
import { SubscriptionForm } from "@/components/SubscriptionForm";
import { IconHighlights } from "@/components/IconHighlights";
import { Logo } from "@/components/Logo";
import { useTheme } from "@/components/ThemeProvider";
import { fetchSubscriptionStats } from "@/lib/api";

export function LandingShell() {
  const currentYear = new Date().getFullYear();
  const { theme, toggleTheme } = useTheme();
  const [subscriberCount, setSubscriberCount] = useState<number | null>(null);
  const mouseX = useMotionValue(0);
  const mouseY = useMotionValue(0);
  const smoothX = useSpring(mouseX, { stiffness: 120, damping: 18, mass: 0.2 });
  const smoothY = useSpring(mouseY, { stiffness: 120, damping: 18, mass: 0.2 });
  const glow = useMotionTemplate`radial-gradient(420px circle at ${smoothX}px ${smoothY}px, rgba(56, 189, 248, 0.12), transparent 70%)`;

  useEffect(() => {
    const handleResize = () => {
      mouseX.set(window.innerWidth / 2);
      mouseY.set(window.innerHeight / 3);
    };
    handleResize();
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, [mouseX, mouseY]);

  useEffect(() => {
    let mounted = true;
    (async () => {
      const stats = await fetchSubscriptionStats();
      if (mounted && stats?.activeCount != null) {
        setSubscriberCount(stats.activeCount);
      }
    })();
    return () => {
      mounted = false;
    };
  }, []);

  const handleMouseMove = (event: MouseEvent<HTMLDivElement>) => {
    const rect = event.currentTarget.getBoundingClientRect();
    mouseX.set(event.clientX - rect.left);
    mouseY.set(event.clientY - rect.top);
  };

  const isDark = theme === "dark";

  return (
    <div
      className={`landing-shell relative min-h-screen overflow-hidden transition-colors duration-500 ${isDark ? "bg-slate-950 text-slate-100" : "bg-slate-50 text-slate-900"}`}
      data-theme={theme}
      onMouseMove={handleMouseMove}
    >
      {isDark ? <BackgroundStars /> : null}
      <motion.div
        aria-hidden
        className="pointer-events-none absolute inset-0 z-[1] opacity-60"
        style={{ background: glow }}
      />
      <div className="relative z-10 flex min-h-screen flex-col">
        <header className="sticky top-0 z-20 border-b border-[color:rgba(148,163,184,0.24)] bg-[color:var(--navbar-bg)] backdrop-blur-xl transition-colors">
          <nav className="mx-auto flex w-full max-w-6xl items-center justify-between px-6 py-5">
            <Link href="/" className="flex items-center gap-3 text-sm font-semibold tracking-tight text-[color:var(--navbar-text)] transition hover:opacity-90">
              <Logo />
            </Link>
            <div className="flex items-center gap-4 text-sm">
              <a href="#features" className="nav-link hidden md:inline">핵심 기능</a>
              <a href="#subscribe" className="nav-link hidden md:inline">구독</a>
              <button
                type="button"
                onClick={toggleTheme}
                className="theme-toggle inline-flex h-10 w-10 items-center justify-center rounded-full border border-transparent/40 transition hover:scale-105"
                aria-label="테마 전환"
              >
                {isDark ? (
                  <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden>
                    <path
                      fill="currentColor"
                      d="M17.75 15.09a6.5 6.5 0 0 1-7.84-7.84 7 7 0 1 0 7.84 7.84Zm2.11-9.45L18 2.78 16.14 5.7 12.9 4.64 14 8 12 11.36l3.24-1.06L18 13.3l1.86-2.93L23 11.36 21 8l1.1-3.36-3.24 1.1Z"
                    />
                  </svg>
                ) : (
                  <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden>
                    <path
                      fill="currentColor"
                      d="M12 18a6 6 0 1 1 0-12 6 6 0 0 1 0 12Zm0-16a1 1 0 0 1 1 1v1a1 1 0 1 1-2 0V3a1 1 0 0 1 1-1Zm0 18a1 1 0 0 1 1 1v1a1 1 0 1 1-2 0v-1a1 1 0 0 1 1-1Zm9-7a1 1 0 0 1-1 1h-1a1 1 0 1 1 0-2h1a1 1 0 0 1 1 1ZM5 12a1 1 0 0 1-1 1H3a1 1 0 1 1 0-2h1a1 1 0 0 1 1 1Zm15.07 7.07a1 1 0 0 1-1.41 0l-.7-.7a1 1 0 1 1 1.42-1.42l.7.7a1 1 0 0 1 0 1.42ZM6.05 6.05A1 1 0 0 1 7.46 4.64l.7.7A1 1 0 0 1 6.75 6.76l-.7-.7Zm12.02-.7a1 1 0 1 1 1.41 1.41l-.7.7a1 1 0 0 1-1.42-1.42Zm-12 12a1 1 0 0 1 1.42 1.42l-.7.7a1 1 0 1 1-1.42-1.42l.7-.7Z"
                    />
                  </svg>
                )}
              </button>
            </div>
          </nav>
        </header>

        <main className="flex flex-1 flex-col items-center gap-16 px-6 pb-20 pt-16 sm:pt-24">
          <HeroSection subscriberCount={subscriberCount} />
          <section id="features" className="w-full max-w-2xl space-y-6">
            <IconHighlights />
            <div id="subscribe">
              <SubscriptionForm
                onSubscribed={(nextCount) =>
                  setSubscriberCount((prev) =>
                    typeof nextCount === "number"
                      ? nextCount
                      : typeof prev === "number"
                        ? prev + 1
                        : 1,
                  )
                }
              />
            </div>
          </section>
        </main>

        <footer className="footer mx-auto w-full max-w-6xl px-6 pb-12 text-center text-xs">
          © {currentYear} KIKI
        </footer>
      </div>
    </div>
  );
}
