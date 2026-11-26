import { ProviderAuthenticationError, ProviderAuthorizationError, ProviderNotConfiguredError } from "../errors.js";
import { RwaIdentityProvider, RwaCatalogueProvider, RwaEntitlementProvider, RwaPlaybackProvider, LiveJoinProvider } from "../contracts.js";
import { accountSessionDto, batchDto, courseDto, chapterDto, lectureDto, liveClassDto, entitlementDto, playbackSessionDto, liveJoinSessionDto } from "../rwa/dtos.js";

const copy = value => structuredClone(value);
const key = (userId, contentId) => `${userId}:${contentId}`;

export class MockRwaIdentityProvider extends RwaIdentityProvider {
  constructor({ accounts = [{ email: "student@example.test", password: "mock-password", accountId: "mock-account-1", userId: "mock-user-1" }] } = {}) {
    super();
    this.accounts = accounts;
    this.sessions = new Map();
  }
  async authenticate({ email, password } = {}) {
    const account = this.accounts.find(x => x.email === email && x.password === password);
    if (!account) throw new ProviderAuthenticationError("Mock provider credentials were rejected.", { provider: "rwa-mock" });
    const session = accountSessionDto({ accountId: account.accountId, userId: account.userId, status: "authenticated", tokenRef: `mock-session:${account.accountId}`, expiresAt: new Date(Date.now() + 1800000).toISOString() });
    this.sessions.set(account.userId, session);
    return session;
  }
  async refreshSession(userId) {
    const old = this.sessions.get(userId);
    if (!old || old.status !== "authenticated") throw new ProviderAuthenticationError("No active mock provider session.", { provider: "rwa-mock" });
    const refreshed = accountSessionDto({ ...old, expiresAt: new Date(Date.now() + 1800000).toISOString() });
    this.sessions.set(userId, refreshed);
    return refreshed;
  }
  async validateSession(userId) {
    const session = this.sessions.get(userId);
    return Boolean(session && session.status === "authenticated" && Date.parse(session.expiresAt) > Date.now());
  }
  async revokeSession(userId) {
    const old = this.sessions.get(userId);
    if (!old) return false;
    this.sessions.set(userId, accountSessionDto({ ...old, status: "revoked", tokenRef: null }));
    return true;
  }
}

export class MockRwaCatalogueProvider extends RwaCatalogueProvider {
  constructor(fixtures = {}) {
    super();
    this.data = {
      batches: fixtures.batches || [], courses: fixtures.courses || [], chapters: fixtures.chapters || [],
      lectures: fixtures.lectures || [], liveClasses: fixtures.liveClasses || []
    };
  }
  async listBatches() { return this.data.batches.map(batchDto); }
  async listCourses(batchId = null) { return this.data.courses.filter(x => !batchId || x.batchId === batchId).map(courseDto); }
  async getCourse(courseId) { const row = this.data.courses.find(x => x.id === courseId); return row ? courseDto(row) : null; }
  async listChapters(courseId) { return this.data.chapters.filter(x => x.courseId === courseId).map(chapterDto); }
  async listLectures(chapterId, type = null) { return this.data.lectures.filter(x => x.chapterId === chapterId && (!type || x.type === type)).map(lectureDto); }
  async listLiveClasses() { return this.data.liveClasses.map(liveClassDto); }
}

export class MockRwaEntitlementProvider extends RwaEntitlementProvider {
  constructor({ accountAccess = {}, courseAccess = {}, lectureAccess = {}, subscriptions = {} } = {}) {
    super();
    this.accountAccess = accountAccess; this.courseAccess = courseAccess;
    this.lectureAccess = lectureAccess; this.subscriptions = subscriptions;
  }
  async checkAccountAccess(userId) { return entitlementDto({ contentId: `account:${userId}`, decision: this.accountAccess[userId] || "unknown", source: "mock" }); }
  async checkCourseAccess(userId, courseId) { return entitlementDto({ contentId: courseId, decision: this.courseAccess[key(userId, courseId)] || "unknown", source: "mock" }); }
  async checkLectureAccess(userId, lectureId) { return entitlementDto({ contentId: lectureId, decision: this.lectureAccess[key(userId, lectureId)] || "unknown", source: "mock" }); }
  async getSubscriptionStatus(userId) { return copy(this.subscriptions[userId] || { status: "unknown", source: "mock" }); }
  async check(userId, contentId) { return this.checkLectureAccess(userId, contentId); }
}

export class MockRwaPlaybackProvider extends RwaPlaybackProvider {
  constructor({ entitlementProvider, sessions = new Map() } = {}) { super(); this.entitlementProvider = entitlementProvider; this.sessions = sessions; }
  async getLectureDetails(contentId) { return { contentId, status: "mock_metadata_only", providerAuthorization: false }; }
  async createSession(userId, contentId) {
    if (!this.entitlementProvider) throw new ProviderNotConfiguredError("rwa-mock");
    const access = await this.entitlementProvider.checkLectureAccess(userId, contentId);
    if (access.decision !== "granted") throw new ProviderAuthorizationError("Mock playback requires an explicit granted entitlement.", { provider: "rwa-mock" });
    const session = playbackSessionDto({ id: `mock-playback:${userId}:${contentId}`, contentId, status: "mock_created", playbackUrl: null, providerAuthorization: false });
    this.sessions.set(key(userId, contentId), session);
    return session;
  }
  async refreshSession(userId, contentId) {
    const session = this.sessions.get(key(userId, contentId));
    if (!session) throw new ProviderAuthorizationError("No mock playback session exists.", { provider: "rwa-mock" });
    return session;
  }
  async closeSession(userId, contentId) { return this.sessions.delete(key(userId, contentId)); }
}

export class MockLiveJoinProvider extends LiveJoinProvider {
  constructor({ entitlementProvider, sessions = new Map() } = {}) { super(); this.entitlementProvider = entitlementProvider; this.sessions = sessions; }
  async getLiveClassDetails(liveClassId) { return { liveClassId, status: "mock_metadata_only", providerAuthorization: false }; }
  async createJoinSession(userId, liveClassId) {
    if (!this.entitlementProvider) throw new ProviderNotConfiguredError("rwa-mock");
    const access = await this.entitlementProvider.checkLectureAccess(userId, liveClassId);
    if (access.decision !== "granted") throw new ProviderAuthorizationError("Mock live join requires an explicit granted entitlement.", { provider: "rwa-mock" });
    const session = liveJoinSessionDto({ id: `mock-live:${userId}:${liveClassId}`, liveClassId, status: "mock_created", joinUrl: null, providerAuthorization: false });
    this.sessions.set(key(userId, liveClassId), session);
    return session;
  }
  async refreshJoinSession(userId, liveClassId) {
    const session = this.sessions.get(key(userId, liveClassId));
    if (!session) throw new ProviderAuthorizationError("No mock live-join session exists.", { provider: "rwa-mock" });
    return session;
  }
  async leaveSession(userId, liveClassId) { return this.sessions.delete(key(userId, liveClassId)); }
}
