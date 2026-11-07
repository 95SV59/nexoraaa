import { api } from "./api";
import type { CreateGroupRequest, GroupDto } from "../types/group";

export const groupService = {
  createGroup: (req: CreateGroupRequest) =>
    api.post<GroupDto>("/groups", req).then((r) => r.data),

  getAllGroups: () =>
    api.get<GroupDto[]>("/groups").then((r) => r.data),

  getMyGroups: () =>
    api.get<GroupDto[]>("/groups/my").then((r) => r.data),

  getGroup: (id: number) =>
    api.get<GroupDto>(`/groups/${id}`).then((r) => r.data),

  joinGroup: (id: number) =>
    api.post<GroupDto>(`/groups/${id}/join`).then((r) => r.data),

  leaveGroup: (id: number) =>
    api.post<void>(`/groups/${id}/leave`),
};
