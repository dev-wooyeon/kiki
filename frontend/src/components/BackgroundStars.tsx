export function BackgroundStars() {
  return (
    <div className="pointer-events-none absolute inset-0 overflow-hidden">
      <div className="stars" />
      <div className="stars2" />
      <div className="stars3" />
      <div className="absolute top-[-10%] left-[10%] h-80 w-80 rounded-full bg-sky-400/30 blur-[160px]" />
      <div className="absolute bottom-[-20%] right-[5%] h-[26rem] w-[26rem] rounded-full bg-blue-500/20 blur-[200px]" />
      <div className="absolute top-[40%] left-1/2 h-64 w-64 -translate-x-1/2 rounded-full bg-cyan-400/15 blur-[140px]" />
    </div>
  );
}
