import { ProviderContractError } from "../errors.js";

const requiredString = (value, field) => {
  if (typeof value !== "string" || !value.trim()) throw new ProviderContractError(`Missing normalized field: ${field}`);
  return value.trim();
};
const optionalString = value => typeof value === "string" && value.trim() ? value.trim() : null;
const decision = value => ["granted", "denied", "unknown"].includes(value) ? value : "unknown";

export function accountSessionDto(input = {}) {
  return Object.freeze({
    provider: "rwa",
    accountId: requiredString(input.accountId, "accountId"),
    userId: requiredString(input.userId, "userId"),
    status: ["authenticated", "expired", "revoked", "unknown"].includes(input.status) ? input.status : "unknown",
    expiresAt: optionalString(input.expiresAt),
    tokenRef: optionalString(input.tokenRef)
  });
}
export function batchDto(input = {}) {
  return Object.freeze({ id: requiredString(input.id, "batch.id"), title: requiredString(input.title, "batch.title"), name: optionalString(input.name) || input.title });
}
export function courseDto(input = {}) {
  return Object.freeze({ id: requiredString(input.id, "course.id"), batchId: optionalString(input.batchId), title: requiredString(input.title, "course.title"), description: optionalString(input.description), visibility: input.visibility === "free" ? "free" : "paid" });
}
export function chapterDto(input = {}) {
  return Object.freeze({ id: requiredString(input.id, "chapter.id"), courseId: requiredString(input.courseId, "chapter.courseId"), title: requiredString(input.title, "chapter.title"), position: Number.isInteger(input.position) ? input.position : 0 });
}
export function lectureDto(input = {}) {
  return Object.freeze({ id: requiredString(input.id, "lecture.id"), chapterId: requiredString(input.chapterId, "lecture.chapterId"), title: requiredString(input.title, "lecture.title"), description: optionalString(input.description), type: input.type === "live" ? "live" : "recorded", visibility: input.visibility === "free" ? "free" : "paid" });
}
export function liveClassDto(input = {}) {
  return Object.freeze({ id: requiredString(input.id, "liveClass.id"), courseId: optionalString(input.courseId), title: requiredString(input.title, "liveClass.title"), startsAt: optionalString(input.startsAt), status: optionalString(input.status) || "unknown", visibility: input.visibility === "free" ? "free" : "paid" });
}
export function entitlementDto(input = {}) {
  return Object.freeze({ contentId: requiredString(input.contentId, "entitlement.contentId"), decision: decision(input.decision), source: optionalString(input.source) || "provider", checkedAt: optionalString(input.checkedAt), expiresAt: optionalString(input.expiresAt) });
}
export function playbackSessionDto(input = {}) {
  return Object.freeze({ id: optionalString(input.id), contentId: requiredString(input.contentId, "playback.contentId"), status: ["created", "unavailable", "expired", "denied", "mock_created"].includes(input.status) ? input.status : "unavailable", protocol: optionalString(input.protocol), playbackUrl: optionalString(input.playbackUrl), expiresAt: optionalString(input.expiresAt), providerAuthorization: input.providerAuthorization === true });
}
export function liveJoinSessionDto(input = {}) {
  return Object.freeze({ id: optionalString(input.id), liveClassId: requiredString(input.liveClassId, "liveJoin.liveClassId"), status: ["created", "unavailable", "expired", "denied", "mock_created"].includes(input.status) ? input.status : "unavailable", joinUrl: optionalString(input.joinUrl), expiresAt: optionalString(input.expiresAt), providerAuthorization: input.providerAuthorization === true });
}
