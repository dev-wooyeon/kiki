"use client";

import type { JSX } from "react";
import { motion } from "framer-motion";

export type FeatureCard = {
  name: string;
  description: string;
  details: string[];
  icon: JSX.Element;
};

export const featureCards: FeatureCard[] = [
  {
    name: "자동 수집",
    description: "30분마다 게임 공지 변화를 잡아내는 실시간 모니터링",
    details: [
      "게임별 변화를 추적하는 스케줄러",
      "중복 URL을 제거한 신규 공지 필터",
      "실패 시 재시도를 통해 안정적인 수집",
    ],
    icon: (
      <svg viewBox="0 0 24 24" className="h-9 w-9 text-sky-500" aria-hidden>
        <path
          fill="currentColor"
          d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm0 2a8 8 0 1 1 0 16 8 8 0 0 1 0-16Zm.75 2.5h-1.5v6l5.25 3.15.75-1.23-4.5-2.67V6.5Z"
        />
      </svg>
    ),
  },
  {
    name: "요약 메일",
    description: "아침 한 통의 digest 메일로 핵심만 전달해요.",
    details: [
      "게임사별로 그룹화된 HTML 템플릿",
      "구독/구독취소 토큰으로 간편 관리",
    ],
    icon: (
      <svg viewBox="0 0 24 24" className="h-9 w-9 text-sky-600" aria-hidden>
        <path d="M3 6.75A2.75 2.75 0 0 1 5.75 4h12.5A2.75 2.75 0 0 1 21 6.75v10.5A2.75 2.75 0 0 1 18.25 20H5.75A2.75 2.75 0 0 1 3 17.25V6.75Z" fill="none" stroke="currentColor" strokeWidth="1.8"/>
        <path d="M4 7.5l8 5.25L20 7.5" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
      </svg>
    ),
  },
  // 아카이브 기능은 준비 중이므로 표기하지 않습니다.
];

export function IconHighlights() {
  return (
    <section aria-label="서비스 핵심 기능" className="mx-auto grid w-full max-w-2xl gap-6 text-left">
      {featureCards.map((item) => (
        <motion.article
          key={item.name}
          className="feature-card glass-panel group relative flex h-full flex-col gap-4 rounded-3xl border p-6 text-left shadow-xl transition"
          whileHover={{ y: -8, scale: 1.01 }}
          transition={{ type: "spring", stiffness: 260, damping: 20 }}
        >
          <span className="feature-card-icon flex h-14 w-14 items-center justify-center rounded-2xl">
            {item.icon}
          </span>
          <div className="space-y-2">
            <h3 className="feature-card-title text-base font-semibold">{item.name}</h3>
            <p className="feature-card-text text-sm">{item.description}</p>
          </div>
          <ul className="mt-4 space-y-2 text-sm text-[color:var(--color-text)]">
            {item.details.map((detail) => (
              <li key={detail} className="flex items-start gap-2">
                <span className="panel-bullet mt-[6px] h-1.5 w-1.5" aria-hidden />
                <span>{detail}</span>
              </li>
            ))}
          </ul>
          <div className="pointer-events-none absolute inset-0 rounded-3xl border border-white/0 transition group-hover:border-white/20" />
        </motion.article>
      ))}
    </section>
  );
}
