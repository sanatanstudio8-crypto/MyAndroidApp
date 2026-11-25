import {
  RwaIdentityProvider, RwaCatalogueProvider, RwaEntitlementProvider,
  RwaPlaybackProvider, LiveJoinProvider
} from "../contracts.js";
import {
  accountSessionDto, batchDto, courseDto, chapterDto, lectureDto,
  liveClassDto, entitlementDto, playbackSessionDto, liveJoinSessionDto
} from "./dtos.js";
import { ProviderNotConfiguredError, ProviderContractError } from "../errors.js";

/**
 * Provider-independent orchestration adapter.
 *
 * No HTTP paths, credentials, request/response field names, token format,
 * playback protocol, DRM scheme, or live-room protocol are assumed here.
 * Supply operation callbacks and normalizers only from approved provider docs.
 */
const REQUIRED_OPERATIONS = Object.freeze([
  "authenticate", "refreshSession", "validateSession", "revokeSession",
  "listBatches", "listCourses", "getCourse", "listChapters", "listLectures",
  "listLiveClasses", "checkAccountAccess", "checkCourseAccess",
  "checkLectureAccess", "getSubscriptionStatus", "getLectureDetails",
  "createPlaybackSession", "refreshPlaybackSession", "closePlaybackSession",
  "getLiveClassDetails", "createLiveJoinSession", "refreshLiveJoinSession",
  "leaveLiveSession"
]);

const REQUIRED_NORMALIZERS = Object.freeze([
  "accountSession", "batches", "courses", "course", "chapters", "lectures",
  "liveClasses", "entitlement", "subscriptionStatus", "lectureDetails",
  "playbackSession", "liveClassDetails", "liveJoinSession"
]);

const ensureFunction = (record, key, group) => {
  if (!record || typeof record[key] !== "function") {
    throw new ProviderNotConfiguredError(`original-rwa:${group}.${key}`);
  }
};

const mapList = (value, mapper, field) => {
  const items = mapper(value);
  if (!Array.isArray(items)) throw new ProviderContractError(`Normalizer ${field} must return an array.`);
  return items;
};

