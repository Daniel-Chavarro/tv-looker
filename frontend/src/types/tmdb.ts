export type TmdbOperationResult = {
  status: "started" | "completed" | "failed";
  message?: string;
  timestamp: string;
};

export type TmdbStatusResponse = {
    collectorRunning: boolean;
    lastSyncDate: string | null;
    syncEnabled: boolean;
    timestamp: string;
}