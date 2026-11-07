import { api } from "./api";
import { FriendDto, NotificationDto, PresenceStatus } from "../types/social";

export const socialService = {
  // Friends
  getFriends: () =>
    api.get<FriendDto[]>("/friends").then((r) => r.data),

  getIncomingRequests: () =>
    api.get<FriendDto[]>("/friends/requests/incoming").then((r) => r.data),

  getOutgoingRequests: () =>
    api.get<FriendDto[]>("/friends/requests/outgoing").then((r) => r.data),

  sendFriendRequest: (username: string) =>
    api.post<FriendDto>("/friends/request", { username }).then((r) => r.data),

  acceptFriendRequest: (friendshipId: number) =>
    api.put<FriendDto>(`/friends/request/${friendshipId}/accept`).then((r) => r.data),

  declineFriendRequest: (friendshipId: number) =>
    api.put<void>(`/friends/request/${friendshipId}/decline`).then((r) => r.data),

  removeFriend: (friendUserId: number) =>
    api.delete<void>(`/friends/${friendUserId}`).then((r) => r.data),

  // Presence
  updatePresence: (status: PresenceStatus, currentGameSlug?: string, currentRoomCode?: string) =>
    api.put("/presence", { status, currentGameSlug, currentRoomCode }).then((r) => r.data),

  getPresence: (userId: number) =>
    api.get(`/presence/${userId}`).then((r) => r.data),

  // Notifications
  getNotifications: () =>
    api.get<NotificationDto[]>("/notifications").then((r) => r.data),

  getUnreadNotificationCount: () =>
    api.get<{ unreadCount: number }>("/notifications/unread-count").then((r) => r.data.unreadCount),

  markNotificationRead: (id: number) =>
    api.put<void>(`/notifications/${id}/read`).then((r) => r.data),

  markAllNotificationsRead: () =>
    api.put<void>("/notifications/read-all").then((r) => r.data),
};
