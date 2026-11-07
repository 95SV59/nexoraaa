import { useState, useEffect } from 'react';
import { getMyAchievements } from '../../services/progressionService';
import { getMyProgression } from '../../services/progressionService';
import type { AchievementDto, UserProgressionDto } from '../../types/progression';
import { RARITY_COLORS } from '../../types/progression';

const BG = '#060913';
const CARD_BG = '#0d1627';
const BORDER = '#1a2a4a';
const CYAN = '#00f2fe';
const GREEN = '#00ff87';
const AMBER = '#f59e0b';
const TEXT_DIM = '#7a8ba8';

const GAME_TABS = ['ALL', 'ARENA', 'RACERS', 'FOOTBALL', 'TAG', 'PLATFORM'];

function getRarityGlow(rarity: string): string {
  const c = RARITY_COLORS[rarity] ?? '#64748b';
  return `0 0 12px ${c}55`;
}

function RarityBadge({ rarity }: { rarity: string }) {
  const color = RARITY_COLORS[rarity] ?? '#64748b';
  return (
    <span style={{
      fontSize: 10, fontWeight: 700, letterSpacing: '0.08em',
      padding: '2px 8px', borderRadius: 4,
      border: `1px solid ${color}`,
      color, background: `${color}18`,
      textTransform: 'uppercase',
    }}>{rarity}</span>
  );
}

function AchievementCard({ ach }: { ach: AchievementDto }) {
  const color = RARITY_COLORS[ach.rarityTier] ?? '#64748b';
  const pct = Math.min(100, Math.round((ach.progress / Math.max(1, ach.criteriaThreshold)) * 100));

  return (
    <div style={{
      background: CARD_BG,
      border: `1px solid ${ach.completed ? color : BORDER}`,
      borderRadius: 12,
      padding: '18px 20px',
      display: 'flex',
      gap: 16,
      alignItems: 'flex-start',
      boxShadow: ach.completed ? getRarityGlow(ach.rarityTier) : undefined,
      opacity: ach.completed ? 1 : 0.75,
      transition: 'all 0.2s ease',
      position: 'relative',
      overflow: 'hidden',
    }}>
      {/* Badge icon */}
      <div style={{
        width: 56, height: 56, flexShrink: 0,
        background: ach.completed ? `${color}25` : '#1a2a4a',
        borderRadius: 10, display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 28, border: `1px solid ${ach.completed ? color : BORDER}`,
      }}>
        {ach.badgeIcon || '🏆'}
      </div>

      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap', marginBottom: 4 }}>
          <span style={{ fontSize: 15, fontWeight: 700, color: ach.completed ? '#fff' : '#8fa0bc' }}>
            {ach.title}
          </span>
          <RarityBadge rarity={ach.rarityTier} />
          {ach.completed && (
            <span style={{
              fontSize: 10, fontWeight: 700, padding: '2px 8px', borderRadius: 4,
              background: `${GREEN}25`, color: GREEN, border: `1px solid ${GREEN}`,
              textTransform: 'uppercase', letterSpacing: '0.08em',
            }}>UNLOCKED</span>
          )}
        </div>

        <p style={{ fontSize: 13, color: TEXT_DIM, margin: '0 0 10px' }}>{ach.description}</p>

        {/* Progress bar */}
        <div style={{ marginBottom: 6 }}>
          <div style={{
            height: 6, background: '#1a2a4a', borderRadius: 3, overflow: 'hidden',
          }}>
            <div style={{
              width: `${pct}%`, height: '100%',
              background: ach.completed ? `linear-gradient(90deg, ${color}, ${color}aa)` : CYAN,
              borderRadius: 3, transition: 'width 0.5s ease',
            }} />
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 4 }}>
            <span style={{ fontSize: 11, color: TEXT_DIM }}>
              {ach.progress} / {ach.criteriaThreshold}
            </span>
            {ach.unlockedAt && (
              <span style={{ fontSize: 11, color: TEXT_DIM }}>
                Unlocked {new Date(ach.unlockedAt).toLocaleDateString()}
              </span>
            )}
          </div>
        </div>

        <span style={{ fontSize: 12, color: AMBER, fontWeight: 600 }}>+{ach.xpReward} XP</span>
      </div>
    </div>
  );
}

