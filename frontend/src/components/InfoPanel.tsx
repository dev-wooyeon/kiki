"use client";

import { AnimatePresence, motion } from "framer-motion";
import { Fragment } from "react";
import type { FeatureCard } from "@/components/IconHighlights";

interface InfoPanelProps {
  open: boolean;
  card: FeatureCard | null;
  onClose: () => void;
}

export function InfoPanel({ open, card, onClose }: InfoPanelProps) {
  return (
    <AnimatePresence>
      {open && card && (
        <Fragment>
          <motion.div
            className="info-overlay fixed inset-0 z-40"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          />
          <motion.div
            role="dialog"
            aria-modal="true"
            className="info-panel glass-panel fixed inset-x-4 top-24 z-50 mx-auto max-w-lg rounded-3xl border p-8 shadow-2xl"
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 24 }}
          >
            <div className="mb-6 flex items-center justify-between">
              <h2 className="panel-title text-lg font-semibold">{card.name}</h2>
              <button
                type="button"
                onClick={onClose}
                className="panel-close inline-flex h-8 w-8 items-center justify-center rounded-full text-sm"
                aria-label="정보 패널 닫기"
              >
                ×
              </button>
            </div>
            <div className="panel-summary mb-6 flex items-center gap-3 rounded-2xl p-4">
              <span className="panel-icon flex h-12 w-12 items-center justify-center rounded-2xl" aria-hidden>
                {card.icon}
              </span>
              <p className="panel-text text-sm">{card.description}</p>
            </div>
            <ul className="panel-list space-y-2 text-sm">
              {card.details.map((detail) => (
                <li key={detail} className="flex items-start gap-2">
                  <span className="panel-bullet mt-[6px] h-1.5 w-1.5" aria-hidden />
                  <span>{detail}</span>
                </li>
              ))}
            </ul>
          </motion.div>
        </Fragment>
      )}
    </AnimatePresence>
  );
}
