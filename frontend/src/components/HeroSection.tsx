"use client";

import { motion } from "framer-motion";

interface HeroSectionProps {
  subscriberCount?: number | null;
}

export function HeroSection({ subscriberCount }: HeroSectionProps) {
  const formattedCount =
    typeof subscriberCount === "number"
      ? new Intl.NumberFormat("ko-KR").format(subscriberCount)
      : null;

  return (
    <section className="space-y-8 text-center">
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: "easeOut" }}
        className="hero-intro mx-auto flex max-w-3xl flex-col items-center space-y-6 text-center"
      >
        <h1 className="hero-title text-balance text-4xl font-bold leading-tight sm:text-5xl">
          게임 공지, 찾지 말고 받아보세요 <br /> 업데이트 소식을 메일로 보내드려요
        </h1>
        <p className="hero-subtitle max-w-xl text-balance text-sm">
          여러 게임들의 업데이트 소식을 실시간으로 감지해 메일로 알려드려요.
        </p>
        <span className="hero-badge rounded-full px-4 py-2 text-sm font-semibold">
          지금까지 {formattedCount ?? "많은"}명의 회원들이 키키를 구독했어요!
        </span>
      </motion.div>
    </section>
  );
}
