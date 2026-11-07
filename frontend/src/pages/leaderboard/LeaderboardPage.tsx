import { useState, useEffect } from 'react';
import { getLeaderboard, getFriendsLeaderboard } from '../../services/progressionService';
import type { LeaderboardEntryDto } from '../../types/progression';
import { RANK_TIER_COLORS } from '../../types/progression';
import { useAuth } from '../../context/AuthContext';

const BG = '#060913';
const CARD_BG = '#0d1627';
const BORDER = '#1a2a4a';
const CYAN = '#00f2fe';
const GREEN = '#00ff87';
const AMBER = '#f59e0b';
const TEXT_DIM = '#7a8ba8';

const GAMES = ['arena', 'racers', 'football', 'tag'];
const GAME_LABELS: Record<string, string> = {
  arena: '⚔️ Arena',
  racers: '🏎️ Racers',
  football: '⚽ Football',
  tag: '🏃 Tag',
};

function TierBadge({ tier }: { tier: string }) {
  const color = RANK_TIER_COLORS[tier] ?? '#64748b';
  return (
    <span style={{
      fontSize: 10, fontWeight: 800, padding: '3px 8px', borderRadius: 4,
      border: `1px solid ${color}`, color,
      background: `${color}18`, letterSpacing: '0.08em', textTransform: 'uppercase',
    }}>{tier}</span>
  );
}

function PodiumCard({ entry, pos }: { entry: LeaderboardEntryDto; pos: 1 | 2 | 3 }) {
  const colorMap = { 1: '#ffd700', 2: '#c0c0c0', 3: '#cd7f32' };
  const sizeMap = { 1: 72, 2: 60, 3: 60 };
  const color = colorMap[pos];
  const size = sizeMap[pos];

  return (
    <div style={{
      flex: 1, minWidth: 140,
      background: CARD_BG, border: `1px solid ${color}55`,
      borderRadius: 16, padding: '20px 16px',
      display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8,
      boxShadow: `0 0 20px ${color}22`,
      order: pos === 1 ? 0 : pos === 2 ? -1 : 1,
    }}>
      <div style={{
        width: size, height: size, borderRadius: '50%',
        background: `linear-gradient(135deg, ${color}44, ${color}22)`,
        border: `2px solid ${color}`, display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: pos === 1 ? 32 : 26,
      }}>
        {entry.avatarUrl
          ? <img src={entry.avatarUrl} alt="" style={{ width: '100%', height: '100%', borderRadius: '50%' }} />
          : ['🥇', '🥈', '🥉'][pos - 1]}
      </div>
      <div style={{ fontSize: 13, fontWeight: 700, color: '#fff', textAlign: 'center' }}>
        {entry.username}
      </div>
      <div style={{ fontSize: 20, fontWeight: 800, color }}>
        {entry.score.toLocaleString()}
      </div>
      <TierBadge tier={entry.rankTier} />
      <div style={{ fontSize: 11, color: TEXT_DIM }}>
        {entry.wins}W / {entry.matchesPlayed}M · {entry.winRate}%
      </div>
    </div>
  );
}

