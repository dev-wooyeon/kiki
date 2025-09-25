type LogoProps = {
  className?: string;
};

export function Logo({ className }: LogoProps) {
  return (
    <div className={["inline-flex items-center gap-3", className].filter(Boolean).join(" ")}>
      <div className="relative flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-sky-400 via-blue-500 to-slate-900 p-[2px] shadow-lg shadow-blue-500/25">
        <div className="flex h-full w-full items-center justify-center rounded-[0.85rem] bg-[color:var(--color-background)]">
          <svg
            aria-hidden
            viewBox="0 0 32 32"
            className="h-7 w-7"
            role="img"
          >
            <defs>
              <linearGradient id="kiki-logo-gradient" x1="0" x2="1" y1="0" y2="1">
                <stop offset="0%" stopColor="#38bdf8" />
                <stop offset="50%" stopColor="#2563eb" />
                <stop offset="100%" stopColor="#1e293b" />
              </linearGradient>
            </defs>
            <path
              d="M11 7v18m0-9 9-9m-9 9 9 9"
              fill="none"
              stroke="url(#kiki-logo-gradient)"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2.8}
            />
          </svg>
        </div>
      </div>
      <span className="text-xl font-semibold tracking-tight text-[color:var(--navbar-text)]">키키</span>
    </div>
  );
}
