export type PresenceStatus = "ONLINE" | "AWAY" | "IN_GAME" | "IN_LOBBY" | "OFFLINE";

export interface FriendDto {
  friendshipId: number;
  friendUserId: number;
  username: string;
  avatarUrl: string | null;
  status: "PENDING" | "ACCEPTED" | "DECLINED" | "BLOCKED";
  presenceStatus: PresenceStatus;
  currentGameSlug: string | null;
  currentRoomCode: string | null;
  memberSince: string;
}

export type NotificationType = "FRIEND_REQUEST" | "FRIEND_ACCEPT" | "PARTY_INVITE" | "SYSTEM";

export interface NotificationDto {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  link: string | null;
  read: boolean;
  createdAt: string;
}
