import { apiGet } from "./client";
import type { components } from "./schema.gen";

export type Me = components["schemas"]["Me"];
export type ClubMembership = components["schemas"]["ClubMembership"];

export function getMe() {
  return apiGet<Me>("/me");
}
