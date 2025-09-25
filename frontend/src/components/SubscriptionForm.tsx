"use client";

import { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import type { AxiosError } from "axios";
import { apiClient } from "@/lib/api";

type FormValues = {
  email: string;
};

type ApiResponse = {
  success: boolean;
  message: string;
};

const defaultSuccessMessage = "구독이 완료되었습니다. 받은 편지함을 확인해주세요.";

type SubscriptionFormProps = {
  onSubscribed?: (nextCount?: number) => void;
};

export function SubscriptionForm({ onSubscribed }: SubscriptionFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<FormValues>({
    mode: "onBlur",
    defaultValues: { email: "" },
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [isError, setIsError] = useState(false);
  const [isDesktop, setIsDesktop] = useState(false);
  const inputRef = useRef<HTMLInputElement | null>(null);

  // 게임 선택 상태 (기본 전체 선택)
  const [games, setGames] = useState({ nikke: true, mabinogi: true, wuthering: true });

  useEffect(() => {
    // 데스크톱 환경 판단 및 자동 포커스
    if (typeof window !== "undefined") {
      const desktop = window.matchMedia && window.matchMedia("(pointer: fine)").matches;
      setIsDesktop(desktop);
      // 페이지 진입 시 포커스(데스크톱에서만)
      if (desktop) {
        setTimeout(() => inputRef.current?.focus(), 0);
      }
    }
  }, []);

  const onSubmit = handleSubmit(async (values) => {
    setIsSubmitting(true);
    setFeedback(null);
    setIsError(false);

    try {
      // 현재 백엔드 스키마는 email만 허용하므로 values만 전송
      const response = await apiClient.post<ApiResponse>("/subscribe", values);
      const message = response.data?.message?.trim() || defaultSuccessMessage;
      setFeedback(message);
      setIsError(false);
      reset();
      // 제출 후에도 입력칸 포커스 유지(데스크톱)
      if (isDesktop) setTimeout(() => inputRef.current?.focus(), 0);

      // 구독자 수 갱신 요청 (비동기)
      try {
        const { fetchSubscriptionStats } = await import("@/lib/api");
        const stats = await fetchSubscriptionStats();
        onSubscribed?.(stats?.activeCount);
      } catch {
        // 문제가 있어도 UI 흐름은 유지 (옵티미스틱 +1)
        onSubscribed?.();
      }
    } catch (error) {
      const axiosError = error as AxiosError<ApiResponse>;
      const message =
        axiosError.response?.data?.message || "구독 요청 처리 중 문제가 발생했습니다.";
      setFeedback(message);
      setIsError(true);
    } finally {
      setIsSubmitting(false);
    }
  });

  // 키보드 단축키 처리: Shift+Enter 제출, Shift+Q/W/E 토글
  const handleKeyDown: React.KeyboardEventHandler<HTMLInputElement> = (e) => {
    if (!e.shiftKey) return;
    // 레이아웃/언어와 무관한 물리 키 인식: e.code 사용 (KeyQ/KeyW/KeyE)
    const code = e.code;
    const keyLower = e.key.toLowerCase();
    if (keyLower === "enter" || code === "Enter" || code === "NumpadEnter") {
      e.preventDefault();
      // 폼 제출 트리거
      void onSubmit();
      return;
    }
    if (code === "KeyQ" || code === "KeyW" || code === "KeyE") {
      e.preventDefault();
      setGames((prev) => {
        const next = { ...prev };
        if (code === "KeyQ") next.nikke = !prev.nikke;
        if (code === "KeyW") next.mabinogi = !prev.mabinogi;
        if (code === "KeyE") next.wuthering = !prev.wuthering;
        return next;
      });
    }
  };

  // 전역(페이지 레벨) 단축키: 입력 포커스가 없어도 Shift 조합키 동작
  useEffect(() => {
    if (!isDesktop) return;
    const handler = (e: KeyboardEvent) => {
      if (!e.shiftKey) return;
      const code = e.code;
      const keyLower = (e.key || "").toLowerCase();
      if (keyLower === "enter" || code === "Enter" || code === "NumpadEnter") {
        e.preventDefault();
        void onSubmit();
        return;
      }
      if (code === "KeyQ" || code === "KeyW" || code === "KeyE") {
        e.preventDefault();
        setGames((prev) => {
          const next = { ...prev };
          if (code === "KeyQ") next.nikke = !prev.nikke;
          if (code === "KeyW") next.mabinogi = !prev.mabinogi;
          if (code === "KeyE") next.wuthering = !prev.wuthering;
          return next;
        });
      }
    };
    window.addEventListener("keydown", handler);
    return () => window.removeEventListener("keydown", handler);
  }, [isDesktop, onSubmit]);

  return (
    <div className="subscription-card glass-panel glow-border w-full rounded-3xl border p-10 shadow-2xl">
      <h2 className="subscription-title mb-3 text-3xl font-semibold">구독 신청</h2>
      <p className="subscription-caption mb-6 text-xs">필요한 건 이메일 하나뿐이에요.</p>
      <form onSubmit={onSubmit} className="space-y-5">
        <div className="space-y-2">
          <label htmlFor="email" className="subscription-label block text-sm font-semibold">
            이메일 주소
          </label>
          <div className="relative">
            <input
              id="email"
              type="email"
              autoComplete="email"
              placeholder="you@example.com"
              className="subscription-input w-full rounded-2xl border px-4 py-3 pr-12 placeholder:text-slate-500 shadow-inner focus:outline-none focus:ring-2"
              onKeyDown={handleKeyDown}
              {
                ...(() => {
                  const { ref: formRef, ...rest } = register("email", {
                    required: "이메일을 입력해주세요.",
                    pattern: {
                      value: /\S+@\S+\.\S+/,
                      message: "올바른 이메일 형식이 아닙니다.",
                    },
                  });
                  return {
                    ...rest,
                    ref: (el: HTMLInputElement | null) => {
                      formRef(el);
                      inputRef.current = el;
                    },
                  } as const;
                })()
              }
              disabled={isSubmitting}
            />
            {feedback && !isError && (
              <span className="input-status input-status-success" aria-label="성공" title={feedback}>
                <svg viewBox="0 0 20 20" aria-hidden>
                  <path d="M16 6l-7.5 8L4 10.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </span>
            )}
            {feedback && isError && (
              <span className="input-status input-status-error" aria-label="오류" title={feedback}>
                <svg viewBox="0 0 20 20" aria-hidden>
                  <path d="M6 6l8 8M14 6l-8 8" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              </span>
            )}
          </div>
          {errors.email && (
            <p className="subscription-error text-sm">{errors.email.message}</p>
          )}
          {!errors.email && feedback && (
            <p className={`text-sm ${isError ? "subscription-error" : "subscription-success"}`}>{feedback}</p>
          )}
          {isDesktop && (
            <div className="mt-3 flex flex-wrap items-center gap-2">
              <button
                type="button"
                aria-pressed={games.nikke}
                onClick={() => setGames((p) => ({ ...p, nikke: !p.nikke }))}
                className={`chip rounded-xl px-3 py-1.5 text-xs ${games.nikke ? "chip-active" : ""}`}
              >
                <span className="inline-flex items-center gap-1">
                  {games.nikke ? (
                    <svg viewBox="0 0 20 20" className="h-3.5 w-3.5" aria-hidden>
                      <path d="M16 6l-7.5 8L4 10.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  ) : (
                    <span className="inline-block h-3.5 w-3.5 rounded-[4px] border" />
                  )}
                  니케 <span className="opacity-70">(Shift+Q)</span>
                </span>
              </button>
              <button
                type="button"
                aria-pressed={games.mabinogi}
                onClick={() => setGames((p) => ({ ...p, mabinogi: !p.mabinogi }))}
                className={`chip rounded-xl px-3 py-1.5 text-xs ${games.mabinogi ? "chip-active" : ""}`}
              >
                <span className="inline-flex items-center gap-1">
                  {games.mabinogi ? (
                    <svg viewBox="0 0 20 20" className="h-3.5 w-3.5" aria-hidden>
                      <path d="M16 6l-7.5 8L4 10.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  ) : (
                    <span className="inline-block h-3.5 w-3.5 rounded-[4px] border" />
                  )}
                  마비노기 모바일 <span className="opacity-70">(Shift+W)</span>
                </span>
              </button>
              <button
                type="button"
                aria-pressed={games.wuthering}
                onClick={() => setGames((p) => ({ ...p, wuthering: !p.wuthering }))}
                className={`chip rounded-xl px-3 py-1.5 text-xs ${games.wuthering ? "chip-active" : ""}`}
              >
                <span className="inline-flex items-center gap-1">
                  {games.wuthering ? (
                    <svg viewBox="0 0 20 20" className="h-3.5 w-3.5" aria-hidden>
                      <path d="M16 6l-7.5 8L4 10.5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  ) : (
                    <span className="inline-block h-3.5 w-3.5 rounded-[4px] border" />
                  )}
                  명조 <span className="opacity-70">(Shift+E)</span>
                </span>
              </button>
              <span className="ml-auto text-[11px] text-[color:var(--subscription-footnote)]">
                단축키: Shift+Enter 제출
              </span>
            </div>
          )}
        </div>
        <button
          type="submit"
          disabled={isSubmitting}
          className="subscription-button inline-flex w-full items-center justify-center rounded-2xl px-4 py-3 font-semibold shadow-lg transition disabled:cursor-not-allowed disabled:opacity-60"
        >
          {isSubmitting ? "무료 구독 처리 중..." : "무료 구독하기"}
        </button>
      </form>
      <p className="subscription-footnote mt-5 text-[11px]">취소 링크는 메일 하단에 포함됩니다.</p>
    </div>
  );
}
