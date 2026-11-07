export interface PartyMemberDto {
  userId: number;
  username: string;
  avatarUrl?: string;
  isLeader: boolean;
  joinedAt: string;
}

export interface PartyDto {
  id: number;
  partyCode: string;
  leaderUserId: number;
  leaderUsername: string;
  maxMembers: number;
  members: PartyMemberDto[];
  createdAt: string;
}
