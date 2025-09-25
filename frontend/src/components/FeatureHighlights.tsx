const features = [
  {
    title: "게임별 전용 스크래퍼",
    description:
      "니케, 마비노기 모바일, 명조를 위한 전용 파서로 구조 변경에도 빠르게 대응할 수 있는 아키텍처를 유지합니다.",
  },
  {
    title: "스마트 중복 필터",
    description:
      "이미 전달된 공지는 자동으로 제외하고 새로운 소식만 정리하여, 받은 편지함이 깔끔하게 유지됩니다.",
  },
  {
    title: "한 눈에 이메일",
    description:
      "게임사별로 그룹화된 HTML 템플릿을 사용해 하루치 내용을 한 번에 검토할 수 있도록 디자인했습니다.",
  },
  {
    title: "간편 구독 관리",
    description:
      "이메일만으로 구독을 시작하고, 한 번의 클릭으로 구독 취소까지. 인증 없이도 관리가 가능합니다.",
  },
];

export function FeatureHighlights() {
  return (
    <section id="features" className="mx-auto w-full max-w-5xl space-y-8">
      <div className="space-y-3 text-center">
        <span className="text-xs uppercase tracking-[0.35em] text-slate-400/80">
          Platform Capabilities
        </span>
        <h2 className="text-3xl font-semibold text-white sm:text-4xl">
          자동화와 요약에 최적화된 운영 흐름
        </h2>
        <p className="text-sm text-slate-200/70 sm:text-base">
          확장 가능한 Kotlin/Spring 백엔드와 Next.js 프런트엔드를 기반으로, 새로운 게임 추가와 아카이브
          경험도 매끄럽게 준비하고 있어요.
        </p>
      </div>
      <div className="grid gap-6 md:grid-cols-2">
        {features.map((feature) => (
          <article
            key={feature.title}
            className="glass-panel rounded-3xl border border-white/10 p-8 text-left shadow-2xl shadow-indigo-950/40"
          >
            <h3 className="text-lg font-semibold text-white/95">{feature.title}</h3>
            <p className="mt-3 text-sm leading-relaxed text-slate-200/75">
              {feature.description}
            </p>
          </article>
        ))}
      </div>
    </section>
  );
}
