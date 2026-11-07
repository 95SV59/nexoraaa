export interface GroupMemberDto {
  userId: number;
  username: string;
  avatarUrl: string | null;
  role: "LEADER" | "OFFICER" | "MEMBER";
  joinedAt: string;
}

export interface GroupDto {
  id: number;
  name: string;
  tag: string;
  description: string;
  avatarUrl: string;
  leaderUserId: number;
  leaderUsername: string;
  memberCount: number;
  isMember: boolean;
  myRole: "LEADER" | "OFFICER" | "MEMBER" | null;
  createdAt: string;
  members?: GroupMemberDto[];
}

export interface CreateGroupRequest {
  name: string;
  tag: string;
  description?: string;
  avatarUrl?: string;
}
