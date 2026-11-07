import { Navigate, Route, Routes } from "react-router-dom";
import { ProtectedLayout } from "./routes/ProtectedLayout";
import { LoginPage } from "./pages/auth/LoginPage";
import { RegisterPage } from "./pages/auth/RegisterPage";
import { HomePage } from "./pages/home/HomePage";
import { GameCatalogPage } from "./pages/catalog/GameCatalogPage";
import { GameDetailPage } from "./pages/catalog/GameDetailPage";
import { MultiplayerArenaPage } from "./pages/multiplayer/MultiplayerArenaPage";
import { LibraryPage } from "./pages/library/LibraryPage";
import { WishlistPage } from "./pages/wishlist/WishlistPage";
import { NewsPage } from "./pages/news/NewsPage";
import { ProfilePage } from "./pages/profile/ProfilePage";
import { AdminDashboardPage } from "./pages/admin/AdminDashboardPage";
import { ExperimentComparePage } from "./pages/network-lab/ExperimentComparePage";
import AchievementsPage from "./pages/achievements/AchievementsPage";
import LeaderboardPage from "./pages/leaderboard/LeaderboardPage";
import { GroupsPage } from "./pages/social/GroupsPage";

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<ProtectedLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/games" element={<GameCatalogPage />} />
        <Route path="/games/:slug" element={<GameDetailPage />} />
        <Route path="/play/arena" element={<MultiplayerArenaPage />} />
        <Route path="/library" element={<LibraryPage />} />
        <Route path="/wishlist" element={<WishlistPage />} />
        <Route path="/news" element={<NewsPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/achievements" element={<AchievementsPage />} />
        <Route path="/leaderboard" element={<LeaderboardPage />} />
        <Route path="/groups" element={<GroupsPage />} />
        <Route path="/admin" element={<AdminDashboardPage />} />
        <Route path="/experiments" element={<ExperimentComparePage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