export default function LeaderboardPage() {
  const { user } = useAuth();
  const [game, setGame] = useState('arena');
  const [friendsMode, setFriendsMode] = useState(false);
  const [entries, setEntries] = useState<LeaderboardEntryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    const fetch = friendsMode && user
      ? getFriendsLeaderboard(game)
      : getLeaderboard(game, 1, 50);

    fetch
      .then(setEntries)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, [game, friendsMode, user]);

  const top3 = entries.slice(0, 3);
  const rest = entries.slice(3);

  return (
    <div style={{ minHeight: '100vh', background: BG, padding: '32px 24px', fontFamily: 'Inter, sans-serif' }}>
      <div style={{ maxWidth: 900, margin: '0 auto' }}>

        {/* Header */}
        <div style={{ marginBottom: 32 }}>
          <h1 style={{
            fontSize: 32, fontWeight: 800, margin: '0 0 8px',
            background: `linear-gradient(90deg, ${AMBER}, ${CYAN})`,
            WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
          }}>Leaderboards</h1>
          <p style={{ color: TEXT_DIM, fontSize: 14, margin: 0 }}>
            Season 1 rankings — compete, rank up, and claim the top spot
          </p>
        </div>

        {/* Controls */}
        <div style={{
          display: 'flex', gap: 12, marginBottom: 28, flexWrap: 'wrap', alignItems: 'center',
        }}>
          {/* Game selector */}
          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
            {GAMES.map(g => (
              <button
                key={g}
                onClick={() => setGame(g)}
                style={{
                  padding: '8px 16px', borderRadius: 8, fontSize: 13, fontWeight: 600,
                  cursor: 'pointer', transition: 'all 0.2s',
                  background: game === g ? `${CYAN}22` : 'transparent',
                  color: game === g ? CYAN : TEXT_DIM,
                  border: `1px solid ${game === g ? CYAN : BORDER}`,
                }}
              >
                {GAME_LABELS[g]}
              </button>
            ))}
          </div>

          {/* Global / Friends toggle */}
          {user && (
            <div style={{
              display: 'flex', borderRadius: 8, overflow: 'hidden',
              border: `1px solid ${BORDER}`, marginLeft: 'auto',
            }}>
              {[false, true].map(isFriends => (
                <button
                  key={String(isFriends)}
                  onClick={() => setFriendsMode(isFriends)}
                  style={{
                    padding: '8px 18px', fontSize: 13, fontWeight: 600, cursor: 'pointer',
                    border: 'none', transition: 'all 0.2s',
                    background: friendsMode === isFriends ? CYAN : 'transparent',
                    color: friendsMode === isFriends ? '#060913' : TEXT_DIM,
                  }}
                >
                  {isFriends ? '👥 Friends' : '🌍 Global'}
                </button>
              ))}
            </div>
          )}
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', color: TEXT_DIM, padding: '60px 0' }}>
            <div style={{ fontSize: 32, marginBottom: 12 }}>⚡</div>
            Loading leaderboard...
          </div>
        ) : error ? (
          <div style={{
            background: '#1a0a0a', border: '1px solid #ff4444', borderRadius: 12,
            padding: 24, color: '#ff6666', textAlign: 'center',
          }}>
            Failed to load leaderboard: {error}
          </div>
        ) : entries.length === 0 ? (
          <div style={{ textAlign: 'center', color: TEXT_DIM, padding: '60px 0' }}>
            <div style={{ fontSize: 48, marginBottom: 12 }}>🏆</div>
            <p>No rankings yet — be the first to play!</p>
          </div>
        ) : (
          <>
            {/* Podium top 3 */}
            {top3.length >= 1 && (
              <div style={{
                display: 'flex', gap: 12, marginBottom: 28, alignItems: 'flex-end',
                justifyContent: 'center',
              }}>
                {top3.map((e, i) => (
                  <PodiumCard key={e.userId} entry={e} pos={(i + 1) as 1 | 2 | 3} />
                ))}
              </div>
            )}

            {/* Rank table */}
            {rest.length > 0 && (
              <div style={{
                background: CARD_BG, border: `1px solid ${BORDER}`, borderRadius: 12,
                overflow: 'hidden',
              }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
                  <thead>
                    <tr style={{ background: '#0a1322' }}>
                      {['Rank', 'Player', 'Score', 'Tier', 'Matches', 'Wins', 'Win%'].map(h => (
                        <th key={h} style={{
                          padding: '12px 16px', textAlign: 'left',
                          color: TEXT_DIM, fontWeight: 600, letterSpacing: '0.05em',
                          borderBottom: `1px solid ${BORDER}`,
                        }}>{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {rest.map((e) => {
                      const isMe = user && e.userId === user.userId;
                      return (
                        <tr key={e.userId} style={{
                          borderBottom: `1px solid ${BORDER}`,
                          background: isMe ? `${CYAN}0d` : 'transparent',
                          transition: 'background 0.15s',
                        }}>
                          <td style={{ padding: '12px 16px', color: TEXT_DIM, fontWeight: 700 }}>
                            #{e.rank}
                          </td>
                          <td style={{ padding: '12px 16px' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                              <div style={{
                                width: 32, height: 32, borderRadius: '50%',
                                background: '#1a2a4a', display: 'flex',
                                alignItems: 'center', justifyContent: 'center', fontSize: 14,
                                overflow: 'hidden', flexShrink: 0,
                              }}>
                                {e.avatarUrl
                                  ? <img src={e.avatarUrl} alt="" style={{ width: '100%', height: '100%', borderRadius: '50%' }} />
                                  : '👤'}
                              </div>
                              <span style={{ fontWeight: 600, color: isMe ? CYAN : '#d4e4f7' }}>
                                {e.username}
                                {isMe && <span style={{ marginLeft: 6, fontSize: 10, color: CYAN }}>(You)</span>}
                              </span>
                            </div>
                          </td>
                          <td style={{ padding: '12px 16px', fontWeight: 700, color: AMBER }}>
                            {e.score.toLocaleString()}
                          </td>
                          <td style={{ padding: '12px 16px' }}>
                            <TierBadge tier={e.rankTier} />
                          </td>
                          <td style={{ padding: '12px 16px', color: TEXT_DIM }}>{e.matchesPlayed}</td>
                          <td style={{ padding: '12px 16px', color: GREEN }}>{e.wins}</td>
                          <td style={{ padding: '12px 16px', color: TEXT_DIM }}>{e.winRate}%</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