export default function AchievementsPage() {
  const [achievements, setAchievements] = useState<AchievementDto[]>([]);
  const [progression, setProgression] = useState<UserProgressionDto | null>(null);
  const [activeTab, setActiveTab] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    const game = activeTab === 'ALL' ? undefined : activeTab.toLowerCase();
    Promise.all([
      getMyAchievements(game),
      getMyProgression().catch(() => null),
    ])
      .then(([achs, prog]) => {
        setAchievements(achs);
        if (prog) setProgression(prog);
      })
      .catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, [activeTab]);

  const completed = achievements.filter(a => a.completed).length;
  const total = achievements.length;

  return (
    <div style={{ minHeight: '100vh', background: BG, padding: '32px 24px', fontFamily: 'Inter, sans-serif' }}>
      <div style={{ maxWidth: 900, margin: '0 auto' }}>

        {/* Header */}
        <div style={{ marginBottom: 32 }}>
          <h1 style={{
            fontSize: 32, fontWeight: 800, margin: '0 0 8px',
            background: `linear-gradient(90deg, ${CYAN}, ${GREEN})`,
            WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
          }}>Achievements</h1>
          <p style={{ color: TEXT_DIM, fontSize: 14, margin: 0 }}>
            Track your milestones and unlock rewards across all NEXORA games
          </p>
        </div>

        {/* Stats bar */}
        {progression && (
          <div style={{
            background: CARD_BG, border: `1px solid ${BORDER}`, borderRadius: 12,
            padding: '20px 24px', marginBottom: 24,
            display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: 16,
          }}>
            <div>
              <div style={{ fontSize: 24, fontWeight: 800, color: CYAN }}>{completed}/{total}</div>
              <div style={{ fontSize: 12, color: TEXT_DIM }}>Achievements Unlocked</div>
            </div>
            <div>
              <div style={{ fontSize: 24, fontWeight: 800, color: GREEN }}>
                {total > 0 ? Math.round((completed / total) * 100) : 0}%
              </div>
              <div style={{ fontSize: 12, color: TEXT_DIM }}>Completion Rate</div>
            </div>
            <div>
              <div style={{ fontSize: 24, fontWeight: 800, color: AMBER }}>
                Lv.{progression.level}
              </div>
              <div style={{ fontSize: 12, color: TEXT_DIM }}>{progression.rankTitle}</div>
            </div>
            <div>
              <div style={{ fontSize: 24, fontWeight: 800, color: '#fff' }}>
                {progression.totalXp.toLocaleString()}
              </div>
              <div style={{ fontSize: 12, color: TEXT_DIM }}>Total XP Earned</div>
            </div>
          </div>
        )}

        {/* Filter tabs */}
        <div style={{ display: 'flex', gap: 8, marginBottom: 24, flexWrap: 'wrap' }}>
          {GAME_TABS.map(tab => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              style={{
                padding: '8px 18px', borderRadius: 8, fontSize: 13, fontWeight: 600,
                cursor: 'pointer', transition: 'all 0.2s',
                background: activeTab === tab ? `${CYAN}22` : 'transparent',
                color: activeTab === tab ? CYAN : TEXT_DIM,
                border: `1px solid ${activeTab === tab ? CYAN : BORDER}`,
              }}
            >
              {tab}
            </button>
          ))}
        </div>

        {/* Content */}
        {loading ? (
          <div style={{ textAlign: 'center', color: TEXT_DIM, padding: '60px 0' }}>
            <div style={{ fontSize: 32, marginBottom: 12 }}>⚡</div>
            Loading achievements...
          </div>
        ) : error ? (
          <div style={{
            background: '#1a0a0a', border: '1px solid #ff4444', borderRadius: 12,
            padding: 24, color: '#ff6666', textAlign: 'center',
          }}>
            Failed to load achievements: {error}
          </div>
        ) : achievements.length === 0 ? (
          <div style={{ textAlign: 'center', color: TEXT_DIM, padding: '60px 0' }}>
            <div style={{ fontSize: 48, marginBottom: 12 }}>🏅</div>
            <p>No achievements found for this category.</p>
          </div>
        ) : (
          <div style={{ display: 'grid', gap: 12 }}>
            {achievements.map(ach => (
              <AchievementCard key={ach.id} ach={ach} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
