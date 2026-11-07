import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { gameService } from "../../services/gameService";
import type { GameSearchParams, GameSummary, Lookup, PageResponse } from "../../types/catalog";

const PAGE_SIZE = 12;

/** Phase 6 — Catalogue browse/search/filter/sort. The "Play Now" link only ever appears
 *  on Arena's detail page (see GameDetailPage), not here — this page just lists what
 *  exists, honestly, including the five placeholder entries with no engine behind them yet. */
export function GameCatalogPage() {
  const [genres, setGenres] = useState<Lookup[]>([]);
  const [platforms, setPlatforms] = useState<Lookup[]>([]);
  const [params, setParams] = useState<GameSearchParams>({ sortBy: "title", direction: "asc", page: 0, size: PAGE_SIZE });
  const [result, setResult] = useState<PageResponse<GameSummary> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    gameService.listGenres().then(setGenres).catch(() => {});
    gameService.listPlatforms().then(setPlatforms).catch(() => {});
  }, []);

  useEffect(() => {
    setLoading(true);
    setError(null);
    gameService
      .listGames(params)
      .then(setResult)
      .catch(() => setError("Could not load the catalogue. Is the backend running?"))
      .finally(() => setLoading(false));
  }, [params]);

  function update(patch: Partial<GameSearchParams>) {
    // Any filter change resets to page 0 — otherwise "page 3 of a narrower result set" can
    // silently come back empty and look like a bug rather than just being past the end.
    setParams((p) => ({ ...p, ...patch, page: patch.page ?? 0 }));
  }

  return (
    <div style={styles.page}>
      <h1 style={{ marginBottom: 4 }}>Games</h1>
      <p style={styles.dim}>{result ? `${result.totalItems} game${result.totalItems === 1 ? "" : "s"}` : "\u00A0"}</p>

      <div style={styles.filters}>
        <input
          style={styles.input}
          placeholder="Search by title…"
          defaultValue={params.search ?? ""}
          onChange={(e) => update({ search: e.target.value || undefined })}
        />
        <select style={styles.input} value={params.genreId ?? ""} onChange={(e) => update({ genreId: e.target.value ? Number(e.target.value) : undefined })}>
          <option value="">All genres</option>
          {genres.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
        </select>
        <select style={styles.input} value={params.platformId ?? ""} onChange={(e) => update({ platformId: e.target.value ? Number(e.target.value) : undefined })}>
          <option value="">All platforms</option>
          {platforms.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
        </select>
        <label style={styles.checkboxLabel}>
          <input type="checkbox" checked={params.multiplayerOnly ?? false} onChange={(e) => update({ multiplayerOnly: e.target.checked })} />
          Multiplayer only
        </label>
        <select
          style={styles.input}
          value={`${params.sortBy}:${params.direction}`}
          onChange={(e) => {
            const [sortBy, direction] = e.target.value.split(":") as [GameSearchParams["sortBy"], GameSearchParams["direction"]];
            update({ sortBy, direction });
          }}
        >
          <option value="title:asc">Title A–Z</option>
          <option value="title:desc">Title Z–A</option>
          <option value="releaseDate:desc">Newest first</option>
          <option value="releaseDate:asc">Oldest first</option>
        </select>
      </div>

      {error && <p style={{ color: "#f87171" }}>{error}</p>}
      {loading && <p style={styles.dim}>Loading…</p>}

      {result && (
        <>
          <div style={styles.grid}>
            {result.items.map((g) => <GameCard key={g.id} game={g} />)}
          </div>
          {result.items.length === 0 && !loading && <p style={styles.dim}>No games match those filters.</p>}

          {result.totalPages > 1 && (
            <div style={styles.pagination}>
              <button style={styles.pageButton} disabled={result.page === 0} onClick={() => update({ page: result.page - 1 })}>
                Previous
              </button>
              <span style={styles.dim}>Page {result.page + 1} of {result.totalPages}</span>
              <button style={styles.pageButton} disabled={result.page >= result.totalPages - 1} onClick={() => update({ page: result.page + 1 })}>
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

function GameCard({ game }: { game: GameSummary }) {
  return (
    <Link to={`/games/${game.slug}`} style={styles.card}>
      {game.thumbnailUrl && <img src={game.thumbnailUrl} alt={game.title} style={styles.thumb} />}
      <div style={{ padding: 10 }}>
        <div style={{ fontWeight: 600 }}>{game.title}</div>
        <div style={styles.dim}>
          {game.genre ?? "Uncategorized"}
          {game.multiplayer && game.minPlayers && game.maxPlayers ? ` · ${game.minPlayers}-${game.maxPlayers} players` : ""}
        </div>
      </div>
    </Link>
  );
}

const styles: Record<string, React.CSSProperties> = {
  page: { maxWidth: 1000, margin: "0 auto", padding: 24, fontFamily: "sans-serif", color: "#e2e8f0" },
  dim: { color: "#94a3b8", fontSize: 13 },
  filters: { display: "flex", gap: 8, flexWrap: "wrap", margin: "16px 0" },
  input: { padding: "8px 10px", borderRadius: 8, border: "1px solid #334155", background: "#1e293b", color: "#e2e8f0" },
  checkboxLabel: { display: "flex", alignItems: "center", gap: 6, color: "#94a3b8", fontSize: 14 },
  grid: { display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))", gap: 14 },
  card: { borderRadius: 10, overflow: "hidden", background: "#1e293b", textDecoration: "none", color: "#e2e8f0", display: "block" },
  thumb: { width: "100%", aspectRatio: "16/9", objectFit: "cover", display: "block" },
  pagination: { display: "flex", gap: 12, alignItems: "center", justifyContent: "center", marginTop: 20 },
  pageButton: { padding: "6px 14px", borderRadius: 8, border: "1px solid #334155", background: "transparent", color: "#e2e8f0", cursor: "pointer" },
};
