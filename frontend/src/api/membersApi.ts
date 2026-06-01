import { apiDelete, apiGet, apiPost } from "./client";

export type ClubMembershipRole = "MANAGER" | "STAFF";

export interface ClubMember {
  userId: string;
  username: string | null;
  email: string | null;
  firstName: string | null;
  lastName: string | null;
  role: ClubMembershipRole;
}

export interface ClubMembers {
  managers: ClubMember[];
  staff: ClubMember[];
}

export interface AddMemberRequest {
  /** Existing user to attach. Omit to create a new user inline (managers). */
  userId?: string;
  role: ClubMembershipRole;
  /** Inline-create fields (used when no `userId` is given). */
  username?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  password?: string;
}

export function listMembers(clubId: number) {
  return apiGet<ClubMembers>(`/clubs/${clubId}/members`);
}

export function addMember(clubId: number, body: AddMemberRequest) {
  return apiPost<AddMemberRequest, void>(`/clubs/${clubId}/members`, body);
}

export async function removeMember(clubId: number, userId: string, role: ClubMembershipRole) {
  // The DELETE here needs a query param (role); apiDelete doesn't take a body
  // but the URL itself carries it.
  await apiDelete(`/clubs/${clubId}/members/${encodeURIComponent(userId)}?role=${role}`);
}
