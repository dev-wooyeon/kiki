import axios from "axios";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api";

export const apiClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000,
});

export type SubscriptionStats = {
  activeCount: number;
};

export async function fetchSubscriptionStats(): Promise<SubscriptionStats | null> {
  try {
    const response = await apiClient.get<{ success: boolean; data?: { activeCount?: number } }>(
      "/subscribe/stats",
    );
    const count = response.data?.data?.activeCount;
    if (typeof count === "number" && Number.isFinite(count)) {
      return { activeCount: count };
    }
    return null;
  } catch {
    return null;
  }
}