export class ProviderIndependentRwaAdapter extends
  RwaIdentityProvider {
  constructor({ operations = {}, normalizers = {} } = {}) {
    super();
    this.operations = Object.freeze({ ...operations });
    this.normalizers = Object.freeze({ ...normalizers });
    this.catalogue = new RwaCatalogueAdapter(this.operations, this.normalizers);
    this.entitlement = new RwaEntitlementAdapter(this.operations, this.normalizers);
    this.playback = new RwaPlaybackAdapter(this.operations, this.normalizers);
    this.liveJoin = new RwaLiveJoinAdapter(this.operations, this.normalizers);
  }

  toProviderBundle({ enabled = false } = {}) {
    return Object.freeze({
      enabled: enabled === true && this.configurationStatus.ready,
      identity: this,
      catalogue: this.catalogue,
      entitlement: this.entitlement,
      playback: this.playback,
      liveJoin: this.liveJoin,
      configurationStatus: this.configurationStatus
    });
  }

  get configurationStatus() {
    return Object.freeze({
      configuredOperations: REQUIRED_OPERATIONS.filter(key => typeof this.operations[key] === "function"),
      missingOperations: REQUIRED_OPERATIONS.filter(key => typeof this.operations[key] !== "function"),
      configuredNormalizers: REQUIRED_NORMALIZERS.filter(key => typeof this.normalizers[key] === "function"),
      missingNormalizers: REQUIRED_NORMALIZERS.filter(key => typeof this.normalizers[key] !== "function"),
      ready: REQUIRED_OPERATIONS.every(key => typeof this.operations[key] === "function") &&
        REQUIRED_NORMALIZERS.every(key => typeof this.normalizers[key] === "function")
    });
  }

  async #call(operation, input) {
    ensureFunction(this.operations, operation, "operation");
    return this.operations[operation](input);
  }

  async #normalize(name, value, fallback) {
    ensureFunction(this.normalizers, name, "normalizer");
    const normalized = await this.normalizers[name](value);
    return fallback ? fallback(normalized) : normalized;
  }

  async authenticate(input) {
    return accountSessionDto(await this.#normalize("accountSession", await this.#call("authenticate", input)));
  }
  async refreshSession(userId) {
    return accountSessionDto(await this.#normalize("accountSession", await this.#call("refreshSession", { userId })));
  }
  async validateSession(userId) {
    return accountSessionDto(await this.#normalize("accountSession", await this.#call("validateSession", { userId })));
  }
  async revokeSession(userId) {
    return this.#call("revokeSession", { userId });
  }
}

class RwaCatalogueAdapter extends RwaCatalogueProvider {
  constructor(operations, normalizers) { super(); this.operations = operations; this.normalizers = normalizers; }
  async #list(op, mapper, dto) {
    ensureFunction(this.operations, op, "operation");
    ensureFunction(this.normalizers, mapper, "normalizer");
    const raw = await this.operations[op](arguments[2]);
    return mapList(raw, this.normalizers[mapper], mapper).map(dto);
  }
  async listBatches() {
    ensureFunction(this.operations, "listBatches", "operation");
    ensureFunction(this.normalizers, "batches", "normalizer");
    const raw = await this.operations.listBatches();
    return mapList(raw, this.normalizers.batches, "batches").map(batchDto);
  }
  async listCourses(batchId) {
    const raw = await this.operations.listCourses({ batchId });
    ensureFunction(this.normalizers, "courses", "normalizer");
    return mapList(raw, this.normalizers.courses, "courses").map(courseDto);
  }
  async getCourse(courseId) {
    ensureFunction(this.operations, "getCourse", "operation");
    ensureFunction(this.normalizers, "course", "normalizer");
    const raw = await this.operations.getCourse({ courseId });
    return raw == null ? null : courseDto(await this.normalizers.course(raw));
  }
  async listChapters(courseId) {
    const raw = await this.operations.listChapters({ courseId });
    ensureFunction(this.normalizers, "chapters", "normalizer");
    return mapList(raw, this.normalizers.chapters, "chapters").map(chapterDto);
  }
  async listLectures(chapterId, type) {
    const raw = await this.operations.listLectures({ chapterId, type });
    ensureFunction(this.normalizers, "lectures", "normalizer");
    return mapList(raw, this.normalizers.lectures, "lectures").map(lectureDto);
  }
  async listLiveClasses() {
    const raw = await this.operations.listLiveClasses();
    ensureFunction(this.normalizers, "liveClasses", "normalizer");
    return mapList(raw, this.normalizers.liveClasses, "liveClasses").map(liveClassDto);
  }
}

class RwaEntitlementAdapter extends RwaEntitlementProvider {
  constructor(operations, normalizers) { super(); this.operations = operations; this.normalizers = normalizers; }
  async #entitlement(operation, input) {
    ensureFunction(this.operations, operation, "operation");
    ensureFunction(this.normalizers, "entitlement", "normalizer");
    return entitlementDto(await this.normalizers.entitlement(await this.operations[operation](input)));
  }
  checkAccountAccess(userId) { return this.#entitlement("checkAccountAccess", { userId }); }
  checkCourseAccess(userId, courseId) { return this.#entitlement("checkCourseAccess", { userId, courseId }); }
  checkLectureAccess(userId, lectureId) { return this.#entitlement("checkLectureAccess", { userId, lectureId }); }
  async getSubscriptionStatus(userId) {
    ensureFunction(this.operations, "getSubscriptionStatus", "operation");
    ensureFunction(this.normalizers, "subscriptionStatus", "normalizer");
    return this.normalizers.subscriptionStatus(await this.operations.getSubscriptionStatus({ userId }));
  }
}

class RwaPlaybackAdapter extends RwaPlaybackProvider {
  constructor(operations, normalizers) { super(); this.operations = operations; this.normalizers = normalizers; }
  async getLectureDetails(contentId) {
    ensureFunction(this.operations, "getLectureDetails", "operation");
    ensureFunction(this.normalizers, "lectureDetails", "normalizer");
    return this.normalizers.lectureDetails(await this.operations.getLectureDetails({ contentId }));
  }
  async #session(operation, userId, contentId) {
    ensureFunction(this.operations, operation, "operation");
    ensureFunction(this.normalizers, "playbackSession", "normalizer");
    return playbackSessionDto(await this.normalizers.playbackSession(
      await this.operations[operation]({ userId, contentId })
    ));
  }
  createSession(userId, contentId) { return this.#session("createPlaybackSession", userId, contentId); }
  refreshSession(userId, contentId) { return this.#session("refreshPlaybackSession", userId, contentId); }
  closeSession(userId, contentId) {
    ensureFunction(this.operations, "closePlaybackSession", "operation");
    return this.operations.closePlaybackSession({ userId, contentId });
  }
}

class RwaLiveJoinAdapter extends LiveJoinProvider {
  constructor(operations, normalizers) { super(); this.operations = operations; this.normalizers = normalizers; }
  async getLiveClassDetails(liveClassId) {
    ensureFunction(this.operations, "getLiveClassDetails", "operation");
    ensureFunction(this.normalizers, "liveClassDetails", "normalizer");
    return this.normalizers.liveClassDetails(await this.operations.getLiveClassDetails({ liveClassId }));
  }
  async #session(operation, userId, liveClassId) {
    ensureFunction(this.operations, operation, "operation");
    ensureFunction(this.normalizers, "liveJoinSession", "normalizer");
    return liveJoinSessionDto(await this.normalizers.liveJoinSession(
      await this.operations[operation]({ userId, liveClassId })
    ));
  }
  createJoinSession(userId, liveClassId) { return this.#session("createLiveJoinSession", userId, liveClassId); }
  refreshJoinSession(userId, liveClassId) { return this.#session("refreshLiveJoinSession", userId, liveClassId); }
  leaveSession(userId, liveClassId) {
    ensureFunction(this.operations, "leaveLiveSession", "operation");
    return this.operations.leaveLiveSession({ userId, liveClassId });
  }
}

/** Copy this shape into a private integration module and fill from official docs. */
export const ORIGINAL_RWA_INTEGRATION_PLACEHOLDERS = Object.freeze({
  authorizationReference: "REQUIRED: written provider authorization reference",
  documentationVersion: "REQUIRED: official API/SDK documentation version and URL",
  sandboxReference: "REQUIRED: provider-issued sandbox and test-account details",
  operations: "REQUIRED: map each operation to the documented transport call",
  normalizers: "REQUIRED: map official response payloads to the normalized DTO input fields",
  drm: "REQUIRED: official DRM scheme, license request contract, headers and renewal rules",
  live: "REQUIRED: official live-room protocol, join credentials, refresh and leave contract"
});
